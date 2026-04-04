package com.syncjam.app.core.auth

import android.content.Context

private const val PREFS_NAME = "syncjam_session"
private const val KEY_EMAIL = "saved_email"
private const val KEY_PASSWORD = "saved_password"
private const val KEY_REMEMBER = "remember_me"

class SessionPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
}
