package com.syncjam.app.feature.voice.data

import android.util.Log
import com.syncjam.app.feature.voice.domain.model.VoiceConnectionState
import com.syncjam.app.feature.voice.domain.model.VoiceParticipant
import com.syncjam.app.feature.voice.domain.model.VoiceState
import com.syncjam.app.feature.voice.domain.repository.VoiceRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepositoryImpl @Inject constructor(
    private val tokenService: LiveKitTokenService
) : VoiceRepository {

    companion object {
        private const val TAG = "VoiceRepositoryImpl"

        /**
         * LiveKit-Server-URL.
         * Beispiel: "wss://your-project.livekit.cloud"
         * Leer lassen → Stub-Modus.
         */
        private const val LIVEKIT_URL = "" // TODO: Konfigurieren
    }

    private val _voiceState = MutableStateFlow(VoiceState())
    override val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    override suspend fun connect(sessionId: String, userId: String, displayName: String) {
        _voiceState.update { it.copy(connectionState = VoiceConnectionState.Connecting) }

        val token = tokenService.fetchToken(sessionId, userId, displayName)

        if (token == null || LIVEKIT_URL.isBlank()) {
            // ── Stub-Modus: kein Server konfiguriert ──────────────────────────────
            Log.w(
                TAG,
                "LiveKit nicht konfiguriert — Stub-Modus aktiv. " +
                        "Setze LIVEKIT_URL und TOKEN_ENDPOINT für echten Voice-Chat."
            )
            val localParticipant = VoiceParticipant(
                sid = "local-$userId",
                displayName = displayName,
                isMuted = _voiceState.value.isMicMuted,
                isLocal = true
            )
            _voiceState.update {
                it.copy(
                    connectionState = VoiceConnectionState.StubMode,
                    participants = persistentListOf(localParticipant)
                )
            }
            return
        }

        // ── Echte LiveKit-Verbindung (aktiv sobald Server konfiguriert) ───────────
        // Entkommentieren und LiveKit-SDK-Room-Objekt via DI injecten:
        //
        // try {
        //     room.connect(LIVEKIT_URL, token)
        //     room.localParticipant.setMicrophoneEnabled(!_voiceState.value.isMicMuted)
        //     collectRoomEvents(room) // Participant-Updates, Speaking-Indicator etc.
        //     _voiceState.update { it.copy(connectionState = VoiceConnectionState.Connected) }
        // } catch (e: Exception) {
        //     Log.e(TAG, "LiveKit connect failed: ${e.message}", e)
        //     _voiceState.update { it.copy(connectionState = VoiceConnectionState.Error(e.message ?: "Verbindungsfehler")) }
        // }
        Log.w(TAG, "LiveKit-Verbindungspfad noch nicht aktiviert.")
    }

    override suspend fun disconnect() {
        // room.disconnect() — stub
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
        // room.localParticipant.setMicrophoneEnabled(enabled) — stub
        _voiceState.update { state ->
            val updatedParticipants = state.participants
                .map { p -> if (p.isLocal) p.copy(isMuted = !enabled) else p }
                .toImmutableList()
            state.copy(isMicMuted = !enabled, participants = updatedParticipants)
        }
    }
}
