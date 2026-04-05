package com.syncjam.app.feature.social.domain.repository

import com.syncjam.app.feature.social.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    /**
     * Observes incoming chat messages for the given session.
     * Emits each [ChatMessage] as it arrives from the remote broadcast channel.
     */
    fun observeMessages(sessionId: String): Flow<ChatMessage>

    /**
     * Broadcasts a chat message to all participants in the session.
     * Returns the sent [ChatMessage] (with server-assigned id/timestamp if applicable).
     */
    suspend fun sendMessage(message: ChatMessage): ChatMessage

    /**
     * Broadcasts a typing-indicator event for the given user.
     */
    suspend fun sendTypingIndicator(sessionId: String, userId: String, displayName: String)

    /**
     * Observes typing-indicator events — emits the display name of the user who is typing,
     * or null when the indicator expires.
     */
    fun observeTypingIndicator(sessionId: String): Flow<String?>
}
