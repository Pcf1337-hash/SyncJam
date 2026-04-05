package com.syncjam.app.core.di

import com.syncjam.app.feature.social.data.ChatRepositoryImpl
import com.syncjam.app.feature.social.data.ReactionRepositoryImpl
import com.syncjam.app.feature.social.domain.repository.ChatRepository
import com.syncjam.app.feature.social.domain.repository.ReactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SocialModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindReactionRepository(impl: ReactionRepositoryImpl): ReactionRepository
}
