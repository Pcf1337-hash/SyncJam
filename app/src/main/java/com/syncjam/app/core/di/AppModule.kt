package com.syncjam.app.core.di

import android.content.Context
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.syncjam.app.core.auth.SessionPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideSessionPrefs(@ApplicationContext context: Context): SessionPrefs {
        return SessionPrefs(context)
    }
}
