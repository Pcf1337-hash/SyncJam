package com.syncjam.app.feature.library.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncjam.app.db.dao.LocalTrackDao
import com.syncjam.app.db.dao.PlaylistDao
import com.syncjam.app.db.entity.LocalTrackEntity
import com.syncjam.app.db.entity.PlaylistEntity
import com.syncjam.app.db.entity.PlaylistTrackCrossRef
import com.syncjam.app.feature.library.data.CoverArtDownloader
import com.syncjam.app.feature.library.data.MediaStoreScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val scanner: MediaStoreScanner,
    private val dao: LocalTrackDao,
    private val playlistDao: PlaylistDao,
    private val coverArtDownloader: CoverArtDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadFromCache()
        observeFavorites()
        observePlaylists()
    }

    fun scanLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val entities = withContext(Dispatchers.IO) { scanner.scanTracks() }
                withContext(Dispatchers.IO) { dao.upsertTracks(entities) }
                val tracks = entities.map { it.toUi() }.applySorting(_uiState.value.sortOption).toImmutableList()
                _uiState.update { it.copy(isLoading = false, tracks = tracks, error = null) }
                // Trigger cover art download in background
                triggerCoverDownload()
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Scan failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Scan failed: ${e.message}") }
            }
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { state ->
            state.copy(
                sortOption = option,
                tracks = state.tracks.toList().applySorting(option).toImmutableList()
            )
        }
    }

    fun setTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleFavorite(trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val track = dao.getTrackById(trackId) ?: return@launch
            dao.setFavorite(trackId, !track.isFavorite)
        }
    }

    // ─── Playlist operations ────────────────────────────────────────────────

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                description = description.trim(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            playlistDao.upsertPlaylist(playlist)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = playlistDao.getPlaylistById(playlistId) ?: return@launch
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!playlistDao.isTrackInPlaylist(playlistId, trackId)) {
                playlistDao.addTrackToPlaylist(
                    PlaylistTrackCrossRef(
                        playlistId = playlistId,
                        trackId = trackId,
                        position = 0,
                        addedAt = System.currentTimeMillis()
                    )
                )
                // Update updatedAt
                val playlist = playlistDao.getPlaylistById(playlistId)
                if (playlist != null) {
                    playlistDao.upsertPlaylist(playlist.copy(updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    /** Tracks einer Playlist als Flow — für den In-Session-Bibliothek-Picker. */
    fun getPlaylistTracksFlow(playlistId: String): Flow<List<TrackUi>> =
        playlistDao.getTracksForPlaylist(playlistId).map { entities -> entities.map { it.toUi() } }

    fun triggerCoverDownload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingCovers = true) }
            try {
                withContext(Dispatchers.IO) { coverArtDownloader.downloadMissingCovers() }
                // Reload tracks to show new cover URLs
                loadFromCache()
            } catch (e: Exception) {
                Log.w("LibraryViewModel", "Cover download failed: ${e.message}")
            } finally {
                _uiState.update { it.copy(isDownloadingCovers = false) }
            }
        }
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private fun loadFromCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val entities = withContext(Dispatchers.IO) { dao.getAllTracksOnce() }
                if (entities.isNotEmpty()) {
                    val sorted = entities.map { e -> e.toUi() }
                        .applySorting(_uiState.value.sortOption)
                        .toImmutableList()
                    _uiState.update { it.copy(isLoading = false, tracks = sorted) }
                } else {
                    scanLibrary()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                scanLibrary()
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            dao.getFavoriteTracks().collect { entities ->
                val favs = entities.map { it.toUi() }.toImmutableList()
                _uiState.update { it.copy(favorites = favs) }
            }
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            playlistDao.getAllPlaylists().collect { entities ->
                val playlists = entities.map { playlist ->
                    PlaylistUi(
                        id = playlist.id,
                        name = playlist.name,
                        description = playlist.description,
                        trackCount = 0, // Updated separately
                        createdAt = playlist.createdAt
                    )
                }.toImmutableList()
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    private fun List<TrackUi>.applySorting(option: SortOption): List<TrackUi> = when (option) {
        SortOption.TITLE           -> sortedBy { it.title.lowercase() }
        SortOption.ARTIST          -> sortedBy { it.artist.lowercase() }
        SortOption.ALBUM           -> sortedBy { it.album.lowercase() }
        SortOption.DURATION        -> sortedBy { it.durationMs }
        SortOption.RECENTLY_PLAYED -> this
    }

    private fun LocalTrackEntity.toUi() = TrackUi(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        durationDisplay = formatDuration(durationMs),
        albumArtUri = remoteCoverUrl ?: albumArtUri,
        contentUri = contentUri,
        isFavorite = isFavorite
    )

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
