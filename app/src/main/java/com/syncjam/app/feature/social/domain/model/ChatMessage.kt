package com.syncjam.app.feature.social.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChatMessage(
    val id: String,
    val sessionId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String?,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOwn: Boolean = false
)
