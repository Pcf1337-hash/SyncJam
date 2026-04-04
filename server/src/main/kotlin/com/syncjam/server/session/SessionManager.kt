package com.syncjam.server.session

import com.syncjam.server.model.CreateSessionRequest
import io.ktor.websocket.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class SessionManager {
    private val logger = LoggerFactory.getLogger(SessionManager::class.java)
    private val sessions = ConcurrentHashMap<String, SessionState>()
    private val codeToId = ConcurrentHashMap<String, String>()

    fun createSession(request: CreateSessionRequest): SessionState {
        val sessionId = generateId()
        val sessionCode = generateCode()
        val state = SessionState(
            sessionId = sessionId,
            sessionCode = sessionCode,
            sessionName = request.sessionName,
            hostId = request.hostId
        )
        sessions[sessionId] = state
        codeToId[sessionCode] = sessionId
        logger.info("Session created: $sessionCode ($sessionId) by ${request.hostId}")
        return state
    }

    fun getSessionByCode(code: String): SessionState? =
        codeToId[code.uppercase()]?.let { sessions[it] }

    fun getSessionById(id: String): SessionState? = sessions[id]

    fun removeSession(sessionId: String) {
        sessions.remove(sessionId)?.let { state ->
            codeToId.remove(state.sessionCode)
            logger.info("Session removed: ${state.sessionCode}")
        }
    }

    fun addClient(sessionCode: String, client: ConnectedClient): Boolean {
        val session = getSessionByCode(sessionCode) ?: return false
        if (session.clients.size >= 8) return false
        session.clients[client.userId] = client
        logger.info("Client ${client.userId} joined session $sessionCode")
        return true
    }

    fun removeClient(sessionCode: String, userId: String) {
        getSessionByCode(sessionCode)?.clients?.remove(userId)
        logger.info("Client $userId left session $sessionCode")
        // Auto-cleanup empty sessions
        val session = getSessionByCode(sessionCode)
        if (session != null && session.clients.isEmpty()) {
            removeSession(session.sessionId)
        }
    }

    private fun generateId(): String = java.util.UUID.randomUUID().toString()

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
