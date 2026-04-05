package com.syncjam.app.feature.social.data

import com.syncjam.app.feature.social.domain.model.ChatMessage
import com.syncjam.app.feature.social.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ChatRepository] backed by Supabase Realtime Broadcast.
 *
 * TODO: Inject SupabaseClient and replace stub implementations with real
 *       channel subscriptions on "chat:{sessionId}".
 */
@Singleton
class ChatRepositoryImpl @Inject constructor() : ChatRepository {

    override fun observeMessages(sessionId: String): Flow<ChatMessage> {
        // TODO: supabaseClient.channel("chat:$sessionId")
        //   .broadcast(event = "message")
        //   .decodeAs<ChatMessageDto>()
        //   .map { it.toDomain() }
        return emptyFlow()
    }

    override suspend fun sendMessage(message: ChatMessage): ChatMessage {
        // TODO: supabaseClient.channel("chat:${message.sessionId}")
        //   .broadcast(event = "message", ChatMessageDto.fromDomain(message))
        return message
    }

    override suspend fun sendTypingIndicator(
        sessionId: String,
        userId: String,
        displayName: String
    ) {
        // TODO: supabaseClient.channel("chat:$sessionId")
        //   .broadcast(event = "typing", TypingDto(userId, displayName))
    }

    override fun observeTypingIndicator(sessionId: String): Flow<String?> {
        // TODO: supabaseClient.channel("chat:$sessionId")
        //   .broadcast(event = "typing")
        //   .decodeAs<TypingDto>()
        //   .map { it.displayName }
        return emptyFlow()
    }
}
