package com.syncjam.app.feature.session.presentation

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val supabase: SupabaseClient,
    private val exoPlayer: ExoPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var wsJob: Job? = null
    private var positionTickerJob: Job? = null
    private var currentSessionCode = ""
    private var currentUserId = ""
    private var currentDisplayName = ""
    private var isHostSession = false

    // ── Position Ticker ───────────────────────────────────────────────────────

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                _uiState.update { state ->
                    if (!state.isPlaying || state.currentTrack == null) return@update state
                    // Prefer ExoPlayer position if it's playing (more accurate)
                    val exoPos = withContext(Dispatchers.Main) {
                        if (exoPlayer.isPlaying) exoPlayer.currentPosition else -1L
                    }
                    val newPos = if (exoPos >= 0L) exoPos
                    else (state.positionMs + 100L).coerceAtMost(state.currentTrack.durationMs)

                    if (newPos >= state.currentTrack.durationMs && state.currentTrack.durationMs > 0) {
                        launch { sendTrackEnded(state.currentTrack.id) }
                    }
                    state.copy(positionMs = newPos)
                }
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    // ── ExoPlayer Control (main thread) ───────────────────────────────────────

    /**
     * Returns the playback URI for a track:
     * - YouTube: the HTTP stream URL from the server
     * - Local: content://media/external/audio/media/{id}
     */
    private fun getTrackUri(track: CurrentTrackUi): Uri? {
        return when {
            track.streamUrl != null -> Uri.parse(track.streamUrl)
            else -> {
                val numericId = track.id.toLongOrNull() ?: return null
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, numericId)
            }
        }
    }

    private fun loadAndPlay(track: CurrentTrackUi, positionMs: Long) {
        val uri = getTrackUri(track) ?: return
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val mediaItem = MediaItem.fromUri(uri)
                if (exoPlayer.currentMediaItem?.localConfiguration?.uri != uri) {
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                }
                exoPlayer.seekTo(positionMs)
                exoPlayer.play()
            } catch (e: Exception) {
                Log.w(TAG, "ExoPlayer load failed for $uri: ${e.message}")
            }
        }
    }

    private fun pauseExo(track: CurrentTrackUi, positionMs: Long) {
        viewModelScope.launch(Dispatchers.Main) {
            exoPlayer.pause()
            exoPlayer.seekTo(positionMs)
        }
    }

    private fun seekExo(positionMs: Long) {
        viewModelScope.launch(Dispatchers.Main) {
            exoPlayer.seekTo(positionMs)
        }
    }

    private fun stopExo() {
        viewModelScope.launch(Dispatchers.Main) {
            exoPlayer.stop()
        }
    }

    // ── Event Handler ─────────────────────────────────────────────────────────

    fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.CreateSession -> createSession(event.name, event.userId, event.displayName)
            is SessionEvent.JoinSession -> joinSession(event.code, event.userId, event.displayName)
            is SessionEvent.ConnectToExistingSession -> connectToExistingSession(event.sessionCode, event.isHost, event.displayName)
            is SessionEvent.LeaveSession -> leaveSession()
            is SessionEvent.TogglePlayPause -> togglePlayPause()
            is SessionEvent.SendReaction -> sendReaction(event.emoji)
            is SessionEvent.AddLocalTrackToQueue -> addLocalTrack(event.trackId, event.title, event.artist, event.durationMs)
            is SessionEvent.AddYouTubeTrack -> addYouTubeTrack(event.url)
            is SessionEvent.Vote -> vote(event.requestId, event.voteType)
            is SessionEvent.RemoveFromQueue -> removeFromQueue(event.requestId)
            is SessionEvent.SendTrackEnded -> sendTrackEnded(event.trackId)
            is SessionEvent.Seek -> seek(event.positionMs)
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
                val resolvedName = displayName.ifBlank { "Host" }
                val body = buildJsonObject {
                    put("hostId", resolvedUserId)
                    put("hostName", resolvedName)
                    put("sessionName", name)
                }
                val response = httpClient.post("${Constants.SYNC_SERVER_HTTP_URL}/session") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                val responseBody = response.body<kotlinx.serialization.json.JsonObject>()
                val sessionId = responseBody["sessionId"]?.toString()?.trim('"') ?: ""
                val sessionCode = responseBody["sessionCode"]?.toString()?.trim('"') ?: ""

                // Store session info but do NOT connect WebSocket here.
                // SessionScreen's ConnectToExistingSession will make the single connection.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessionId = sessionId,
                        sessionCode = sessionCode,
                        hostId = resolvedUserId,
                        currentUserId = resolvedUserId,
                        pendingDisplayName = resolvedName
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "createSession failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Session konnte nicht erstellt werden: ${e.message}") }
            }
        }
    }

    private fun joinSession(code: String, userId: String, displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val resolvedUserId = supabase.auth.currentUserOrNull()?.id ?: userId.ifBlank { UUID.randomUUID().toString() }
                val resolvedName = displayName.ifBlank { "Gast" }
                httpClient.post("${Constants.SYNC_SERVER_HTTP_URL}/session/$code") {}

                // Store session info but do NOT connect WebSocket here.
                // SessionScreen's ConnectToExistingSession will make the single connection.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessionId = code.uppercase(),
                        sessionCode = code.uppercase(),
                        currentUserId = resolvedUserId,
                        pendingDisplayName = resolvedName
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "joinSession failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Session nicht gefunden — Code überprüfen") }
            }
        }
    }

    private fun connectToExistingSession(code: String, isHost: Boolean, displayName: String) {
        if (currentSessionCode == code) return
        viewModelScope.launch {
            val resolvedUserId = supabase.auth.currentUserOrNull()?.id ?: UUID.randomUUID().toString()
            val resolvedName = displayName.ifBlank { if (isHost) "Host" else "Gast" }
            currentSessionCode = code
            currentUserId = resolvedUserId
            currentDisplayName = resolvedName
            isHostSession = isHost
            _uiState.update { it.copy(sessionCode = code, currentUserId = resolvedUserId) }
            connectWebSocket(code, resolvedUserId, resolvedName, isHost)
        }
    }

    private fun leaveSession() {
        stopPositionTicker()
        stopExo()
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

                    launch {
                        _outgoingCommands.collect { text ->
                            try { send(Frame.Text(text)) }
                            catch (e: Exception) { Log.w(TAG, "WS send failed: ${e.message}") }
                        }
                    }

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            runCatching { json.decodeFromString<SyncCommand>(frame.readText()) }
                                .onSuccess { processCommand(it) }
                                .onFailure { Log.w(TAG, "Failed to parse command: ${it.message}") }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "WebSocket error in $code: ${e.message}", e)
                _uiState.update { it.copy(isConnected = false, error = "Verbindung unterbrochen…") }
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
                val filteredParticipants = command.participants
                    .filter { it.userId != currentUserId }
                    .distinctBy { it.userId }
                    .map { it.toUi() }
                    .toImmutableList()

                _uiState.update { state ->
                    state.copy(
                        sessionId = command.sessionId,
                        hostId = command.hostId,
                        currentTrack = command.currentTrack?.toUi(Constants.SYNC_SERVER_HTTP_URL),
                        positionMs = command.positionMs,
                        isPlaying = command.isPlaying,
                        playlist = command.queue.mapIndexed { i, q -> q.toUi(i == 0 && command.isPlaying) }.toImmutableList(),
                        participants = filteredParticipants,
                        participantCount = command.participants.size
                    )
                }
                val track = command.currentTrack?.toUi(Constants.SYNC_SERVER_HTTP_URL)
                if (command.isPlaying) {
                    if (track != null) loadAndPlay(track, command.positionMs)
                    startPositionTicker()
                } else {
                    stopPositionTicker()
                    if (track != null) pauseExo(track, command.positionMs)
                }
            }

            is SyncCommand.PlaylistUpdate -> {
                val wasEmpty = _uiState.value.playlist.isEmpty()
                val wasPlaying = _uiState.value.isPlaying
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
                // Auto-play first track: only host triggers play so all others receive a Play command
                if (wasEmpty && command.tracks.isNotEmpty() && !wasPlaying && isHostSession) {
                    val firstEntry = command.tracks.getOrNull(currentIdx) ?: command.tracks.first()
                    val trackUi = firstEntry.trackInfo.toUi(Constants.SYNC_SERVER_HTTP_URL)
                    _uiState.update { it.copy(isPlaying = true, currentTrack = trackUi, positionMs = 0L) }
                    loadAndPlay(trackUi, 0L)
                    startPositionTicker()
                    sendCommand(SyncCommand.Play(
                        trackId = trackUi.id,
                        positionMs = 0L,
                        serverTimestampMs = System.currentTimeMillis()
                    ))
                }
            }

            is SyncCommand.Play -> {
                _uiState.update { it.copy(isPlaying = true, positionMs = command.positionMs) }
                val track = _uiState.value.currentTrack
                if (track != null) loadAndPlay(track, command.positionMs)
                startPositionTicker()
            }

            is SyncCommand.Pause -> {
                stopPositionTicker()
                _uiState.update { it.copy(isPlaying = false, positionMs = command.positionMs) }
                val track = _uiState.value.currentTrack
                if (track != null) pauseExo(track, command.positionMs)
            }

            is SyncCommand.Seek -> {
                _uiState.update { it.copy(positionMs = command.positionMs) }
                seekExo(command.positionMs)
                if (_uiState.value.isPlaying) startPositionTicker()
            }

            is SyncCommand.Skip -> {
                stopPositionTicker()
                stopExo()
                _uiState.update { it.copy(positionMs = 0L, isPlaying = true) }
                startPositionTicker()
            }

            is SyncCommand.YouTubeDownloadStarted -> {
                _uiState.update {
                    it.copy(ytDownloadState = YtDownloadState.Downloading(command.youtubeId, command.title))
                }
            }

            is SyncCommand.YouTubeDownloadReady -> {
                _uiState.update { it.copy(ytDownloadState = null) }
            }

            is SyncCommand.ParticipantJoined -> {
                // Skip self — we're already in the UI as the current user
                if (command.participant.userId == currentUserId) return
                _uiState.update { state ->
                    val updated = (state.participants + command.participant.toUi())
                        .distinctBy { it.userId }.toImmutableList()
                    state.copy(participants = updated, participantCount = updated.size + 1)
                }
            }

            is SyncCommand.ParticipantLeft -> {
                _uiState.update { state ->
                    val updated = state.participants.filter { it.userId != command.userId }.toImmutableList()
                    state.copy(participants = updated, participantCount = (updated.size + 1).coerceAtLeast(1))
                }
            }

            is SyncCommand.Reaction -> {
                val durationMs = when {
                    command.emoji.length <= 4 -> 2500L   // single emoji
                    command.emoji.length <= 30 -> 4000L  // short text
                    else -> 6000L                        // long text
                }
                val reaction = FloatingReactionUi(
                    emoji = command.emoji,
                    xFraction = (command.senderId.hashCode().and(0x7FFFFFFF) % 70 + 10) / 100f,
                    durationMs = durationMs
                )
                _uiState.update { it.copy(floatingReactions = (it.floatingReactions + reaction).toImmutableList()) }
                viewModelScope.launch {
                    delay(durationMs + 200L)
                    _uiState.update { state ->
                        state.copy(floatingReactions = state.floatingReactions.filter { it.id != reaction.id }.toImmutableList())
                    }
                }
            }

            is SyncCommand.Error -> {
                _uiState.update { it.copy(error = command.message) }
            }

            else -> {}
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
                _uiState.update { it.copy(ytDownloadState = YtDownloadState.Downloading("", "Wird gesendet…")) }
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
            } catch (e: Exception) {
                Log.e(TAG, "addYouTubeTrack failed: ${e.message}", e)
                _uiState.update { it.copy(ytDownloadState = YtDownloadState.Error("Download fehlgeschlagen: ${e.message}")) }
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

    private fun seek(positionMs: Long) {
        _uiState.update { it.copy(positionMs = positionMs) }
        seekExo(positionMs)
        if (_uiState.value.isPlaying) startPositionTicker()
        sendCommand(SyncCommand.Seek(positionMs = positionMs, serverTimestampMs = System.currentTimeMillis()))
    }

    private fun togglePlayPause() {
        val state = _uiState.value
        if (state.isPlaying) {
            stopPositionTicker()
            _uiState.update { it.copy(isPlaying = false) }
            val track = state.currentTrack
            if (track != null) pauseExo(track, state.positionMs)
            sendCommand(SyncCommand.Pause(positionMs = state.positionMs, serverTimestampMs = System.currentTimeMillis()))
        } else {
            val track = state.currentTrack ?: return
            _uiState.update { it.copy(isPlaying = true) }
            startPositionTicker()
            loadAndPlay(track, state.positionMs)
            sendCommand(
                SyncCommand.Play(
                    trackId = track.id,
                    positionMs = state.positionMs,
                    serverTimestampMs = System.currentTimeMillis()
                )
            )
        }
    }

    private fun sendReaction(emoji: String) {
        sendCommand(SyncCommand.Reaction(emoji = emoji, senderId = currentUserId, senderName = currentDisplayName))
    }

    // ── WebSocket Send ────────────────────────────────────────────────────────

    private fun sendCommand(command: SyncCommand) {
        viewModelScope.launch {
            try {
                _outgoingCommands.emit(json.encodeToString(command))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enqueue command: ${e.message}")
            }
        }
    }

    private val _outgoingCommands = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)

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
        stopPositionTicker()
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
