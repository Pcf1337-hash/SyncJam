package com.syncjam.server.session

import com.syncjam.server.model.ParticipantInfo
import com.syncjam.server.model.QueueEntry
import com.syncjam.server.model.TrackInfo
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

data class ConnectedClient(
    val userId: String,
    val displayName: String,
    val isHost: Boolean,
    val session: DefaultWebSocketSession
)

data class SessionState(
    val sessionId: String,
    val sessionCode: String,
    val sessionName: String,
    val hostId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val isPublic: Boolean = false,
    /** Plaintext password; empty = no password */
    val password: String = "",
    /** User ID with admin privileges (kick/ban/mute). Starts as hostId, transferable. */
    var adminId: String = hostId,
    var currentTrack: TrackInfo? = null,
    var positionMs: Long = 0L,
    var isPlaying: Boolean = false,
    var lastUpdateTime: Long = System.currentTimeMillis(),
    val queue: MutableList<QueueEntry> = mutableListOf(),
    val clients: ConcurrentHashMap<String, ConnectedClient> = ConcurrentHashMap(),
    val bannedUserIds: MutableSet<String> = mutableSetOf(),
    /** UserIds the admin has server-muted (cannot speak) */
    val mutedByAdmin: MutableSet<String> = mutableSetOf()
) {
    fun getParticipants(): List<ParticipantInfo> = clients.values.map {
        ParticipantInfo(it.userId, it.displayName, null, it.isHost, it.userId == adminId)
    }

    fun estimatedCurrentPosition(): Long {
        return if (isPlaying) {
            val elapsed = System.currentTimeMillis() - lastUpdateTime
            positionMs + elapsed
        } else {
            positionMs
        }
    }
}
