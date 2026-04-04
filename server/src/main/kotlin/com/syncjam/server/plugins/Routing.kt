package com.syncjam.server.plugins

import com.syncjam.server.model.*
import com.syncjam.server.queue.PlaylistManager
import com.syncjam.server.queue.PlaylistTrack
import com.syncjam.server.queue.TrackSource
import com.syncjam.server.session.*
import com.syncjam.server.youtube.YtDlpService
import com.syncjam.server.youtube.youtubeRoutes
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

private val sessionManager = SessionManager()
private val playlistManager = PlaylistManager()
private val ytDlpService = YtDlpService()

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    classDiscriminator = "type"
}

private val broadcaster = SyncBroadcaster(json)

/** Dedicated scope for long-running YouTube downloads that outlive HTTP requests. */
private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun Application.configureRouting() {
    routing {
        // ── Health ────────────────────────────────────────────────────────────
        get("/health") {
            call.respond(mapOf("status" to "ok", "timestamp" to System.currentTimeMillis().toString()))
        }

        // ── Session REST ──────────────────────────────────────────────────────
        post("/session") {
            val request = call.receive<CreateSessionRequest>()
            val session = sessionManager.createSession(request)
            call.respond(HttpStatusCode.Created, CreateSessionResponse(session.sessionId, session.sessionCode))
        }

        get("/session/{code}") {
            val code = call.parameters["code"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing session code"))
            val session = sessionManager.getSessionByCode(code)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
            call.respond(
                SessionInfo(
                    sessionId = session.sessionId,
                    sessionCode = session.sessionCode,
                    sessionName = session.sessionName,
                    hostId = session.hostId,
                    participantCount = session.clients.size,
                    isActive = true,
                    createdAt = session.createdAt
                )
            )
        }

        delete("/session/{code}") {
            val code = call.parameters["code"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing session code"))
            val session = sessionManager.getSessionByCode(code)
                ?: return@delete call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
            session.clients.values.forEach { client ->
                try { client.session.close(CloseReason(CloseReason.Codes.NORMAL, "Session ended by host")) }
                catch (e: Exception) { logger.warn("Error closing client ${client.userId}: ${e.message}") }
            }
            playlistManager.clearPlaylist(session.sessionCode)
            sessionManager.removeSession(session.sessionId)
            call.respond(HttpStatusCode.NoContent)
        }

        /** GET /session/{code}/playlist — Full playlist state for a session. */
        get("/session/{code}/playlist") {
            val code = call.parameters["code"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing session code"))
            sessionManager.getSessionByCode(code)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
            call.respond(
                mapOf(
                    "tracks" to playlistManager.toQueueEntries(code),
                    "currentIndex" to playlistManager.getCurrentIndex(code)
                )
            )
        }

        // ── File Upload / Static serving ─────────────────────────────────────
        val uploadsRoot = File("/app/uploads").also { it.mkdirs() }

        post("/upload/{sessionCode}") {
            val sessionCode = call.parameters["sessionCode"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing sessionCode"))
            val dir = File(uploadsRoot, sessionCode).also { it.mkdirs() }
            var savedName: String? = null
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is PartData.FileItem && savedName == null) {
                    val rawName = part.originalFileName?.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        ?: "track_${System.currentTimeMillis()}"
                    val file = File(dir, rawName)
                    part.streamProvider().use { input -> file.outputStream().buffered().use { input.copyTo(it) } }
                    savedName = rawName
                }
                part.dispose()
            }
            val name = savedName ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "No file"))
            call.respond(mapOf("url" to "/uploads/$sessionCode/$name"))
        }

        post("/upload/{sessionCode}/delete") {
            val sessionCode = call.parameters["sessionCode"]
                ?: return@post call.respond(HttpStatusCode.BadRequest)
            val dir = File(uploadsRoot, sessionCode)
            if (dir.exists()) dir.deleteRecursively()
            call.respond(HttpStatusCode.NoContent)
        }

        staticFiles("/uploads", uploadsRoot)

        // ── WebSocket Sync ────────────────────────────────────────────────────
        webSocket("/ws/session/{code}") {
            val code = call.parameters["code"] ?: run {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing session code"))
                return@webSocket
            }
            val userId = call.request.queryParameters["userId"] ?: run {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing userId"))
                return@webSocket
            }
            val displayName = call.request.queryParameters["displayName"] ?: "Unknown"
            val isHost = call.request.queryParameters["host"] == "true"

            val session = sessionManager.getSessionByCode(code) ?: run {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session not found: $code"))
                return@webSocket
            }

            val client = ConnectedClient(userId, displayName, isHost, this)
            if (!sessionManager.addClient(code, client)) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session is full (max 8 participants)"))
                return@webSocket
            }

            logger.info("Client $userId ($displayName) joined session $code [host=$isHost]")

            // Send full state snapshot including current playlist
            val snapshot: SyncCommand = SyncCommand.StateSnapshot(
                sessionId = session.sessionId,
                hostId = session.hostId,
                currentTrack = session.currentTrack,
                positionMs = session.estimatedCurrentPosition(),
                isPlaying = session.isPlaying,
                queue = playlistManager.toQueueEntries(code),
                participants = session.getParticipants(),
                serverTimestampMs = System.currentTimeMillis()
            )
            broadcaster.sendTo(client, snapshot)

            // Also send a PlaylistUpdate so client has the full collaborative playlist
            if (playlistManager.getPlaylist(code).isNotEmpty()) {
                broadcaster.sendTo(
                    client,
                    SyncCommand.PlaylistUpdate(
                        tracks = playlistManager.toQueueEntries(code),
                        currentIndex = playlistManager.getCurrentIndex(code),
                        serverTimestampMs = System.currentTimeMillis()
                    )
                )
            }

            // Notify others
            broadcaster.broadcast(
                session,
                SyncCommand.ParticipantJoined(
                    participant = ParticipantInfo(userId, displayName, null, isHost),
                    serverTimestampMs = System.currentTimeMillis()
                ),
                excludeUserId = userId
            )

            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> handleCommand(code, session, client, frame.readText())
                        is Frame.Binary -> handleBinaryTransfer(session, client, frame.readBytes())
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                logger.warn("WebSocket error for $userId in session $code: ${e.message}")
            } finally {
                sessionManager.removeClient(code, userId)
                logger.info("Client $userId disconnected from session $code")
                if (sessionManager.getSessionByCode(code) != null) {
                    try {
                        broadcaster.broadcast(
                            session,
                            SyncCommand.ParticipantLeft(userId = userId, serverTimestampMs = System.currentTimeMillis())
                        )
                    } catch (e: Exception) {
                        logger.warn("Failed to broadcast participant_left for $userId: ${e.message}")
                    }
                }
            }
        }

        // ── NTP Clock Sync ────────────────────────────────────────────────────
        webSocket("/ws/ntp") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    try {
                        val t2 = System.currentTimeMillis()
                        val request = json.decodeFromString<SyncCommand>(frame.readText())
                        if (request is SyncCommand.NtpRequest) {
                            val t3 = System.currentTimeMillis()
                            val response: SyncCommand = SyncCommand.NtpResponse(t1 = request.t1, t2 = t2, t3 = t3)
                            send(Frame.Text(json.encodeToString(response)))
                        }
                    } catch (e: Exception) {
                        logger.warn("NTP error: ${e.message}")
                    }
                }
            }
        }

        // ── YouTube / Playlist ────────────────────────────────────────────────
        youtubeRoutes(ytDlpService, json, sessionManager, playlistManager, broadcaster, downloadScope)
    }
}

