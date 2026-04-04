package com.syncjam.app.core.di

import android.content.Context
import androidx.room.Room
import com.syncjam.app.db.SyncJamDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SyncJamDatabase =
        Room.databaseBuilder(context, SyncJamDatabase::class.java, "syncjam.db").build()

    @Provides
    fun provideLocalTrackDao(db: SyncJamDatabase) = db.localTrackDao()

    @Provides
    fun provideSessionHistoryDao(db: SyncJamDatabase) = db.sessionHistoryDao()
}
