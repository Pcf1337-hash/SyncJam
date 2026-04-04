package com.syncjam.app.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.syncjam.app.db.dao.LocalTrackDao
import com.syncjam.app.db.dao.SessionHistoryDao
import com.syncjam.app.db.entity.LocalTrackEntity
import com.syncjam.app.db.entity.SessionHistoryEntity

@Database(
    entities = [LocalTrackEntity::class, SessionHistoryEntity::class],
    version = 2,
    exportSchema = true
)
abstract class SyncJamDatabase : RoomDatabase() {
    abstract fun localTrackDao(): LocalTrackDao
    abstract fun sessionHistoryDao(): SessionHistoryDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions_history ADD COLUMN isHost INTEGER NOT NULL DEFAULT 0")
    }
}
