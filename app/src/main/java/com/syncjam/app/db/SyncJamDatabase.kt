package com.syncjam.app.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.syncjam.app.db.dao.LocalTrackDao
import com.syncjam.app.db.dao.PlaylistDao
import com.syncjam.app.db.dao.SessionHistoryDao
import com.syncjam.app.db.entity.LocalTrackEntity
import com.syncjam.app.db.entity.PlaylistEntity
import com.syncjam.app.db.entity.PlaylistTrackCrossRef
import com.syncjam.app.db.entity.SessionHistoryEntity

@Database(
    entities = [
        LocalTrackEntity::class,
        SessionHistoryEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class
    ],
    version = 3,
    exportSchema = true
)
abstract class SyncJamDatabase : RoomDatabase() {
    abstract fun localTrackDao(): LocalTrackDao
    abstract fun sessionHistoryDao(): SessionHistoryDao
    abstract fun playlistDao(): PlaylistDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions_history ADD COLUMN isHost INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add favorites and remote cover URL to tracks
        db.execSQL("ALTER TABLE local_tracks ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE local_tracks ADD COLUMN remoteCoverUrl TEXT")
        // Create playlists table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS playlists (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
        // Create playlist_tracks junction table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS playlist_tracks (
                playlistId TEXT NOT NULL,
                trackId TEXT NOT NULL,
                position INTEGER NOT NULL DEFAULT 0,
                addedAt INTEGER NOT NULL,
                PRIMARY KEY (playlistId, trackId),
                FOREIGN KEY (playlistId) REFERENCES playlists(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_playlistId ON playlist_tracks(playlistId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_trackId ON playlist_tracks(trackId)")
    }
}
