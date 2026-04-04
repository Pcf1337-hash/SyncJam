package com.syncjam.app.feature.library.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncjam.app.db.dao.LocalTrackDao
import com.syncjam.app.feature.library.data.MediaStoreScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val scanner: MediaStoreScanner,
    private val dao: LocalTrackDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadFromCache()
    }

    fun scanLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val entities = withContext(Dispatchers.IO) { scanner.scanTracks() }
                withContext(Dispatchers.IO) { dao.upsertTracks(entities) }
                val tracks = entities.map { it.toUi() }.toImmutableList()
                _uiState.update { it.copy(isLoading = false, tracks = tracks, error = null) }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Scan failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Scan failed: ${e.message}") }
            }
        }
    }

    private fun loadFromCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val entities = withContext(Dispatchers.IO) { dao.getAllTracksOnce() }
                if (entities.isNotEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, tracks = entities.map { e -> e.toUi() }.toImmutableList())
                    }
                } else {
                    // No cache — trigger scan automatically
                    scanLibrary()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                scanLibrary()
            }
        }
    }

    private fun com.syncjam.app.db.entity.LocalTrackEntity.toUi() = TrackUi(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        durationDisplay = formatDuration(durationMs),
        albumArtUri = albumArtUri
    )

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
