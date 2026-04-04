package com.syncjam.server.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionRequest(
    val hostId: String,
    val hostName: String,
    val sessionName: String = "Jam Session",
    /** 0 = never auto-delete, otherwise delete after this many hours of inactivity */
    val autoDeleteAfterHours: Int = 0
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
