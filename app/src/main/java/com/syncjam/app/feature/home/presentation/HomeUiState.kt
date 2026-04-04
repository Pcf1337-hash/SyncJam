package com.syncjam.app.feature.home.presentation

import androidx.compose.runtime.Immutable
import com.syncjam.app.core.update.AppRelease
import com.syncjam.app.db.entity.SessionHistoryEntity

@Immutable
data class HomeUiState(
    val recentSessions: List<SessionHistoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val availableUpdate: AppRelease? = null,
    val lastSessionCode: String? = null,
    val isLastSessionHost: Boolean = false,
    val displayName: String = ""
)
