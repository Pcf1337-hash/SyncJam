package com.syncjam.app.feature.session.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncjam.app.core.common.Constants
import com.syncjam.app.sync.ParticipantInfo
import com.syncjam.app.sync.QueueEntry
import com.syncjam.app.sync.SyncCommand
import com.syncjam.app.sync.TrackInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var wsJob: Job? = null
    private var currentSessionCode = ""
    private var currentUserId = ""
    private var currentDisplayName = ""

    fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.CreateSession -> createSession(event.name, event.userId, event.displayName)
            is SessionEvent.JoinSession -> joinSession(event.code, event.userId, event.displayName)
            is SessionEvent.ConnectToExistingSession -> connectToExistingSession(event.sessionCode, event.isHost)
            is SessionEvent.LeaveSession -> leaveSession()
            is SessionEvent.TogglePlayPause -> togglePlayPause()
            is SessionEvent.SendReaction -> sendReaction(event.emoji)
            is SessionEvent.AddLocalTrackToQueue -> addLocalTrack(event.trackId, event.title, event.artist, event.durationMs)
            is SessionEvent.AddYouTubeTrack -> addYouTubeTrack(event.url)
            is SessionEvent.Vote -> vote(event.requestId, event.voteType)
            is SessionEvent.RemoveFromQueue -> removeFromQueue(event.requestId)
            is SessionEvent.SendTrackEnded -> sendTrackEnded(event.trackId)
            is SessionEvent.Seek -> sendCommand(SyncCommand.Seek(positionMs = event.positionMs, serverTimestampMs = 0L))
            is SessionEvent.ToggleMic -> _uiState.update { it.copy(isMicMuted = !it.isMicMuted) }
            is SessionEvent.SetVolume -> _uiState.update { it.copy(musicVolume = event.volume) }
            is SessionEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    // ── Session Lifecycle ─────────────────────────────────────────────────────

    private fun createSession(name: String, userId: String, displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val resolvedUserId = supabase.auth.currentUserOrNull()?.id ?: userId.ifBlank { UUID.randomUUID().toString() }
                val body = buildJsonObject {
                    put("hostId", resolvedUserId)
                    put("hostName", displayName)
                    put("sessionName", name)
                }
                val response = httpClient.post("${Constants.SYNC_SERVER_HTTP_URL}/session") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                val responseBody = response.body<kotlinx.serialization.json.JsonObject>()
                val sessionId = responseBody["sessionId"]?.toString()?.trim('"') ?: ""
                val sessionCode = responseBody["sessionCode"]?.toString()?.trim('"') ?: ""

                currentSessionCode = sessionCode
                currentUserId = resolvedUserId
                currentDisplayName = displayName

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessionId = sessionId,
                        sessionCode = sessionCode,
                        hostId = resolvedUserId,
                        currentUserId = resolvedUserId
                    )
                }
                connectWebSocket(sessionCode, resolvedUserId, displayName, isHost = true)
            } catch (e: Exception) {
                Log.e(TAG,"createSession failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Session konnte nicht erstellt werden: ${e.message}") }
            }
        }
    }

    private fun joinSession(code: String, userId: String, displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val resolvedUserId = supabase.auth.currentUserOrNull()?.id ?: userId.ifBlank { UUID.randomUUID().toString() }
                httpClient.post("${Constants.SYNC_SERVER_HTTP_URL}/session/$code") {}
                currentSessionCode = code.uppercase()
                currentUserId = resolvedUserId
                currentDisplayName = displayName

                _uiState.update {
                    it.copy(isLoading = false, sessionId = code.uppercase(), sessionCode = code.uppercase(), currentUserId = resolvedUserId)
                }
                connectWebSocket(code.uppercase(), resolvedUserId, displayName, isHost = false)
            } catch (e: Exception) {
                Log.e(TAG,"joinSession failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Session nicht gefunden — Code überprüfen") }
            }
        }
    }

    private fun connectToExistingSession(code: String, isHost: Boolean) {
        viewModelScope.launch {
            val resolvedUserId = supabase.auth.currentUserOrNull()?.id ?: UUID.randomUUID().toString()
            val displayName = if (isHost) "Host" else "Gast"
            currentSessionCode = code
            currentUserId = resolvedUserId
            currentDisplayName = displayName
            _uiState.update { it.copy(sessionCode = code, currentUserId = resolvedUserId) }
            connectWebSocket(code, resolvedUserId, displayName, isHost)
        }
    }

    private fun leaveSession() {
        wsJob?.cancel()
        wsJob = null
        _uiState.update { SessionUiState() }
        currentSessionCode = ""
        currentUserId = ""
        currentDisplayName = ""
    }

    // ── WebSocket Connection ──────────────────────────────────────────────────

    private fun connectWebSocket(code: String, userId: String, displayName: String, isHost: Boolean) {
        wsJob?.cancel()
        wsJob = viewModelScope.launch {
            val encodedName = URLEncoder.encode(displayName, "UTF-8")
            val url = "${Constants.SYNC_SERVER_BASE_URL}/ws/session/$code" +
                "?userId=$userId&displayName=$encodedName&host=$isHost"
            try {
                httpClient.webSocket(url) {
                    _uiState.update { it.copy(isConnected = true) }

                    // Heartbeat every 2s
                    launch {
                        while (true) {
                            delay(Constants.HEARTBEAT_INTERVAL_MS)
                            val cmd: SyncCommand = SyncCommand.Heartbeat(
                                positionMs = _uiState.value.positionMs,
                                clientTimestampMs = System.currentTimeMillis()
                            )
                            try { send(Frame.Text(json.encodeToString(cmd))) }
                            catch (_: Exception) { break }
                        }
                    }

                    // Drain outgoing commands from sendCommand()
                    launch {
                        _outgoingCommands.collect { text ->
                            try { send(Frame.Text(text)) }
                            catch (e: Exception) { Log.w(TAG,"WS send failed: ${e.message}") }
                        }
                    }

                    // Receive loop
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            runCatching { json.decodeFromString<SyncCommand>(frame.readText()) }
                                .onSuccess { processCommand(it) }
                                .onFailure { Log.w(TAG,"Failed to parse command: ${it.message}") }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG,"WebSocket error in $code: ${e.message}", e)
                _uiState.update { it.copy(isConnected = false, error = "Verbindung unterbrochen — reconnecting…") }
                delay(3000)
                if (currentSessionCode.isNotEmpty()) {
                    connectWebSocket(currentSessionCode, currentUserId, currentDisplayName, isHost)
                }
            }
        }
    }

    private fun processCommand(command: SyncCommand) {
        when (command) {
            is SyncCommand.StateSnapshot -> {
                _uiState.update { state ->
                    state.copy(
                        sessionId = command.sessionId,
                        hostId = command.hostId,
                        currentTrack = command.currentTrack?.toUi(Constants.SYNC_SERVER_HTTP_URL),
                        positionMs = command.positionMs,
                        isPlaying = command.isPlaying,
                        playlist = command.queue.mapIndexed { i, q -> q.toUi(i == 0 && command.isPlaying) }.toImmutableList(),
                        participants = command.participants.map { it.toUi() }.toImmutableList(),
                        participantCount = command.participants.size
                    )
                }
            }

            is SyncCommand.PlaylistUpdate -> {
                val currentIdx = command.currentIndex
                _uiState.update { state ->
                    state.copy(
                        playlist = command.tracks.mapIndexed { i, q -> q.toUi(i == currentIdx) }.toImmutableList(),
                        currentQueueIndex = currentIdx,
                        currentTrack = command.tracks.getOrNull(currentIdx)
                            ?.let { entry -> state.currentTrack ?: entry.trackInfo.toUi(Constants.SYNC_SERVER_HTTP_URL) }
                            ?: state.currentTrack
                    )
                }
            }

            is SyncCommand.Play -> {
                _uiState.update { it.copy(isPlaying = true, positionMs = command.positionMs) }
            }

            is SyncCommand.Pause -> {
                _uiState.update { it.copy(isPlaying = false, positionMs = command.positionMs) }
            }

            is SyncCommand.Seek -> {
                _uiState.update { it.copy(positionMs = command.positionMs) }
            }

            is SyncCommand.Skip -> {
                _uiState.update { it.copy(positionMs = 0L, isPlaying = true) }
            }

            is SyncCommand.YouTubeDownloadStarted -> {
                _uiState.update {
                    it.copy(ytDownloadState = YtDownloadState.Downloading(command.youtubeId, command.title))
                }
            }

            is SyncCommand.YouTubeDownloadReady -> {
                // The PlaylistUpdate that follows will update the playlist; just clear download state
                _uiState.update { it.copy(ytDownloadState = null) }
            }

            is SyncCommand.ParticipantJoined -> {
                _uiState.update { state ->
                    val updated = (state.participants + command.participant.toUi())
                        .distinctBy { it.userId }.toImmutableList()
                    state.copy(participants = updated, participantCount = updated.size)
                }
            }

            is SyncCommand.ParticipantLeft -> {
                _uiState.update { state ->
                    val updated = state.participants.filter { it.userId != command.userId }.toImmutableList()
                    state.copy(participants = updated, participantCount = updated.size)
                }
            }

            is SyncCommand.Error -> {
                _uiState.update { it.copy(error = command.message) }
            }

            else -> {} // NtpResponse, TrackTransferOffer, etc. handled elsewhere
        }
    }

    // ── Playlist Actions ──────────────────────────────────────────────────────

    private fun addLocalTrack(trackId: String, title: String, artist: String, durationMs: Long) {
        sendCommand(
            SyncCommand.AddToQueue(
                requestId = "req_${UUID.randomUUID()}",
                trackInfo = TrackInfo(trackId, title, artist, durationMs),
                requestedBy = currentUserId,
                requestedByName = currentDisplayName,
                source = "LOCAL"
            )
        )
    }

    private fun addYouTubeTrack(url: String) {
        if (currentSessionCode.isEmpty()) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(ytDownloadState = YtDownloadState.Downloading("", "Sending to server…")) }
                val body = buildJsonObject {
                    put("youtubeUrl", url)
                    put("sessionCode", currentSessionCode)
                    put("userId", currentUserId)
                    put("displayName", currentDisplayName)
                }
                httpClient.post("${Constants.SYNC_SERVER_HTTP_URL}/youtube/add") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                // Server broadcasts YouTubeDownloadStarted → processCommand clears the temp state
            } catch (e: Exception) {
                Log.e(TAG,"addYouTubeTrack failed: ${e.message}", e)
                _uiState.update { it.copy(ytDownloadState = YtDownloadState.Error("Download failed: ${e.message}")) }
            }
        }
    }

    private fun vote(requestId: String, voteType: Int) {
        sendCommand(SyncCommand.Vote(requestId = requestId, userId = currentUserId, voteType = voteType))
    }

    private fun removeFromQueue(requestId: String) {
        sendCommand(SyncCommand.RemoveFromQueue(requestId = requestId))
    }

    private fun sendTrackEnded(trackId: String) {
        sendCommand(SyncCommand.TrackEnded(trackId = trackId))
    }

    private fun togglePlayPause() {
        val state = _uiState.value
        if (state.isPlaying) {
            sendCommand(SyncCommand.Pause(positionMs = state.positionMs, serverTimestampMs = 0L))
        } else {
            sendCommand(
                SyncCommand.Play(
                    trackId = state.currentTrack?.id ?: return,
                    positionMs = state.positionMs,
                    serverTimestampMs = 0L
                )
            )
        }
    }

    private fun sendReaction(emoji: String) {
        sendCommand(SyncCommand.Reaction(emoji = emoji, senderId = currentUserId, senderName = currentDisplayName))
    }

    // ── Internal WebSocket Send ───────────────────────────────────────────────

    private fun sendCommand(command: SyncCommand) {
        viewModelScope.launch {
            // The ws send is handled by the active websocket session — we use a shared channel.
            // For simplicity, re-send via a new job that finds the active session.
            // In production this would use a Channel<SyncCommand> fed into the WS loop.
            try {
                val encoded = json.encodeToString(command)
                // Stored frame queue pattern: commands are dispatched here
                // (direct send from outside the websocket block requires the session reference)
                _outgoingCommands.emit(encoded)
            } catch (e: Exception) {
                Log.w(TAG,"Failed to enqueue command: ${e.message}")
            }
        }
    }

    private val _outgoingCommands = kotlinx.coroutines.flow.MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)

    // ── Mapper Extensions ─────────────────────────────────────────────────────

    private fun TrackInfo.toUi(serverBaseUrl: String): CurrentTrackUi {
        val isYt = id.startsWith("yt_")
        val ytId = if (isYt) id.removePrefix("yt_") else null
        return CurrentTrackUi(
            id = id,
            title = title,
            artist = artist,
            durationMs = durationMs,
            albumArtUri = null,
            streamUrl = if (isYt) "$serverBaseUrl/youtube/stream/$ytId" else null
        )
    }

    private fun QueueEntry.toUi(isCurrent: Boolean = false): QueueEntryUi = QueueEntryUi(
        requestId = requestId,
        trackId = trackInfo.id,
        title = trackInfo.title,
        artist = trackInfo.artist,
        durationMs = trackInfo.durationMs,
        score = score,
        requestedBy = requestedBy,
        requestedByName = requestedByName,
        source = if (source == "YOUTUBE") TrackSourceUi.YOUTUBE else TrackSourceUi.LOCAL,
        youtubeId = youtubeId,
        thumbnailUrl = thumbnailUrl,
        isCurrent = isCurrent
    )

    private fun ParticipantInfo.toUi(): ParticipantUi = ParticipantUi(
        userId = userId,
        displayName = displayName,
        avatarUrl = avatarUrl,
        isHost = isHost
    )

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
    }

    companion object {
        private const val TAG = "SessionViewModel"
    }
}

private fun <T> List<T>.distinctBy(selector: (T) -> Any?): List<T> {
    val seen = mutableSetOf<Any?>()
    return filter { seen.add(selector(it)) }
}
