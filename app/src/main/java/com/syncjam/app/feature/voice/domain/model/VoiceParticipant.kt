package com.syncjam.app.feature.voice.domain.model

import androidx.compose.runtime.Immutable

/**
 * Netzwerkqualität eines Teilnehmers — gespiegelt vom LiveKit ConnectionQuality enum.
 * Wird für den 3-Balken-Indikator im VoiceParticipantChip genutzt.
 */
enum class ConnectionQuality {
    EXCELLENT,
    GOOD,
    POOR,
    LOST,
    UNKNOWN
}

@Immutable
data class VoiceParticipant(
    val sid: String,
    val displayName: String,
    val isMuted: Boolean = true,
    val isSpeaking: Boolean = false,
    val isLocal: Boolean = false,
    val connectionQuality: ConnectionQuality = ConnectionQuality.UNKNOWN
)
