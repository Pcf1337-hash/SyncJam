package com.syncjam.app.feature.settings.presentation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "syncjam_settings")

enum class AudioQuality { LOW, MEDIUM, HIGH }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class SettingsUiState(
    val audioQuality: AudioQuality = AudioQuality.MEDIUM,
    val notificationSounds: Boolean = true,
    val vibration: Boolean = true,
    val pushToTalk: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val cacheSize: String = "Wird berechnet..."
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        private val KEY_NOTIFICATION_SOUNDS = booleanPreferencesKey("notification_sounds")
        private val KEY_VIBRATION = booleanPreferencesKey("vibration")
        private val KEY_PTT = booleanPreferencesKey("push_to_talk")
        private val KEY_THEME = stringPreferencesKey("theme_mode")
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            context.settingsDataStore.data.collect { prefs ->
                _uiState.value = SettingsUiState(
                    audioQuality = runCatching {
                        AudioQuality.valueOf(prefs[KEY_AUDIO_QUALITY] ?: AudioQuality.MEDIUM.name)
                    }.getOrDefault(AudioQuality.MEDIUM),
                    notificationSounds = prefs[KEY_NOTIFICATION_SOUNDS] ?: true,
                    vibration = prefs[KEY_VIBRATION] ?: true,
                    pushToTalk = prefs[KEY_PTT] ?: false,
                    themeMode = runCatching {
                        ThemeMode.valueOf(prefs[KEY_THEME] ?: ThemeMode.DARK.name)
                    }.getOrDefault(ThemeMode.DARK),
                    cacheSize = _uiState.value.cacheSize
                )
            }
        }
        calculateCacheSize()
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            context.settingsDataStore.edit { it[KEY_AUDIO_QUALITY] = quality.name }
        }
    }

    fun setNotificationSounds(enabled: Boolean) {
        viewModelScope.launch {
            context.settingsDataStore.edit { it[KEY_NOTIFICATION_SOUNDS] = enabled }
        }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch {
            context.settingsDataStore.edit { it[KEY_VIBRATION] = enabled }
        }
    }

    fun setPushToTalk(enabled: Boolean) {
        viewModelScope.launch {
            context.settingsDataStore.edit { it[KEY_PTT] = enabled }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            context.settingsDataStore.edit { it[KEY_THEME] = mode.name }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            context.cacheDir.deleteRecursively()
            calculateCacheSize()
        }
    }

    private fun calculateCacheSize() {
        viewModelScope.launch {
            val bytes = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val mb = bytes / (1024.0 * 1024.0)
            _uiState.update { it.copy(cacheSize = String.format("%.1f MB", mb)) }
        }
    }
}
