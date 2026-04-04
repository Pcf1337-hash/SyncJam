package com.syncjam.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val httpClient: HttpClient
) : ViewModel() {

    private val _updateRelease = MutableStateFlow<com.syncjam.app.core.update.AppRelease?>(null)

    val uiState = combine(
        sessionHistoryDao.getRecentSessions(),
        _updateRelease
    ) { sessions, update ->
        HomeUiState(recentSessions = sessions, availableUpdate = update)
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
}
