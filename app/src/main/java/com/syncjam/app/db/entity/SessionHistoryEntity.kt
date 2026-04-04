package com.syncjam.app.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions_history")
data class SessionHistoryEntity(
    @PrimaryKey val id: String,
    val sessionCode: String,
    val hostName: String,
    val participantCount: Int,
    val tracksPlayed: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val lastTrackTitle: String?,
    val lastTrackArtist: String?,
    /** True wenn der lokale User beim Erstellen Host war (für Admin-Aktionen). */
    @ColumnInfo(defaultValue = "0") val isHost: Boolean = false
)
