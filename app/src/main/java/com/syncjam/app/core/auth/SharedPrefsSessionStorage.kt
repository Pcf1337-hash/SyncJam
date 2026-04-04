package com.syncjam.app.core.auth

import android.content.Context

private const val PREFS_NAME = "syncjam_session"
private const val KEY_EMAIL = "saved_email"
private const val KEY_PASSWORD = "saved_password"
private const val KEY_REMEMBER = "remember_me"
private const val KEY_DISPLAY_NAME = "display_name"
private const val KEY_LAST_SESSION_CODE = "last_session_code"
private const val KEY_LAST_SESSION_IS_HOST = "last_session_is_host"

class SessionPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Auth credentials ──────────────────────────────────────────────────────

    fun saveCredentials(email: String, password: String) {
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_REMEMBER, true)
            .apply()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_EMAIL)
            .remove(KEY_PASSWORD)
            .putBoolean(KEY_REMEMBER, false)
            .apply()
    }

    fun getSavedEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getSavedPassword(): String? = prefs.getString(KEY_PASSWORD, null)
    fun isRememberMeEnabled(): Boolean = prefs.getBoolean(KEY_REMEMBER, false)

    // ── Display name ──────────────────────────────────────────────────────────

    fun saveDisplayName(name: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name.trim()).apply()
    }

    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)?.takeIf { it.isNotBlank() }

    // ── Last active session (for quick rejoin) ────────────────────────────────

    fun saveLastSession(code: String, isHost: Boolean) {
        prefs.edit()
            .putString(KEY_LAST_SESSION_CODE, code)
            .putBoolean(KEY_LAST_SESSION_IS_HOST, isHost)
            .apply()
    }

    fun getLastSessionCode(): String? = prefs.getString(KEY_LAST_SESSION_CODE, null)?.takeIf { it.isNotBlank() }
    fun isLastSessionHost(): Boolean = prefs.getBoolean(KEY_LAST_SESSION_IS_HOST, false)

    fun clearLastSession() {
        prefs.edit()
            .remove(KEY_LAST_SESSION_CODE)
            .remove(KEY_LAST_SESSION_IS_HOST)
            .apply()
    }
}
