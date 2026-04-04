package com.syncjam.app.feature.auth.presentation

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userName: String? = null,
    val error: String? = null
)

sealed interface AuthEvent {
    data class SignIn(val email: String, val password: String, val rememberMe: Boolean = false) : AuthEvent
    data class SignUp(val email: String, val password: String) : AuthEvent
    data object SignOut : AuthEvent
    data object ClearError : AuthEvent
}
