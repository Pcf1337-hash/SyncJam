package com.syncjam.server.youtube

import com.syncjam.server.model.QueueEntry
import com.syncjam.server.model.SyncCommand
import com.syncjam.server.model.TrackInfo
import com.syncjam.server.queue.PlaylistManager
import com.syncjam.server.queue.PlaylistTrack
import com.syncjam.server.queue.TrackSource
import com.syncjam.server.session.SessionManager
import com.syncjam.server.session.SyncBroadcaster
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

private val logger = LoggerFactory.getLogger("YouTubeRoutes")

@Serializable
data class YtAddRequest(
    val youtubeUrl: String,
    val sessionCode: String,
    val userId: String,
    val displayName: String
)

@Serializable
data class YtAddResponse(
    val success: Boolean,
    val accepted: Boolean = false,
    val youtubeId: String? = null,
    val trackId: String? = null,
    val title: String? = null,
    val error: String? = null
)

@Serializable
data class YtStatusResponse(
    val youtubeId: String,
    val status: String,
    val fileSize: Long
)

/** Tracks active downloads to avoid concurrent duplicate requests. */
val activeDownloads: ConcurrentHashMap<String, String> = ConcurrentHashMap()

fun Route.youtubeRoutes(
    ytDlpService: YtDlpService,
    @Suppress("UNUSED_PARAMETER") json: Json,
    sessionManager: SessionManager,
    playlistManager: PlaylistManager,
    broadcaster: SyncBroadcaster,
    downloadScope: CoroutineScope
) {

    /** GET /youtube/info?url=... — Fetch metadata without downloading. */
    get("/youtube/info") {
        val url = call.request.queryParameters["url"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing url parameter"))

        if (!ytDlpService.isYouTubeUrl(url)) {
            return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Not a YouTube URL"))
        }

        val info = ytDlpService.getInfo(url)
            ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Could not fetch video info"))

        call.respond(info)
    }

    /**
     * POST /youtube/add — Session-aware async download.
     * Returns immediately with HTTP 202; broadcasts progress via WebSocket.
     */
    post("/youtube/add") {
        val request = call.receive<YtAddRequest>()

        if (!ytDlpService.isYouTubeUrl(request.youtubeUrl)) {
            call.respond(HttpStatusCode.BadRequest, YtAddResponse(success = false, error = "Not a YouTube URL"))
            return@post
        }

        val ytId = ytDlpService.extractYouTubeId(request.youtubeUrl)
            ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                YtAddResponse(success = false, error = "Invalid YouTube URL — could not extract video ID")
            )

        // Reject duplicate concurrent downloads
        if (activeDownloads.putIfAbsent(ytId, "downloading") != null) {
            call.respond(
                YtAddResponse(
                    success = true, accepted = true,
                    youtubeId = ytId,
                    title = "Already downloading — check status"
                )
            )
            return@post
        }

        // Respond immediately so client isn't blocked
        call.respond(
            HttpStatusCode.Accepted,
            YtAddResponse(success = true, accepted = true, youtubeId = ytId)
        )

        // Notify session that download started (fire quick getInfo first)
        downloadScope.launch {
            val session = sessionManager.getSessionByCode(request.sessionCode)
            val quickInfo = ytDlpService.getInfo(request.youtubeUrl)
            if (session != null) {
                broadcaster.broadcast(
                    session,
                    SyncCommand.YouTubeDownloadStarted(
                        youtubeId = ytId,
                        title = quickInfo?.title ?: ytId,
                        requestedBy = request.displayName,
                        serverTimestampMs = System.currentTimeMillis()
                    )
                )
            }

            try {
                val result = ytDlpService.download(request.youtubeUrl)
                val sessionAfter = sessionManager.getSessionByCode(request.sessionCode)

                if (result != null && sessionAfter != null) {
                    val serverTs = System.currentTimeMillis()
                    val trackId = "yt_${result.youtubeId}"
                    val requestId = "req_${result.youtubeId}"

                    // Add to collaborative playlist
                    playlistManager.addTrack(
                        request.sessionCode,
                        PlaylistTrack(
                            requestId = requestId,
                            trackInfo = TrackInfo(trackId, result.title, result.artist, result.durationMs),
                            score = 0,
                            requestedBy = request.userId,
                            requestedByName = request.displayName,
                            source = TrackSource.YOUTUBE,
                            youtubeId = result.youtubeId,
                            thumbnailUrl = result.thumbnailUrl,
                            addedAt = serverTs
                        )
                    )
                    // Sync authoritative session queue
                    sessionAfter.queue.clear()
                    sessionAfter.queue.addAll(playlistManager.toQueueEntries(request.sessionCode))

                    // Notify all participants: track is ready + full playlist update
                    broadcaster.broadcast(
                        sessionAfter,
                        SyncCommand.YouTubeDownloadReady(
                            youtubeId = result.youtubeId,
                            trackId = trackId,
                            title = result.title,
                            artist = result.artist,
                            durationMs = result.durationMs,
                            serverTimestampMs = serverTs
                        )
                    )
                    broadcaster.broadcast(
                        sessionAfter,
                        SyncCommand.PlaylistUpdate(
                            tracks = playlistManager.toQueueEntries(request.sessionCode),
                            currentIndex = playlistManager.getCurrentIndex(request.sessionCode),
                            serverTimestampMs = serverTs
                        )
                    )
                    logger.info("YouTube track ready: ${result.title} (yt_${result.youtubeId}) in session ${request.sessionCode}")
                } else {
                    logger.error("Download result null or session gone for $ytId")
                }
            } catch (e: Exception) {
                logger.error("Async YouTube download failed for $ytId: ${e.message}", e)
            } finally {
                activeDownloads.remove(ytId)
            }
        }
    }

    /** GET /youtube/stream/{youtubeId} — Stream the downloaded MP3 to the Android client. */
    get("/youtube/stream/{youtubeId}") {
        val ytId = call.parameters["youtubeId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing youtubeId"))

        val file = ytDlpService.getFilePath(ytId)
            ?: return@get call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Track not found. Call /youtube/add first.")
            )

        val mimeType = when (file.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "webm" -> "audio/webm"
            else -> "audio/mpeg"
        }
        call.response.header(HttpHeaders.ContentType, mimeType)
        call.response.header(HttpHeaders.ContentDisposition, "inline")
        call.response.header(HttpHeaders.AcceptRanges, "bytes")
        call.respondFile(file)
    }

    /** GET /youtube/status/{youtubeId} — Check if a track is ready, downloading, or missing. */
    get("/youtube/status/{youtubeId}") {
        val ytId = call.parameters["youtubeId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing youtubeId"))

        val file = ytDlpService.getFilePath(ytId)
        val status = when {
            file != null -> "ready"
            activeDownloads.containsKey(ytId) -> activeDownloads[ytId] ?: "downloading"
            else -> "not_found"
        }
        call.respond(YtStatusResponse(youtubeId = ytId, status = status, fileSize = file?.length() ?: 0L))
    }
}
