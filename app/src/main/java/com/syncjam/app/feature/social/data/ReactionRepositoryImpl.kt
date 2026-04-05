package com.syncjam.app.feature.social.data

import com.syncjam.app.feature.social.domain.model.Reaction
import com.syncjam.app.feature.social.domain.repository.ReactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ReactionRepository] backed by Supabase Realtime Broadcast.
 *
 * TODO: Inject SupabaseClient and replace stub implementations with real
 *       channel subscriptions on "reactions:{sessionId}".
 */
@Singleton
class ReactionRepositoryImpl @Inject constructor() : ReactionRepository {

    override fun observeReactions(sessionId: String): Flow<Reaction> {
        // TODO: supabaseClient.channel("reactions:$sessionId")
        //   .broadcast(event = "reaction")
        //   .decodeAs<ReactionDto>()
        //   .map { it.toDomain() }
        return emptyFlow()
    }

    override suspend fun sendReaction(reaction: Reaction) {
        // TODO: supabaseClient.channel("reactions:${reaction.sessionId}")
        //   .broadcast(event = "reaction", ReactionDto.fromDomain(reaction))
    }
}
