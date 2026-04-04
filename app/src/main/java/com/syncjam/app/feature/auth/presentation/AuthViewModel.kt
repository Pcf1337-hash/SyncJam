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
                val session = supabase.auth.currentSessionOrNull()
                if (session != null) {
                    _uiState.update { it.copy(isLoggedIn = true, userName = supabase.auth.currentUserOrNull()?.email ?: "") }
                    return@launch
                }
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
                sessionPrefs.clearCredentials()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun signIn(email: String, password: String, rememberMe: Boolean = false) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Bitte E-Mail und Passwort eingeben.") }
            return
        }
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
                _uiState.update { it.copy(isLoading = false, error = friendlyAuthError(e)) }
            }
        }
    }

    private fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Bitte E-Mail und Passwort eingeben.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Das Passwort muss mindestens 6 Zeichen lang sein.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                sessionPrefs.saveCredentials(email.trim(), password)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true, userName = email.trim()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = friendlyAuthError(e)) }
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

    private fun friendlyAuthError(e: Exception): String {
        val msg = e.message?.lowercase() ?: return "Unbekannter Fehler. Bitte versuche es erneut."
        return when {
            "invalid login credentials" in msg || "invalid_credentials" in msg ->
                "E-Mail oder Passwort ist falsch."
            "email not confirmed" in msg ->
                "Bitte bestätige zuerst deine E-Mail-Adresse."
            "user already registered" in msg || "user_already_exists" in msg ->
                "Ein Konto mit dieser E-Mail existiert bereits."
            "password should be at least" in msg ->
                "Das Passwort muss mindestens 6 Zeichen lang sein."
            "unable to validate email address" in msg || "invalid email" in msg ->
                "Bitte gib eine gültige E-Mail-Adresse ein."
            "network" in msg || "timeout" in msg || "connect" in msg ->
                "Keine Verbindung. Überprüfe deine Internetverbindung."
            "rate limit" in msg || "too many requests" in msg ->
                "Zu viele Versuche. Bitte warte kurz und versuche es erneut."
            else -> "Anmeldung fehlgeschlagen. Bitte versuche es erneut."
        }
    }
}
