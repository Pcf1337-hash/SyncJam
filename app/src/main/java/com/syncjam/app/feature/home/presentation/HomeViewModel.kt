package com.syncjam.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncjam.app.core.auth.SessionPrefs
import com.syncjam.app.core.common.Constants
import com.syncjam.app.core.update.checkForUpdate
import com.syncjam.app.db.dao.SessionHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class PublicSessionResponse(
    val sessionCode: String,
    val sessionName: String,
    val participantCount: Int,
    val currentTrackTitle: String? = null,
    val currentTrackArtist: String? = null,
    val isPasswordProtected: Boolean = false,
    val createdAt: Long = 0L
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionHistoryDao: SessionHistoryDao,
    private val httpClient: HttpClient,
    private val sessionPrefs: SessionPrefs
) : ViewModel() {

    private val _updateRelease = MutableStateFlow<com.syncjam.app.core.update.AppRelease?>(null)
    private val _publicSessions = MutableStateFlow<List<PublicSessionUi>>(emptyList())
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
        _extraState,
        _publicSessions
    ) { sessions, update, (lastCode, isHost, name), publicSessions ->
        HomeUiState(
            recentSessions = sessions,
            availableUpdate = update,
            lastSessionCode = lastCode,
            isLastSessionHost = isHost,
            displayName = name,
            publicSessions = publicSessions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        checkForUpdates()
        fetchPublicSessions()
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

    fun fetchPublicSessions() {
        viewModelScope.launch {
            runCatching {
                val list = httpClient.get("${Constants.SYNC_SERVER_HTTP_URL}/sessions/public")
                    .body<List<PublicSessionResponse>>()
                _publicSessions.update {
                    list.map { s ->
                        PublicSessionUi(
                            sessionCode = s.sessionCode,
                            sessionName = s.sessionName,
                            participantCount = s.participantCount,
                            currentTrackTitle = s.currentTrackTitle,
                            currentTrackArtist = s.currentTrackArtist,
                            isPasswordProtected = s.isPasswordProtected,
                            createdAt = s.createdAt
                        )
                    }
                }
            }
        }
    }
}
