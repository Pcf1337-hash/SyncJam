package com.syncjam.app.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncjam.app.core.auth.SessionPrefs
import com.syncjam.app.core.common.Constants
import com.syncjam.app.feature.auth.presentation.AuthEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class ProfileUiState(
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val isSaved: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val avatarError: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient,
    private val sessionPrefs: SessionPrefs,
    private val httpClient: HttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val email = supabase.auth.currentUserOrNull()?.email ?: ""
            val displayName = sessionPrefs.getDisplayName()
                ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
            val avatarUrl = sessionPrefs.getAvatarUrl()
            _uiState.update { it.copy(email = email, displayName = displayName, avatarUrl = avatarUrl) }
        }
    }

    fun onDisplayNameChange(name: String) {
        _uiState.update { it.copy(displayName = name, isSaved = false) }
    }

    fun saveDisplayName() {
        val name = _uiState.value.displayName.trim().ifBlank { return }
        sessionPrefs.saveDisplayName(name)
        _uiState.update { it.copy(isSaved = true) }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, avatarError = null) }
            runCatching {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: sessionPrefs.getDisplayName()?.replace(" ", "_")
                    ?: "guest"
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: error("Konnte Bild nicht lesen")
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when (mimeType) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                val responseBody = httpClient.submitFormWithBinaryData(
                    url = "${Constants.SYNC_SERVER_HTTP_URL}/upload/avatars",
                    formData = formData {
                        append("file", bytes, Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=\"avatar_$userId.$ext\"")
                        })
                    }
                ).body<String>()
                val json = Json.parseToJsonElement(responseBody)
                val path = json.jsonObject["url"]?.jsonPrimitive?.content ?: error("Kein URL in Antwort")
                val fullUrl = "${Constants.SYNC_SERVER_HTTP_URL}$path"
                sessionPrefs.saveAvatarUrl(fullUrl)
                _uiState.update { it.copy(avatarUrl = fullUrl, isUploadingAvatar = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isUploadingAvatar = false, avatarError = "Upload fehlgeschlagen: ${e.message}") }
            }
        }
    }

    fun removeAvatar() {
        sessionPrefs.clearAvatarUrl()
        _uiState.update { it.copy(avatarUrl = null) }
    }

    fun signOut(authViewModel: com.syncjam.app.feature.auth.presentation.AuthViewModel) {
        authViewModel.onEvent(AuthEvent.SignOut)
    }
}
