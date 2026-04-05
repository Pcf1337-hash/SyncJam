package com.syncjam.app.feature.voice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncjam.app.feature.voice.domain.model.VoiceState
import com.syncjam.app.feature.voice.domain.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    val voiceState: StateFlow<VoiceState> = voiceRepository.voiceState

    /**
     * True wenn > 4 Teilnehmer → PTT-Modus wird empfohlen.
     * Computed direkt aus VoiceState.isPttRecommended, kein eigener StateFlow nötig
     * da VoiceOverlay sowieso voiceState beobachtet.
     */
    val isPttMode: Boolean
        get() = voiceState.value.isPttRecommended

    /**
     * Music Ducking Flow — true wenn jemand spricht.
     * SessionViewModel abonniert dies: wenn true → player.volume = 0.25f
     */
    val anyoneSpeaking: StateFlow<Boolean> = voiceRepository.anyoneSpeaking

    /**
     * Push-to-Talk: Taste gedrückt → Verbindet (falls nötig) und aktiviert Mikrofon.
     * Beim ersten Drücken wird die LiveKit-Verbindung aufgebaut.
     */
    fun onPttPressed(sessionId: String, userId: String, displayName: String) {
        viewModelScope.launch {
            if (!voiceState.value.isActive) {
                voiceRepository.connect(sessionId, userId, displayName)
            }
            voiceRepository.setMicrophoneEnabled(true)
        }
    }

    /**
     * Push-to-Talk: Taste losgelassen → Mikrofon sofort wieder stumm.
     * Verbindung bleibt bestehen (für schnelles Re-Aktivieren).
     */
    fun onPttReleased() {
        voiceRepository.setMicrophoneEnabled(false)
    }

    /**
     * Verbindet ohne sofort das Mikrofon zu aktivieren (für Auto-Join beim Betreten einer Session).
     * Mikrofon bleibt stumm bis der User PTT drückt oder manuell aktiviert.
     */
    fun connect(sessionId: String, userId: String, displayName: String) {
        viewModelScope.launch {
            voiceRepository.connect(sessionId, userId, displayName)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            voiceRepository.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
