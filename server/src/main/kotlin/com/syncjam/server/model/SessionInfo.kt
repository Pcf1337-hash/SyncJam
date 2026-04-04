package com.syncjam.server.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionRequest(
    val hostId: String,
    val hostName: String,
    val sessionName: String = "Jam Session",
    val autoDeleteAfterHours: Int = 0,
    val isPublic: Boolean = false,
    val password: String = ""
)

@Serializable
data class CreateSessionResponse(val sessionId: String, val sessionCode: String)

@Serializable
data class SessionInfo(
    val sessionId: String,
    val sessionCode: String,
    val sessionName: String,
    val hostId: String,
    val participantCount: Int,
    val isActive: Boolean,
    val createdAt: Long
)

@Serializable
data class PublicSessionInfo(
    val sessionCode: String,
    val sessionName: String,
    val participantCount: Int,
    val currentTrackTitle: String?,
    val currentTrackArtist: String?,
    val isPasswordProtected: Boolean,
    val createdAt: Long
)
