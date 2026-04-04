package com.syncjam.app.feature.voice.domain.repository

import com.syncjam.app.feature.voice.domain.model.VoiceState
import kotlinx.coroutines.flow.StateFlow

interface VoiceRepository {
    val voiceState: StateFlow<VoiceState>

    /** Verbindet mit dem LiveKit-Raum für die gegebene Session. */
    suspend fun connect(sessionId: String, userId: String, displayName: String)

    /** Trennt die Voice-Verbindung und gibt alle Ressourcen frei. */
    suspend fun disconnect()

    /** Aktiviert oder deaktiviert das lokale Mikrofon. */
    fun setMicEnabled(enabled: Boolean)
}
