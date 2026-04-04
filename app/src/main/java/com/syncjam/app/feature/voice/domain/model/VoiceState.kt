package com.syncjam.app.feature.voice.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed interface VoiceConnectionState {
    data object Disconnected : VoiceConnectionState
    data object Connecting : VoiceConnectionState
    data object Connected : VoiceConnectionState
    data class Error(val message: String) : VoiceConnectionState

    /**
     * Stub/Demo-Modus: kein Token-Server konfiguriert.
     * Lokaler Zustand wird verwaltet, aber keine echte WebRTC-Verbindung.
     */
    data object StubMode : VoiceConnectionState
}

@Immutable
data class VoiceState(
    val connectionState: VoiceConnectionState = VoiceConnectionState.Disconnected,
    val isMicMuted: Boolean = true,
    val isSpeaking: Boolean = false,
    val participants: ImmutableList<VoiceParticipant> = persistentListOf()
) {
    /** True sobald Voice aktiv ist (echt oder Stub). */
    val isActive: Boolean
        get() = connectionState == VoiceConnectionState.Connected ||
                connectionState == VoiceConnectionState.StubMode ||
                connectionState == VoiceConnectionState.Connecting
}
