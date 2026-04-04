package com.syncjam.server.session

import com.syncjam.server.model.CreateSessionRequest
import io.ktor.websocket.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class SessionManager {
    private val logger = LoggerFactory.getLogger(SessionManager::class.java)
    private val sessions = ConcurrentHashMap<String, SessionState>()
    private val codeToId = ConcurrentHashMap<String, String>()

    init {
        // Background job: prune expired sessions every 5 minutes
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            while (true) {
                delay(5 * 60 * 1000L)
                pruneExpired()
            }
        }
    }

    fun createSession(request: CreateSessionRequest): SessionState {
        val sessionId = generateId()
        val sessionCode = generateCode()
        val expiresAt = if (request.autoDeleteAfterHours > 0) {
            System.currentTimeMillis() + request.autoDeleteAfterHours * 3_600_000L
        } else null
        val state = SessionState(
            sessionId = sessionId,
            sessionCode = sessionCode,
            sessionName = request.sessionName,
            hostId = request.hostId,
            expiresAt = expiresAt,
            isPublic = request.isPublic,
            password = request.password,
            adminId = request.hostId
        )
        sessions[sessionId] = state
        codeToId[sessionCode] = sessionId
        logger.info("Session created: $sessionCode ($sessionId) by ${request.hostId}" +
            if (expiresAt != null) " — expires in ${request.autoDeleteAfterHours}h" else " — never expires")
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

    fun isBanned(sessionCode: String, userId: String): Boolean =
        getSessionByCode(sessionCode)?.bannedUserIds?.contains(userId) == true

    fun removeClient(sessionCode: String, userId: String) {
        getSessionByCode(sessionCode)?.clients?.remove(userId)
        logger.info("Client $userId left session $sessionCode — session persists")
        // Sessions are NOT auto-deleted when empty; they expire via expiresAt or host DELETE
    }

    fun getAllSessions(): List<SessionState> = sessions.values.toList()

    private fun pruneExpired() {
        val now = System.currentTimeMillis()
        val expired = sessions.values.filter { s -> s.expiresAt != null && s.expiresAt <= now }
        expired.forEach { s ->
            // Disconnect any lingering clients
            s.clients.values.forEach { client ->
                runCatching { runBlocking { client.session.close(CloseReason(CloseReason.Codes.NORMAL, "Session expired")) } }
            }
            removeSession(s.sessionId)
            logger.info("Session ${s.sessionCode} expired and was removed")
        }
    }

    private fun generateId(): String = java.util.UUID.randomUUID().toString()

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
