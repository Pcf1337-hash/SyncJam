package com.syncjam.app.feature.social.data

import com.syncjam.app.feature.social.domain.model.ChatMessage
import com.syncjam.app.feature.social.domain.repository.ChatRepository
import com.syncjam.app.sync.SyncCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chat bridge between [SocialViewModel] and [SessionViewModel] via the WebSocket.
 *
 * - [sendMessage] emits to [outgoing] which SessionViewModel subscribes to and forwards via WS.
 * - SessionViewModel calls [deliverIncoming] when a [SyncCommand.ChatMessage] arrives from WS.
 * - [observeMessages] returns a filtered flow of messages for the current session.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor() : ChatRepository {

    /** Outgoing messages to be forwarded by SessionViewModel → WebSocket. */
    private val _outgoing = MutableSharedFlow<SyncCommand.ChatMessage>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val outgoing: Flow<SyncCommand.ChatMessage> = _outgoing.asSharedFlow()

    /** Incoming messages received from WebSocket and delivered by SessionViewModel. */
    private val _incoming = MutableSharedFlow<ChatMessage>(
        replay = 0,
        extraBufferCapacity = 64
    )

    /** Called by SessionViewModel when a chat_message SyncCommand arrives from the server. */
    fun deliverIncoming(message: ChatMessage) {
        _incoming.tryEmit(message)
    }

    override fun observeMessages(sessionId: String): Flow<ChatMessage> =
        _incoming.asSharedFlow().filter { it.sessionId == sessionId }

    override suspend fun sendMessage(message: ChatMessage): ChatMessage {
        _outgoing.emit(
            SyncCommand.ChatMessage(
                senderId = message.senderId,
                senderName = message.senderName,
                message = message.text,
                timestampMs = message.timestamp
            )
        )
        return message
    }

    override suspend fun sendTypingIndicator(
        sessionId: String,
        userId: String,
        displayName: String
    ) {
        // Typing indicators via WebSocket are not implemented yet
    }

    override fun observeTypingIndicator(sessionId: String): Flow<String?> =
        _incoming.asSharedFlow().map { null }.filter { false }
}
