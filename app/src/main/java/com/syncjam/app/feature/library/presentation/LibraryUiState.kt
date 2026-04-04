package com.syncjam.app.feature.library.presentation

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class LibraryUiState(
    val isLoading: Boolean = false,
    val tracks: ImmutableList<TrackUi> = persistentListOf(),
    val error: String? = null
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
    val contentUri: String
)
