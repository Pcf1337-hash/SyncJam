package com.syncjam.app.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.syncjam.app.db.entity.SessionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionHistoryDao {
    @Query("SELECT * FROM sessions_history ORDER BY startedAt DESC LIMIT 20")
    fun getRecentSessions(): Flow<List<SessionHistoryEntity>>

    @Query("SELECT * FROM sessions_history ORDER BY startedAt DESC LIMIT 20")
    suspend fun getRecentSessionsOnce(): List<SessionHistoryEntity>

    @Upsert
    suspend fun upsert(session: SessionHistoryEntity)

    @Query("UPDATE sessions_history SET endedAt = :endedAt, tracksPlayed = :tracksPlayed WHERE id = :id")
    suspend fun closeSession(id: String, endedAt: Long, tracksPlayed: Int)

    @Query("DELETE FROM sessions_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sessions_history")
    suspend fun deleteAll()
}
