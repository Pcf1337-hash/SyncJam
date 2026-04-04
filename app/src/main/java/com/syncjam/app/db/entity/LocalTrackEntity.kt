package com.syncjam.app.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_tracks")
data class LocalTrackEntity(
    @PrimaryKey val id: String,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtUri: String?,
    val durationMs: Long,
    val fileSize: Long,
    val mimeType: String,
    val bitrate: Int?,
    val sampleRate: Int?,
    val lastModified: Long,
    val lastPlayed: Long? = null,
    val playCount: Int = 0
)
