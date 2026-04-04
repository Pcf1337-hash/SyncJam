package com.syncjam.app.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncjam.app.core.auth.SessionPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val sessionPrefs: SessionPrefs
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SignIn -> signIn(event.email, event.password, event.rememberMe)
            is AuthEvent.SignUp -> signUp(event.email, event.password)
            is AuthEvent.SignOut -> signOut()
            is AuthEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            try {
                // First check if Supabase has an in-memory session
                val session = supabase.auth.currentSessionOrNull()
                if (session != null) {
                    _uiState.update { it.copy(isLoggedIn = true, userName = supabase.auth.currentUserOrNull()?.email ?: "") }
                    return@launch
                }
                // Fall back to saved credentials (remember me)
                if (sessionPrefs.isRememberMeEnabled()) {
                    val email = sessionPrefs.getSavedEmail()?.takeIf { it.isNotBlank() } ?: return@launch
                    val password = sessionPrefs.getSavedPassword()?.takeIf { it.isNotBlank() } ?: return@launch
                    _uiState.update { it.copy(isLoading = true) }
                    supabase.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true, userName = email) }
                }
            } catch (_: Exception) {
                // Silent failure — clear invalid saved credentials, user can still guest-login
                sessionPrefs.clearCredentials()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun signIn(email: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                if (rememberMe) {
                    sessionPrefs.saveCredentials(email.trim(), password)
                } else {
                    sessionPrefs.clearCredentials()
                }
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true, userName = email.trim()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Anmeldung fehlgeschlagen") }
            }
        }
    }

    private fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                // signUpWith already creates a session (mailer_autoconfirm=true).
                // Always save credentials so checkSession can restore the session after restart.
                sessionPrefs.saveCredentials(email.trim(), password)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true, userName = email.trim()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Registrierung fehlgeschlagen") }
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            try { supabase.auth.signOut() } catch (_: Exception) {}
            sessionPrefs.clearCredentials()
            _uiState.update { it.copy(isLoggedIn = false, userName = null) }
        }
    }
}
