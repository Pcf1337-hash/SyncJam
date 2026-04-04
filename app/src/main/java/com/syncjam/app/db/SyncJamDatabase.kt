package com.syncjam.app.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.syncjam.app.db.dao.LocalTrackDao
import com.syncjam.app.db.dao.SessionHistoryDao
import com.syncjam.app.db.entity.LocalTrackEntity
import com.syncjam.app.db.entity.SessionHistoryEntity

@Database(
    entities = [LocalTrackEntity::class, SessionHistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class SyncJamDatabase : RoomDatabase() {
    abstract fun localTrackDao(): LocalTrackDao
    abstract fun sessionHistoryDao(): SessionHistoryDao
}
