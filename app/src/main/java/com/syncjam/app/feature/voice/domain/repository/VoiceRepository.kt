package com.syncjam.app.feature.voice.domain.repository

import com.syncjam.app.feature.voice.domain.model.VoiceState
import kotlinx.coroutines.flow.StateFlow

interface VoiceRepository {
    val voiceState: StateFlow<VoiceState>

    /**
     * Computed Flow: true wenn mindestens ein Teilnehmer spricht.
     * SessionViewModel abonniert diesen Flow für Music Ducking.
     */
    val anyoneSpeaking: StateFlow<Boolean>

    /** Verbindet mit dem LiveKit-Raum für die gegebene Session. */
    suspend fun connect(sessionId: String, userId: String, displayName: String)

    /** Trennt die Voice-Verbindung und gibt alle Ressourcen frei. */
    suspend fun disconnect()

    /** Aktiviert oder deaktiviert das lokale Mikrofon. */
    fun setMicEnabled(enabled: Boolean)

    /**
     * Alias für setMicEnabled — sprachlich an LiveKit SDK angepasst.
     * Wird von PTT-Logik im ViewModel genutzt.
     */
    fun setMicrophoneEnabled(enabled: Boolean) = setMicEnabled(enabled)
}
