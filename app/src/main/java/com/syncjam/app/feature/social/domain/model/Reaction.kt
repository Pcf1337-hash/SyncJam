package com.syncjam.app.feature.social.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Reaction(
    val id: Long = System.nanoTime(),
    val emoji: String,
    val senderName: String,
    val senderId: String,
    val x: Float, // 0.0 - 1.0 relative screen position
    val sessionId: String
)
