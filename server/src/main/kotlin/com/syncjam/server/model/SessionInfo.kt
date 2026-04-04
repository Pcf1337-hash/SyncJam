package com.syncjam.server.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionRequest(
    val hostId: String,
    val hostName: String,
    val sessionName: String = "Jam Session"
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
