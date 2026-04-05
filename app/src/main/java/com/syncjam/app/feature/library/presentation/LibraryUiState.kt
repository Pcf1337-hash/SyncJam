package com.syncjam.app.feature.library.presentation

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class SortOption { TITLE, ARTIST, ALBUM, DURATION, RECENTLY_PLAYED }
enum class LibraryTab { TRACKS, FAVORITES, PLAYLISTS }

@Immutable
data class LibraryUiState(
    val isLoading: Boolean = false,
    val tracks: ImmutableList<TrackUi> = persistentListOf(),
    val favorites: ImmutableList<TrackUi> = persistentListOf(),
    val playlists: ImmutableList<PlaylistUi> = persistentListOf(),
    val error: String? = null,
    val isGridView: Boolean = false,
    val sortOption: SortOption = SortOption.TITLE,
    val selectedTab: LibraryTab = LibraryTab.TRACKS,
    val isDownloadingCovers: Boolean = false
)

@Immutable
data class TrackUi(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val durationDisplay: String,
    val albumArtUri: String?,
    val contentUri: String,
    val isFavorite: Boolean = false
)

@Immutable
data class PlaylistUi(
    val id: String,
    val name: String,
    val description: String,
    val trackCount: Int,
    val createdAt: Long
)