// ── Command Handler ───────────────────────────────────────────────────────────

private suspend fun handleCommand(
    sessionCode: String,
    session: SessionState,
    client: ConnectedClient,
    text: String
) {
    try {
        val command = json.decodeFromString<SyncCommand>(text)
        val serverTs = System.currentTimeMillis()

        when (command) {

            // ── Playback ──────────────────────────────────────────────────────
            is SyncCommand.Play -> {
                session.currentTrack = session.currentTrack?.copy(id = command.trackId)
                    ?: TrackInfo(command.trackId, "", "", 0)
                session.positionMs = command.positionMs
                session.isPlaying = true
                session.lastUpdateTime = serverTs
                broadcaster.broadcast(
                    session,
                    SyncCommand.Play(command.trackId, command.positionMs, serverTs),
                    excludeUserId = client.userId
                )
            }

            is SyncCommand.Pause -> {
                session.positionMs = command.positionMs
                session.isPlaying = false
                session.lastUpdateTime = serverTs
                broadcaster.broadcast(session, SyncCommand.Pause(command.positionMs, serverTs), excludeUserId = client.userId)
            }

            is SyncCommand.Seek -> {
                session.positionMs = command.positionMs
                session.lastUpdateTime = serverTs
                broadcaster.broadcast(session, SyncCommand.Seek(command.positionMs, serverTs), excludeUserId = client.userId)
            }

            is SyncCommand.Skip -> {
                session.isPlaying = true
                session.positionMs = 0L
                session.lastUpdateTime = serverTs
                broadcaster.broadcast(session, SyncCommand.Skip(command.nextTrackId, serverTs), excludeUserId = client.userId)
            }

            is SyncCommand.TrackEnded -> {
                // Auto-advance to the next track in the collaborative playlist
                val next = playlistManager.advanceToNext(sessionCode)
                if (next != null) {
                    session.currentTrack = next.trackInfo
                    session.positionMs = 0L
                    session.isPlaying = true
                    session.lastUpdateTime = serverTs
                    broadcaster.broadcast(session, SyncCommand.Play(next.trackInfo.id, 0L, serverTs))
                } else {
                    session.isPlaying = false
                }
                syncAndBroadcastPlaylist(sessionCode, session, serverTs)
            }

            // ── Collaborative Playlist ────────────────────────────────────────
            is SyncCommand.AddToQueue -> {
                val track = PlaylistTrack(
                    requestId = command.requestId,
                    trackInfo = command.trackInfo,
                    score = 0,
                    requestedBy = command.requestedBy,
                    requestedByName = command.requestedByName,
                    source = if (command.source == "YOUTUBE") TrackSource.YOUTUBE else TrackSource.LOCAL,
                    youtubeId = command.youtubeId,
                    thumbnailUrl = command.thumbnailUrl,
                    addedAt = serverTs
                )
                playlistManager.addTrack(sessionCode, track)
                syncAndBroadcastPlaylist(sessionCode, session, serverTs)
            }

            is SyncCommand.RemoveFromQueue -> {
                playlistManager.removeTrack(sessionCode, command.requestId)
                syncAndBroadcastPlaylist(sessionCode, session, serverTs)
            }

            is SyncCommand.Vote -> {
                if (command.voteType == 1 || command.voteType == -1) {
                    playlistManager.vote(sessionCode, command.requestId, command.userId, command.voteType)
                    syncAndBroadcastPlaylist(sessionCode, session, serverTs)
                }
            }

            is SyncCommand.QueueUpdate -> {
                session.queue.clear()
                session.queue.addAll(command.queue)
                broadcaster.broadcast(
                    session,
                    SyncCommand.QueueUpdate(command.queue, serverTs),
                    excludeUserId = client.userId
                )
            }

            // ── Social ────────────────────────────────────────────────────────
            is SyncCommand.Heartbeat -> { /* drift monitoring — no relay */ }

            is SyncCommand.Reaction -> {
                broadcaster.broadcast(session, command)
            }

            is SyncCommand.ChatMessage -> {
                broadcaster.broadcast(session, command)
            }

            // ── Track Transfer ────────────────────────────────────────────────
            is SyncCommand.TrackTransferOffer -> {
                broadcaster.broadcast(session, command, excludeUserId = client.userId)
            }

            is SyncCommand.TrackTransferRequest -> {
                val target = session.clients.values.firstOrNull { it.isHost }
                if (target != null) {
                    broadcaster.sendTo(target, command)
                } else {
                    broadcaster.sendTo(client, SyncCommand.Error("NO_HOST", "No host available for track transfer"))
                }
            }

            else -> {
                logger.debug("Unhandled command from ${client.userId}: ${command::class.simpleName}")
            }
        }
    } catch (e: Exception) {
        logger.warn("Failed to parse command from ${client.userId}: ${e.message} | raw: $text")
        broadcaster.sendTo(client, SyncCommand.Error("PARSE_ERROR", "Invalid command format"))
    }
}

/** Sync session.queue from PlaylistManager and broadcast PlaylistUpdate to all. */
private suspend fun syncAndBroadcastPlaylist(sessionCode: String, session: SessionState, serverTs: Long) {
    val entries = playlistManager.toQueueEntries(sessionCode)
    session.queue.clear()
    session.queue.addAll(entries)
    broadcaster.broadcast(
        session,
        SyncCommand.PlaylistUpdate(
            tracks = entries,
            currentIndex = playlistManager.getCurrentIndex(sessionCode),
            serverTimestampMs = serverTs
        )
    )
}

private suspend fun handleBinaryTransfer(session: SessionState, client: ConnectedClient, data: ByteArray) {
    session.clients.values
        .filter { it.userId != client.userId }
        .forEach { target ->
            try { target.session.send(Frame.Binary(true, data)) }
            catch (e: Exception) { logger.warn("Failed to relay binary to ${target.userId}: ${e.message}") }
        }
}
