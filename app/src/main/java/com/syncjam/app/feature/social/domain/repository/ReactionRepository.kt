package com.syncjam.app.feature.social.domain.repository

import com.syncjam.app.feature.social.domain.model.Reaction
import kotlinx.coroutines.flow.Flow

interface ReactionRepository {

    /**
     * Observes incoming reactions for the given session.
     * Emits each [Reaction] as it is received from the remote broadcast channel.
     */
    fun observeReactions(sessionId: String): Flow<Reaction>

    /**
     * Broadcasts a reaction to all participants in the session.
     */
    suspend fun sendReaction(reaction: Reaction)
}
