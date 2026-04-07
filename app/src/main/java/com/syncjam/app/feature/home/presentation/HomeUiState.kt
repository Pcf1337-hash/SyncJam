package com.syncjam.app.feature.home.presentation

import androidx.compose.runtime.Immutable
import com.syncjam.app.core.update.AppRelease
import com.syncjam.app.db.entity.SessionHistoryEntity

@Immutable
data class NowPlayingUi(
    val title: String,
    val artist: String,
    val albumArtUri: String?,
    val isPlaying: Boolean,
    val progress: Float   // 0f..1f
)

@Immutable
data class HomeUiState(
    val recentSessions: List<SessionHistoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val availableUpdate: AppRelease? = null,
    val lastSessionCode: String? = null,
    val isLastSessionHost: Boolean = false,
    val displayName: String = "",
    val publicSessions: List<PublicSessionUi> = emptyList(),
    val nowPlaying: NowPlayingUi? = null
)

@Immutable
data class PublicSessionUi(
    val sessionCode: String,
    val sessionName: String,
    val participantCount: Int,
    val currentTrackTitle: String?,
    val currentTrackArtist: String?,
    val isPasswordProtected: Boolean,
    val createdAt: Long,
    val isOwnSession: Boolean = false
)
