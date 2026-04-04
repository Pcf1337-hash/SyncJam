package com.syncjam.server.session

import com.syncjam.server.model.SyncCommand
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class SyncBroadcaster(private val json: Json) {
    private val logger = LoggerFactory.getLogger(SyncBroadcaster::class.java)

    suspend fun broadcast(session: SessionState, command: SyncCommand, excludeUserId: String? = null) {
        val text = json.encodeToString(command)
        session.clients.values
            .filter { it.userId != excludeUserId }
            .forEach { client ->
                try {
                    client.session.send(Frame.Text(text))
                } catch (e: Exception) {
                    logger.warn("Failed to send to ${client.userId}: ${e.message}")
                }
            }
    }

    suspend fun sendTo(client: ConnectedClient, command: SyncCommand) {
        val text = json.encodeToString(command)
        try {
            client.session.send(Frame.Text(text))
        } catch (e: Exception) {
            logger.warn("Failed to send to ${client.userId}: ${e.message}")
        }
    }
}
