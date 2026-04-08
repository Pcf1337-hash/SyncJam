package com.syncjam.app.feature.session.presentation

import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.syncjam.app.core.common.Constants
import com.syncjam.app.sync.ParticipantInfo
import com.syncjam.app.sync.QueueEntry
import com.syncjam.app.sync.SyncCommand
import com.syncjam.app.sync.TrackInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.syncjam.app.core.auth.SessionPrefs
import com.syncjam.app.db.dao.SessionHistoryDao
import com.syncjam.app.db.entity.SessionHistoryEntity
import com.syncjam.app.feature.social.data.ChatRepositoryImpl
import com.syncjam.app.feature.social.domain.model.ChatMessage as DomainChatMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
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
import androidx.compose.ui.graphics.Color
import com.syncjam.app.core.ui.theme.AlbumArtColorExtractor
import com.syncjam.app.core.ui.theme.DefaultSeedColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient,
    private val json: Json,
    private val supabase: SupabaseClient,
    private val exoPlayer: ExoPlayer,
    private val sessionPrefs: SessionPrefs,
    private val sessionHistoryDao: SessionHistoryDao,
    private val chatRepository: ChatRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _dominantColor = MutableStateFlow(DefaultSeedColor)
    val dominantColor: StateFlow<Color> = _dominantColor.asStateFlow()

    private var wsJob: Job? = null
    private var positionTickerJob: Job? = null
    private var currentSessionCode = ""
    private var currentUserId = ""
    private var currentDisplayName = ""
    private var isHostSession = false

    init {
        // Forward outgoing chat messages from SocialViewModel to the WebSocket
        viewModelScope.launch {
            chatRepository.outgoing.collect { cmd -> sendCommand(cmd) }
        }

        // Update durationMs from ExoPlayer once the track is ready (server may report 0)
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val realDuration = exoPlayer.duration
                        if (realDuration > 0) {
                            _uiState.update { state ->
                                val track = state.currentTrack ?: return@update state
                                if (track.durationMs > 0) return@update state // already known
                                state.copy(currentTrack = track.copy(durationMs = realDuration))
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        // ExoPlayer finished — stop waveform and controls regardless of host/non-host
                        _uiState.update { it.copy(isPlaying = false) }
                    }
                    Player.STATE_IDLE -> {
                        // Player released or reset — also stop waveform
                        _uiState.update { it.copy(isPlaying = false) }
                    }
                }
            }
        })
    }

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
                        // Only the host auto-advances — non-hosts wait for Play command from server.
                        // Also stop the ticker so TrackEnded is not sent multiple times.
                        if (isHostSession) {
                            launch { sendTrackEnded(state.currentTrack.id) }
                            return@update state.copy(positionMs = newPos, isPlaying = false)
                        }
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

    /** Called after CreateSessionScreen navigated to SessionScreen — prevents LaunchedEffect re-trigger on back nav. */
    fun clearCreatedSession() {
        _uiState.update { it.copy(sessionId = null, sessionCode = "") }
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
        updateDominantColor(track.albumArtUri?.let { Uri.parse(it) })
        val uri = getTrackUri(track) ?: run {
            Log.w(TAG, "loadAndPlay: no URI for track ${track.id} (streamUrl=${track.streamUrl}) — track unavailable")
            _uiState.update { it.copy(error = "Track nicht verfügbar: ${track.title}", isPlaying = false) }
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setArtworkUri(track.albumArtUri?.let { Uri.parse(it) })
                            .build()
                    )
                    .build()
                val sameUri = exoPlayer.currentMediaItem?.localConfiguration?.uri == uri
                val needsPrepare = !sameUri ||
                    exoPlayer.playbackState == Player.STATE_IDLE ||
                    exoPlayer.playbackState == Player.STATE_ENDED
                if (needsPrepare) {
                    if (!sameUri) exoPlayer.setMediaItem(mediaItem)
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

    // ── Dynamic Theming ───────────────────────────────────────────────────────

    private fun updateDominantColor(albumArtUri: Uri?) {
        viewModelScope.launch {
            val color = AlbumArtColorExtractor.extractDominantColor(context, albumArtUri)
            _dominantColor.value = color
        }
    }

    // ── Event Handler ─────────────────────────────────────────────────────────

    fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.CreateSession -> createSession(event.name, event.userId, event.displayName, event.autoDeleteAfterHours, event.isPublic, event.password)
            is SessionEvent.JoinSession -> joinSession(event.code, event.userId, event.displayName, event.password)
            is SessionEvent.ConnectToExistingSession -> connectToExistingSession(event.sessionCode, event.isHost, event.displayName)
            is SessionEvent.LeaveSession -> leaveSession()
            is SessionEvent.TogglePlayPause -> togglePlayPause()
            is SessionEvent.SendReaction -> sendReaction(event.emoji)
            is SessionEvent.AddLocalTrackToQueue -> addLocalTrack(event.trackId, event.title, event.artist, event.durationMs, event.contentUri, event.albumArtUri)
            is SessionEvent.AddYouTubeTrack -> addYouTubeTrack(event.url)
            is SessionEvent.Vote -> vote(event.requestId, event.voteType)
            is SessionEvent.RemoveFromQueue -> removeFromQueue(event.requestId)
            is SessionEvent.SendTrackEnded -> sendTrackEnded(event.trackId)
            is SessionEvent.Seek -> seek(event.positionMs)
            is SessionEvent.ToggleMic -> {
                val newMuted = !_uiState.value.isMicMuted
                _uiState.update { it.copy(isMicMuted = newMuted) }
                // Music ducking: 25 % Lautstärke wenn Mikrofon aktiv (newMuted = false)
                exoPlayer.volume = if (!newMuted) _uiState.value.musicVolume * 0.25f
                                   else _uiState.value.musicVolume
            }
            is SessionEvent.SetVolume -> {
                _uiState.update { it.copy(musicVolume = event.volume) }
                // Ducking-Faktor beibehalten wenn Mic gerade aktiv
                exoPlayer.volume = if (!_uiState.value.isMicMuted) event.volume * 0.25f
                                   else event.volume
            }
            is SessionEvent.DismissError -> _uiState.update { it.copy(error = null) }
            is SessionEvent.KickUser -> kickUser(event.targetUserId, event.reason)
            is SessionEvent.BanUser -> banUser(event.targetUserId)
            is SessionEvent.MuteUser -> muteUser(event.targetUserId, event.muted)
            is SessionEvent.TransferAdmin -> transferAdmin(event.newAdminId)
            is SessionEvent.SendDirectMessage -> sendDirectMessage(event.targetUserId, event.message)
            is SessionEvent.DismissKicked -> { leaveSession() }
            is SessionEvent.DismissDirectMessage -> _uiState.update { it.copy(directMessage = null) }
            is SessionEvent.ApproveTrack -> sendCommand(SyncCommand.TrackApproved(event.requestId))
            is SessionEvent.RejectTrack -> {
                sendCommand(SyncCommand.TrackRejected(event.requestId, event.reason))
                _uiState.update { state ->
                    state.copy(pendingApprovals = state.pendingApprovals.filter { it.requestId != event.requestId }.toImmutableList())
                }
            }
            is SessionEvent.RenameSession -> renameSession(event.newName)
            is SessionEvent.ReorderQueue -> {
                val list = _uiState.value.playlist.toMutableList()
                if (event.fromIndex in list.indices && event.toIndex in list.indices) {
                    val item = list.removeAt(event.fromIndex)
                    list.add(event.toIndex, item)
                    _uiState.update { it.copy(playlist = list.toImmutableList()) }
                }
            }
            is SessionEvent.ToggleDebugOverlay -> {
                _uiState.update { it.copy(showLatencyOverlay = !it.showLatencyOverlay) }
            }
        }
    }

    // ── Session Lifecycle ─────────────────────────────────────────────────────

    private fun createSession(name: String, userId: String, displayName: String, autoDeleteAfterHours: Int = 0, isPublic: Boolean = false, password: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val resolvedUserId = supabase.auth.currentUserOrNull()?.id
                    ?: userId.ifBlank { sessionPrefs.getGuestUserId() ?: UUID.randomUUID().toString() }
                sessionPrefs.saveGuestUserId(resolvedUserId)
                val resolvedName = displayName.ifBlank {
                    sessionPrefs.getDisplayName()
                        ?: supabase.auth.currentUserOrNull()?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                        ?: "Host"
                }
                val body = buildJsonObject {
                    put("hostId", resolvedUserId)
                    put("hostName", resolvedName)
                    put("sessionName", name)
                    put("autoDeleteAfterHours", autoDeleteAfterHours)
                    put("isPublic", isPublic)
                    put("password", password)
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

    private var joinPassword = ""

    private fun joinSession(code: String, userId: String, displayName: String, password: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val resolvedUserId = supabase.auth.currentUserOrNull()?.id
                    ?: userId.ifBlank { sessionPrefs.getGuestUserId() ?: UUID.randomUUID().toString() }
                sessionPrefs.saveGuestUserId(resolvedUserId)
                val resolvedName = displayName.ifBlank {
                    sessionPrefs.getDisplayName()
                        ?: supabase.auth.currentUserOrNull()?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                        ?: "Gast"
                }
                httpClient.post("${Constants.SYNC_SERVER_HTTP_URL}/session/$code") {}
                joinPassword = password

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
            // Reuse the same userId as createSession/joinSession — persisted across ViewModel instances
            val resolvedUserId = supabase.auth.currentUserOrNull()?.id
                ?: sessionPrefs.getGuestUserId()
                ?: UUID.randomUUID().toString().also { sessionPrefs.saveGuestUserId(it) }
            val resolvedName = displayName.ifBlank {
                sessionPrefs.getDisplayName()
                    ?: supabase.auth.currentUserOrNull()?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                    ?: if (isHost) "Host" else "Gast"
            }
            currentSessionCode = code
            currentUserId = resolvedUserId
            currentDisplayName = resolvedName
            isHostSession = isHost
            // Host-Rechte sofort setzen — nicht auf StateSnapshot warten
            _uiState.update {
                it.copy(
                    sessionCode = code,
                    currentUserId = resolvedUserId,
                    adminId = if (isHost) resolvedUserId else it.adminId
                )
            }
            // Persist last session for quick rejoin
            sessionPrefs.saveLastSession(code, isHost)
            // Save to session history
            viewModelScope.launch {
                runCatching {
                    sessionHistoryDao.upsert(
                        SessionHistoryEntity(
                            id = "${resolvedUserId}_$code",
                            sessionCode = code,
                            hostName = resolvedName,
                            participantCount = 1,
                            tracksPlayed = 0,
                            startedAt = System.currentTimeMillis(),
                            endedAt = null,
                            lastTrackTitle = null,
                            lastTrackArtist = null,
                            isHost = isHost
                        )
                    )
                }
            }
            connectWebSocket(code, resolvedUserId, resolvedName, isHost)
        }
    }

    private fun leaveSession() {
        val codeToClose = currentSessionCode
        val userIdToClose = currentUserId
        val tracksPlayed = _uiState.value.playlist.count { it.isCurrent }.coerceAtLeast(
            _uiState.value.currentQueueIndex
        )
        val lastTrack = _uiState.value.currentTrack
        stopPositionTicker()
        stopExo()
        wsJob?.cancel()
        wsJob = null
        if (isHostSession) deleteSessionFiles(codeToClose)
        sessionPrefs.clearLastSession()
        viewModelScope.launch {
            runCatching {
                if (codeToClose.isNotEmpty()) {
                    sessionHistoryDao.closeSession(
                        id = "${userIdToClose}_$codeToClose",
                        endedAt = System.currentTimeMillis(),
                        tracksPlayed = tracksPlayed
                    )
                }
            }
        }
        _uiState.update { SessionUiState() }
        currentSessionCode = ""
        currentUserId = ""
        currentDisplayName = ""
        isHostSession = false
        joinPassword = ""
    }

    // ── WebSocket Connection ──────────────────────────────────────────────────

    private fun connectWebSocket(code: String, userId: String, displayName: String, isHost: Boolean, attempt: Int = 0) {
        wsJob?.cancel()
        wsJob = viewModelScope.launch {
            val encodedName = URLEncoder.encode(displayName, "UTF-8")
            val pwParam = if (joinPassword.isNotEmpty()) "&password=${URLEncoder.encode(joinPassword, "UTF-8")}" else ""
            val avatarUrl = sessionPrefs.getAvatarUrl()
            val avatarParam = if (avatarUrl != null) "&avatarUrl=${URLEncoder.encode(avatarUrl, "UTF-8")}" else ""
            val url = "${Constants.SYNC_SERVER_BASE_URL}/ws/session/$code" +
                "?userId=$userId&displayName=$encodedName&host=$isHost$pwParam$avatarParam"
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
                Log.e(TAG, "WebSocket error in $code (attempt $attempt): ${e.message}", e)
                _uiState.update { it.copy(isConnected = false, error = "Verbindung unterbrochen…") }
                if (currentSessionCode.isNotEmpty()) {
                    // Exponential backoff: 2s, 4s, 8s, 16s … capped at 30s
                    val baseDelay = minOf(2000L * (1L shl attempt.coerceAtMost(4)), 30_000L)
                    val jitter = (0..baseDelay / 4).random()
                    val delayMs = baseDelay + jitter
                    delay(delayMs)
                    connectWebSocket(currentSessionCode, currentUserId, currentDisplayName, isHost, attempt + 1)
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
                    // Pending tracks already in the playlist were approved while we were offline
                    val approvedIds = command.queue.map { it.requestId }.toSet()
                    val stillPending = state.ownPendingTrackIds.filter { it !in approvedIds }.toImmutableList()
                    state.copy(
                        sessionId = command.sessionId,
                        hostId = command.hostId,
                        adminId = command.participants.firstOrNull { it.isAdmin }?.userId ?: command.hostId,
                        currentTrack = command.currentTrack?.toUi(Constants.SYNC_SERVER_HTTP_URL),
                        positionMs = command.positionMs,
                        isPlaying = command.isPlaying,
                        playlist = command.queue.mapIndexed { i, q -> q.toUi(i == 0 && command.isPlaying) }.toImmutableList(),
                        participants = filteredParticipants,
                        participantCount = command.participants.size,
                        ownPendingTrackIds = stillPending
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
                    val approvedIds = command.tracks.map { it.requestId }.toSet()
                    val stillPending = state.ownPendingTrackIds.filter { it !in approvedIds }.toImmutableList()
                    val newCurrentTrack = command.tracks.getOrNull(currentIdx)?.trackInfo
                        ?.toUi(Constants.SYNC_SERVER_HTTP_URL)
                        ?.let { serverTrack ->
                            // Preserve ExoPlayer-measured durationMs for the same track
                            val existing = state.currentTrack
                            if (existing != null && existing.id == serverTrack.id && existing.durationMs > 0)
                                serverTrack.copy(durationMs = existing.durationMs)
                            else serverTrack
                        }
                        ?: state.currentTrack
                    state.copy(
                        ownPendingTrackIds = stillPending,
                        playlist = command.tracks.mapIndexed { i, q -> q.toUi(i == currentIdx) }.toImmutableList(),
                        currentQueueIndex = currentIdx,
                        currentTrack = newCurrentTrack,
                        // Reset position when a different track or queue index becomes current
                        positionMs = if (newCurrentTrack?.id != state.currentTrack?.id || currentIdx != state.currentQueueIndex) 0L else state.positionMs
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
                // Find the correct track from our playlist — currentTrack in state may still be the old one
                val matchingEntry = _uiState.value.playlist.find { it.trackId == command.trackId }
                val newTrack: CurrentTrackUi? = when {
                    matchingEntry != null -> matchingEntry.toCurrentTrackUi()
                    _uiState.value.currentTrack?.id == command.trackId -> _uiState.value.currentTrack
                    else -> null
                }
                _uiState.update { state ->
                    state.copy(
                        isPlaying = true,
                        positionMs = command.positionMs,
                        currentTrack = newTrack ?: state.currentTrack
                    )
                }
                val trackToPlay = newTrack ?: _uiState.value.currentTrack
                if (trackToPlay != null) loadAndPlay(trackToPlay, command.positionMs)
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
                _uiState.update { state ->
                    val current = state.floatingReactions
                    if (current.size >= Constants.MAX_REACTIONS_ON_SCREEN) state
                    else state.copy(floatingReactions = (current + reaction).toImmutableList())
                }
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

            is SyncCommand.AdminUpdate -> {
                _uiState.update { it.copy(adminId = command.adminId) }
            }

            is SyncCommand.YouWereKicked -> {
                stopPositionTicker()
                stopExo()
                wsJob?.cancel()
                wsJob = null
                _uiState.update { it.copy(kickedReason = command.reason.ifEmpty { "Du wurdest aus der Session entfernt." }) }
            }

            is SyncCommand.MuteParticipant -> {
                if (command.targetUserId == currentUserId) {
                    _uiState.update { it.copy(isMicMuted = command.muted) }
                }
                _uiState.update { state ->
                    val updated = state.participants.map {
                        if (it.userId == command.targetUserId) it.copy(mutedByAdmin = command.muted) else it
                    }.toImmutableList()
                    state.copy(participants = updated)
                }
            }

            is SyncCommand.DirectMessage -> {
                if (command.fromUserId != currentUserId) {
                    _uiState.update { it.copy(directMessage = DirectMessageNotification(command.fromName, command.message)) }
                    viewModelScope.launch {
                        delay(6000L)
                        _uiState.update { if (it.directMessage?.fromName == command.fromName && it.directMessage.message == command.message) it.copy(directMessage = null) else it }
                    }
                }
            }

            is SyncCommand.ChatMessage -> {
                if (command.senderId != currentUserId) {
                    chatRepository.deliverIncoming(
                        DomainChatMessage(
                            id = "${command.senderId}_${command.timestampMs}",
                            sessionId = currentSessionCode,
                            senderId = command.senderId,
                            senderName = command.senderName,
                            senderAvatarUrl = null,
                            text = command.message,
                            timestamp = command.timestampMs,
                            isOwn = false
                        )
                    )
                }
            }

            is SyncCommand.HostDisconnected -> {
                // If this client is the admin, promote to effective host so TrackEnded fires correctly
                if (command.adminId == currentUserId && !isHostSession) {
                    isHostSession = true
                    Log.i(TAG, "Host disconnected — promoted to effective host (admin)")
                }
                _uiState.update { state ->
                    state.copy(
                        participants = state.participants
                            .filter { it.userId != command.hostId }
                            .toImmutableList()
                    )
                }
            }

            // ── Track Approval ────────────────────────────────────────────────
            is SyncCommand.TrackPendingApproval -> {
                // Host receives this: a non-host wants to add a track
                val pending = PendingTrackApprovalUi(
                    requestId = command.requestId,
                    title = command.trackInfo.title,
                    artist = command.trackInfo.artist,
                    requestedBy = command.requestedBy,
                    requestedByName = command.requestedByName,
                    source = if (command.source == "YOUTUBE") TrackSourceUi.YOUTUBE else TrackSourceUi.LOCAL,
                    youtubeId = command.youtubeId,
                    thumbnailUrl = command.thumbnailUrl
                )
                _uiState.update { state ->
                    state.copy(pendingApprovals = (state.pendingApprovals + pending).toImmutableList())
                }
            }

            is SyncCommand.TrackApproved -> {
                // Server → approver (host): remove from pending list
                // Server → requester: remove from own-pending list
                _uiState.update { state ->
                    state.copy(
                        pendingApprovals = state.pendingApprovals.filter { it.requestId != command.requestId }.toImmutableList(),
                        ownPendingTrackIds = state.ownPendingTrackIds.filter { it != command.requestId }.toImmutableList()
                    )
                }
            }

            is SyncCommand.TrackRejected -> {
                // Server → requester: track was rejected by host
                _uiState.update { state ->
                    state.copy(
                        ownPendingTrackIds = state.ownPendingTrackIds.filter { it != command.requestId }.toImmutableList(),
                        error = if (command.reason.isNotEmpty()) "Track abgelehnt: ${command.reason}"
                                else "Dein Track wurde vom Host abgelehnt."
                    )
                }
            }

            is SyncCommand.RenameSession -> {
                _uiState.update { it.copy(sessionName = command.newName) }
            }

            else -> {}
        }
    }

    // ── Playlist Actions ──────────────────────────────────────────────────────

    private fun addLocalTrack(
        trackId: String,
        title: String,
        artist: String,
        durationMs: Long,
        contentUri: String,
        albumArtUri: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingTrack = true) }
            val requestId = "req_${UUID.randomUUID()}"
            var streamUrl: String? = null
            var albumArtUrl: String? = null
            try {
                val audioBytes = readBytesFromUri(contentUri)
                if (audioBytes != null) {
                    val mimeType = try { context.contentResolver.getType(Uri.parse(contentUri)) } catch (_: Exception) { null }
                    val ext = getMimeTypeExt(contentUri)
                    val fileName = "$requestId.$ext"
                    val response = httpClient.submitFormWithBinaryData(
                        url = "${Constants.SYNC_SERVER_HTTP_URL}/upload/$currentSessionCode",
                        formData = formData {
                            append("file", audioBytes, Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"; name=\"file\"")
                                append(HttpHeaders.ContentType, mimeType ?: "audio/mpeg")
                            })
                        }
                    )
                    val body = response.body<kotlinx.serialization.json.JsonObject>()
                    val urlPath = body["url"]?.jsonPrimitive?.content
                    if (urlPath != null) streamUrl = "${Constants.SYNC_SERVER_HTTP_URL}$urlPath"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Audio upload failed: ${e.message}")
            }
            try {
                if (albumArtUri != null) {
                    if (albumArtUri.startsWith("http://") || albumArtUri.startsWith("https://")) {
                        // Remote cover URL (MusicBrainz/Last.fm) — share directly, no upload needed
                        albumArtUrl = albumArtUri
                    } else {
                        val artBytes = readBytesFromUri(albumArtUri)
                        if (artBytes != null) {
                            val artFileName = "$requestId.jpg"
                            val response = httpClient.submitFormWithBinaryData(
                                url = "${Constants.SYNC_SERVER_HTTP_URL}/upload/$currentSessionCode",
                                formData = formData {
                                    append("file", artBytes, Headers.build {
                                        append(HttpHeaders.ContentDisposition, "filename=\"$artFileName\"; name=\"file\"")
                                        append(HttpHeaders.ContentType, "image/jpeg")
                                    })
                                }
                            )
                            val body = response.body<kotlinx.serialization.json.JsonObject>()
                            val urlPath = body["url"]?.jsonPrimitive?.content
                            if (urlPath != null) albumArtUrl = "${Constants.SYNC_SERVER_HTTP_URL}$urlPath"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Art upload failed: ${e.message}")
            }
            _uiState.update { it.copy(isUploadingTrack = false) }
            // Track if we're a non-host — the server will send it to the host for approval
            if (!isHostSession) {
                _uiState.update { state ->
                    state.copy(ownPendingTrackIds = (state.ownPendingTrackIds + requestId).toImmutableList())
                }
            }
            sendCommand(
                SyncCommand.AddToQueue(
                    requestId = requestId,
                    trackInfo = TrackInfo(trackId, title, artist, durationMs, streamUrl = streamUrl, albumArtUrl = albumArtUrl),
                    requestedBy = currentUserId,
                    requestedByName = currentDisplayName,
                    source = "LOCAL"
                )
            )
        }
    }

    private suspend fun readBytesFromUri(uriString: String, maxSizeBytes: Long = 200 * 1024 * 1024): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            // Try to get file size — some content URIs (e.g. album art) don't support openAssetFileDescriptor
            val fileSize = try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            } catch (_: Exception) { -1L }
            if (fileSize > maxSizeBytes) {
                Log.w(TAG, "File too large ($fileSize bytes > $maxSizeBytes): $uriString")
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = "Datei zu groß (max ${maxSizeBytes / 1024 / 1024}MB)") }
                }
                return@withContext null
            }
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.w(TAG, "readBytesFromUri failed for $uriString: ${e.message}")
            null
        }
    }

    private fun getMimeTypeExt(uriString: String): String {
        val mime = try { context.contentResolver.getType(Uri.parse(uriString)) } catch (_: Exception) { null }
        return when (mime) {
            "audio/flac" -> "flac"
            "audio/ogg" -> "ogg"
            "audio/mp4", "audio/x-m4a" -> "m4a"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/aac" -> "aac"
            "audio/opus" -> "opus"
            "audio/3gpp" -> "3gp"
            "audio/webm" -> "webm"
            else -> "mp3"
        }
    }

    private fun deleteSessionFiles(sessionCode: String) {
        if (sessionCode.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                httpClient.post("${Constants.SYNC_SERVER_HTTP_URL}/upload/$sessionCode/delete") {}
            } catch (e: Exception) {
                Log.w(TAG, "deleteSessionFiles failed: ${e.message}")
            }
        }
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
                val response = httpClient.post("${Constants.SYNC_SERVER_HTTP_URL}/youtube/add") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                if (!response.status.value.toString().startsWith("2")) {
                    val errorBody = runCatching { response.body<kotlinx.serialization.json.JsonObject>() }.getOrNull()
                    val errMsg = errorBody?.get("error")?.jsonPrimitive?.content ?: "Ungültige YouTube-URL"
                    _uiState.update { it.copy(ytDownloadState = YtDownloadState.Error(errMsg)) }
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

    // ── Admin Actions ─────────────────────────────────────────────────────────

    private fun kickUser(targetUserId: String, reason: String) {
        sendCommand(SyncCommand.KickUser(targetUserId = targetUserId, reason = reason))
    }

    private fun banUser(targetUserId: String) {
        sendCommand(SyncCommand.BanUser(targetUserId = targetUserId))
    }

    private fun muteUser(targetUserId: String, muted: Boolean) {
        sendCommand(SyncCommand.MuteParticipant(targetUserId = targetUserId, muted = muted, issuedBy = currentUserId))
    }

    private fun transferAdmin(newAdminId: String) {
        sendCommand(SyncCommand.TransferAdmin(newAdminId = newAdminId))
    }

    private fun sendDirectMessage(targetUserId: String, message: String) {
        sendCommand(SyncCommand.DirectMessage(fromUserId = currentUserId, fromName = currentDisplayName, message = message))
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
            albumArtUri = albumArtUrl,
            isYt = isYt,
            streamUrl = when {
                isYt -> "$serverBaseUrl/youtube/stream/$ytId"
                streamUrl != null -> streamUrl
                else -> null
            }
        )
    }

    private fun renameSession(newName: String) {
        if (newName.isBlank() || !isHostSession) return
        _uiState.update { it.copy(sessionName = newName) }
        sendCommand(SyncCommand.RenameSession(newName = newName))
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
        streamUrl = trackInfo.streamUrl,
        isCurrent = isCurrent
    )

    private fun QueueEntryUi.toCurrentTrackUi() = CurrentTrackUi(
        id = trackId,
        title = title,
        artist = artist,
        durationMs = durationMs,
        albumArtUri = thumbnailUrl,
        streamUrl = when {
            trackId.startsWith("yt_") ->
                "${Constants.SYNC_SERVER_HTTP_URL}/youtube/stream/${trackId.removePrefix("yt_")}"
            else -> streamUrl
        }
    )

    private fun ParticipantInfo.toUi(): ParticipantUi = ParticipantUi(
        userId = userId,
        displayName = displayName,
        avatarUrl = avatarUrl,
        isHost = isHost,
        isAdmin = isAdmin
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
