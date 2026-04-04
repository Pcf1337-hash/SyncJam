package com.syncjam.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncjam.app.core.auth.SessionPrefs
import com.syncjam.app.core.update.checkForUpdate
import com.syncjam.app.db.dao.SessionHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionHistoryDao: SessionHistoryDao,
    private val httpClient: HttpClient,
    private val sessionPrefs: SessionPrefs
) : ViewModel() {

    private val _updateRelease = MutableStateFlow<com.syncjam.app.core.update.AppRelease?>(null)
    private val _extraState = MutableStateFlow(
        Triple(
            sessionPrefs.getLastSessionCode(),
            sessionPrefs.isLastSessionHost(),
            sessionPrefs.getDisplayName() ?: ""
        )
    )

    val uiState = combine(
        sessionHistoryDao.getRecentSessions(),
        _updateRelease,
        _extraState
    ) { sessions, update, (lastCode, isHost, name) ->
        HomeUiState(
            recentSessions = sessions,
            availableUpdate = update,
            lastSessionCode = lastCode,
            isLastSessionHost = isHost,
            displayName = name
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        checkForUpdates()
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            val release = checkForUpdate(httpClient)
            _updateRelease.update { release }
        }
    }

    fun dismissUpdate() {
        _updateRelease.update { null }
    }

    fun refreshLastSession() {
        _extraState.update {
            Triple(
                sessionPrefs.getLastSessionCode(),
                sessionPrefs.isLastSessionHost(),
                sessionPrefs.getDisplayName() ?: ""
            )
        }
    }
}
