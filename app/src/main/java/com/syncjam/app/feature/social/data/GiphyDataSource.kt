package com.syncjam.app.feature.social.data

import com.syncjam.app.feature.social.domain.model.GifResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source für Giphy GIF-Suche.
 *
 * Die Giphy UI SDK (com.giphy.sdk:ui) stellt keinen direkten programmatischen API-Zugriff
 * bereit — sie ist für den GiphyDialogFragment (Picker-Dialog) gedacht.
 * Für rohen API-Zugriff wäre `com.giphy.sdk:core` notwendig.
 *
 * Aktuell: Stub-Implementation. GIF-Picker nutzt GiphyDialogFragment aus GifPicker.kt direkt.
 * TODO: com.giphy.sdk:core hinzufügen und API-Key in BuildConfig konfigurieren für direkte Suche.
 */
@Singleton
class GiphyDataSource @Inject constructor() {

    suspend fun fetchTrending(limit: Int = 20): List<GifResult> {
        // GiphyDialogFragment wird direkt im GifPicker Composable verwendet
        return emptyList()
    }

    suspend fun search(query: String, limit: Int = 20): List<GifResult> {
        // GiphyDialogFragment wird direkt im GifPicker Composable verwendet
        return emptyList()
    }
}
