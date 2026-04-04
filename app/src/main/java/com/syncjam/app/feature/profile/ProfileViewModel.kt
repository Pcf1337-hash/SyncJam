package com.syncjam.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncjam.app.core.auth.SessionPrefs
import com.syncjam.app.feature.auth.presentation.AuthEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val email: String = "",
    val displayName: String = "",
    val isSaved: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val sessionPrefs: SessionPrefs
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
            _uiState.update { it.copy(email = email, displayName = displayName) }
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

    fun signOut(authViewModel: com.syncjam.app.feature.auth.presentation.AuthViewModel) {
        authViewModel.onEvent(AuthEvent.SignOut)
    }
}
