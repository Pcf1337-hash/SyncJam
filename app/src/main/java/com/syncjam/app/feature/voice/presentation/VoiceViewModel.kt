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
     * Push-to-Talk: Taste gedrückt → Verbindet (falls nötig) und aktiviert Mikrofon.
     * Beim ersten Drücken wird die LiveKit-Verbindung aufgebaut.
     */
    fun onPttPressed(sessionId: String, userId: String, displayName: String) {
        viewModelScope.launch {
            if (!voiceState.value.isActive) {
                voiceRepository.connect(sessionId, userId, displayName)
            }
            voiceRepository.setMicEnabled(true)
        }
    }

    /**
     * Push-to-Talk: Taste losgelassen → Mikrofon sofort wieder stumm.
     * Verbindung bleibt bestehen (für schnelles Re-Aktivieren).
     */
    fun onPttReleased() {
        voiceRepository.setMicEnabled(false)
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
