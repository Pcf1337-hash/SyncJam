package com.syncjam.app.feature.voice.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class VoiceParticipant(
    val sid: String,
    val displayName: String,
    val isMuted: Boolean = true,
    val isSpeaking: Boolean = false,
    val isLocal: Boolean = false
)
