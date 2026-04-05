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
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
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

@Serializable
private data class RenameRequest(val sessionName: String, val hostId: String)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionHistoryDao: SessionHistoryDao,
    private val httpClient: HttpClient,
    private val sessionPrefs: SessionPrefs
) : ViewModel() {

    private val _updateRelease = MutableStateFlow<com.syncjam.app.core.update.AppRelease?>(null)
    private val _publicSessions = MutableStateFlow<List<PublicSessionUi>>(emptyList())
    private val _detectedClipboardCode = MutableStateFlow<String?>(null)
    val detectedClipboardCode = _detectedClipboardCode.asStateFlow()
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

    /** Löscht eine Session aus der lokalen History UND vom Server (best-effort). */
    fun deleteSession(sessionId: String, sessionCode: String, userId: String) {
        viewModelScope.launch {
            // Lokal immer löschen
            runCatching { sessionHistoryDao.deleteById(sessionId) }
            // Server-Löschung best-effort (kein Fehler wenn offline)
            runCatching {
                httpClient.delete("${Constants.SYNC_SERVER_HTTP_URL}/session/$sessionCode?userId=$userId")
            }
        }
    }

    /** Benennt eine laufende Session auf dem Server um (best-effort, kein DB-Eintrag nötig). */
    fun renameSession(sessionCode: String, newName: String, hostId: String) {
        viewModelScope.launch {
            runCatching {
                httpClient.patch("${Constants.SYNC_SERVER_HTTP_URL}/session/$sessionCode") {
                    contentType(ContentType.Application.Json)
                    setBody(RenameRequest(sessionName = newName, hostId = hostId))
                }
                // Public-Sessions-Liste neu laden damit neuer Name sichtbar ist
                fetchPublicSessions()
            }
        }
    }

    /** Called when a valid 6-char session code is found in the clipboard. */
    fun onClipboardCodeDetected(code: String) {
        _detectedClipboardCode.update { code }
    }

    /** Dismiss the clipboard paste dialog without joining. */
    fun dismissClipboardDialog() {
        _detectedClipboardCode.update { null }
    }
}
