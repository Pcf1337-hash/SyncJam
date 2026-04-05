package com.syncjam.app.feature.voice.data

import android.content.Context
import android.util.Log
import com.syncjam.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import com.syncjam.app.feature.voice.domain.model.ConnectionQuality
import com.syncjam.app.feature.voice.domain.model.VoiceConnectionState
import com.syncjam.app.feature.voice.domain.model.VoiceParticipant
import com.syncjam.app.feature.voice.domain.model.VoiceState
import com.syncjam.app.feature.voice.domain.repository.VoiceRepository
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.ConnectionQuality as LiveKitConnectionQuality
import io.livekit.android.room.participant.Participant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val tokenService: LiveKitTokenService
) : VoiceRepository {

    // Room wird direkt erstellt — LiveKit.create() gibt @AssistedInject zurück, nicht Hilt-kompatibel
    private val room: Room by lazy { LiveKit.create(appContext = appContext) }

    companion object {
        private const val TAG = "VoiceRepositoryImpl"
    }

    private val _voiceState = MutableStateFlow(VoiceState())
    override val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    // Repository-eigener Scope für Room-Event-Collection (unabhängig vom ViewModel-Lifecycle)
    private var roomScope: CoroutineScope? = null

    override val anyoneSpeaking: StateFlow<Boolean> = _voiceState
        .map { it.anyoneSpeaking }
        .stateIn(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    override suspend fun connect(sessionId: String, userId: String, displayName: String) {
        _voiceState.update { it.copy(connectionState = VoiceConnectionState.Connecting) }

        val tokenResponse = tokenService.fetchToken(sessionId, userId, displayName)

        // Effektive LiveKit-URL: Server-Response hat Priorität, dann BuildConfig-Fallback
        val livekitUrl = tokenResponse?.livekitUrl
            ?.takeIf { it.isNotBlank() }
            ?: BuildConfig.LIVEKIT_URL.takeIf { it.isNotBlank() }

        if (tokenResponse == null || livekitUrl == null) {
            // ── Stub-Modus: kein Server konfiguriert ─────────────────────────
            Log.w(
                TAG,
                "LiveKit nicht konfiguriert — Stub-Modus aktiv. " +
                    "Setze LIVEKIT_URL und TOKEN_ENDPOINT für echten Voice-Chat."
            )
            val localParticipant = VoiceParticipant(
                sid = "local-$userId",
                displayName = displayName,
                isMuted = _voiceState.value.isMicMuted,
                isLocal = true,
                connectionQuality = ConnectionQuality.UNKNOWN
            )
            _voiceState.update {
                it.copy(
                    connectionState = VoiceConnectionState.StubMode,
                    participants = persistentListOf(localParticipant)
                )
            }
            return
        }

        // ── Echte LiveKit-Verbindung ──────────────────────────────────────────
        try {
            room.connect(livekitUrl, tokenResponse.token)

            // Mikrofon-Zustand initialisieren (stumm wenn isMicMuted)
            room.localParticipant.setMicrophoneEnabled(!_voiceState.value.isMicMuted)

            _voiceState.update { it.copy(connectionState = VoiceConnectionState.Connected) }

            // Room-Events in eigenem Scope abonnieren
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            roomScope = scope
            scope.launch { collectRoomEvents() }

            // Initialen Teilnehmer-Snapshot aufbauen
            syncParticipants()

            Log.i(TAG, "LiveKit verbunden: $livekitUrl, Room=${room.name}")
        } catch (e: Exception) {
            Log.e(TAG, "LiveKit connect fehlgeschlagen: ${e.message}", e)
            _voiceState.update {
                it.copy(
                    connectionState = VoiceConnectionState.Error(
                        e.message ?: "Verbindungsfehler"
                    )
                )
            }
        }
    }

    override suspend fun disconnect() {
        roomScope?.cancel()
        roomScope = null

        runCatching { room.disconnect() }.onFailure { e ->
            Log.w(TAG, "Fehler beim Trennen: ${e.message}")
        }

        Log.d(TAG, "Voice getrennt.")
        _voiceState.update {
            it.copy(
                connectionState = VoiceConnectionState.Disconnected,
                participants = persistentListOf(),
                isSpeaking = false,
                isMicMuted = true
            )
        }
    }

    override fun setMicEnabled(enabled: Boolean) {
        // Lokalen State sofort updaten (optimistisch)
        _voiceState.update { state ->
            val updatedParticipants = state.participants
                .map { p -> if (p.isLocal) p.copy(isMuted = !enabled) else p }
                .toImmutableList()
            state.copy(isMicMuted = !enabled, participants = updatedParticipants)
        }

        // LiveKit-Mikrofon schalten (feuert RoomEvent.TrackMuted/Unmuted)
        if (_voiceState.value.connectionState == VoiceConnectionState.Connected) {
            roomScope?.launch {
                runCatching {
                    room.localParticipant.setMicrophoneEnabled(enabled)
                }.onFailure {
                    Log.w(TAG, "Mikrofon-Umschalten fehlgeschlagen: ${it.message}")
                }
            }
        }
    }

    // ─── Private Helpers ────────────────────────────────────────────────────

    /**
     * Abonniert alle relevanten Room-Events und aktualisiert den VoiceState.
     *
     * Abgedeckte Events:
     * - ParticipantConnected / ParticipantDisconnected
     * - TrackMuted / TrackUnmuted
     * - ActiveSpeakersChanged (Speaking-Erkennung)
     * - ConnectionQualityChanged
     * - Disconnected (Fehlerbehandlung)
     */
    private suspend fun collectRoomEvents() {
        room.events.collect { event ->
            when (event) {
                is RoomEvent.ParticipantConnected -> {
                    Log.d(TAG, "Teilnehmer verbunden: ${event.participant.identity}")
                    syncParticipants()
                }

                is RoomEvent.ParticipantDisconnected -> {
                    Log.d(TAG, "Teilnehmer getrennt: ${event.participant.identity}")
                    syncParticipants()
                }

                is RoomEvent.TrackMuted -> {
                    updateParticipantMuteState(event.participant, isMuted = true)
                }

                is RoomEvent.TrackUnmuted -> {
                    updateParticipantMuteState(event.participant, isMuted = false)
                }

                is RoomEvent.ActiveSpeakersChanged -> {
                    // .sid is a value class — unwrap to String for comparison with VoiceParticipant.sid
                    val speakingSids = event.speakers.map { it.sid.value }.toSet()
                    _voiceState.update { state ->
                        val updated = state.participants
                            .map { p -> p.copy(isSpeaking = p.sid in speakingSids) }
                            .toImmutableList()
                        state.copy(
                            participants = updated,
                            // Lokaler Sprecher-State für schnellen Zugriff
                            isSpeaking = updated.any { it.isLocal && it.isSpeaking }
                        )
                    }
                }

                is RoomEvent.ConnectionQualityChanged -> {
                    val quality = event.quality.toDomainQuality()
                    val changedSid = event.participant.sid.value
                    _voiceState.update { state ->
                        val updated = state.participants
                            .map { p ->
                                if (p.sid == changedSid)
                                    p.copy(connectionQuality = quality)
                                else p
                            }
                            .toImmutableList()
                        state.copy(participants = updated)
                    }
                }

                is RoomEvent.Disconnected -> {
                    Log.w(TAG, "Room unerwartet getrennt: ${event.error?.message}")
                    _voiceState.update {
                        it.copy(
                            connectionState = VoiceConnectionState.Error(
                                event.error?.message ?: "Verbindung unterbrochen"
                            ),
                            participants = persistentListOf(),
                            isSpeaking = false
                        )
                    }
                }

                else -> { /* Nicht benötigte Events ignorieren */ }
            }
        }
    }

    /**
     * Baut den kompletten Teilnehmer-Snapshot aus dem aktuellen Room-Zustand auf.
     * Wird beim initialen Connect und bei Join/Leave-Events aufgerufen.
     */
    private fun syncParticipants() {
        val localP = room.localParticipant
        val localVoice = VoiceParticipant(
            sid = localP.sid.value,
            displayName = localP.identity?.value ?: "Ich",
            isMuted = !localP.isMicrophoneEnabled,
            isSpeaking = localP.isSpeaking,
            isLocal = true,
            connectionQuality = localP.connectionQuality.toDomainQuality()
        )

        val remoteVoiceParticipants = room.remoteParticipants.values.map { remote ->
            VoiceParticipant(
                sid = remote.sid.value,
                displayName = remote.identity?.value ?: remote.name ?: "Unbekannt",
                isMuted = !remote.isMicrophoneEnabled,
                isSpeaking = remote.isSpeaking,
                isLocal = false,
                connectionQuality = remote.connectionQuality.toDomainQuality()
            )
        }

        val allParticipants = (listOf(localVoice) + remoteVoiceParticipants).toImmutableList()
        _voiceState.update { it.copy(participants = allParticipants) }
    }

    /**
     * Aktualisiert den Mute-State eines einzelnen Teilnehmers ohne vollständigen Sync.
     */
    private fun updateParticipantMuteState(participant: Participant, isMuted: Boolean) {
        val sid = participant.sid.value
        _voiceState.update { state ->
            val updated = state.participants
                .map { p -> if (p.sid == sid) p.copy(isMuted = isMuted) else p }
                .toImmutableList()
            // Lokaler isMicMuted State mitführen
            val newIsMicMuted = if (participant == room.localParticipant) isMuted
                                else state.isMicMuted
            state.copy(participants = updated, isMicMuted = newIsMicMuted)
        }
    }
}

// ─── Extension: LiveKit → Domain ConnectionQuality Mapping ──────────────────

private fun LiveKitConnectionQuality.toDomainQuality(): ConnectionQuality = when (this) {
    LiveKitConnectionQuality.EXCELLENT -> ConnectionQuality.EXCELLENT
    LiveKitConnectionQuality.GOOD      -> ConnectionQuality.GOOD
    LiveKitConnectionQuality.POOR      -> ConnectionQuality.POOR
    LiveKitConnectionQuality.LOST      -> ConnectionQuality.LOST
    LiveKitConnectionQuality.UNKNOWN   -> ConnectionQuality.UNKNOWN
}
