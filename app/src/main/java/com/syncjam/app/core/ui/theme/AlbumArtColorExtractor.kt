package com.syncjam.app.core.ui.theme

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlbumArtColorExtractor {

    private val fallbackColor = DefaultSeedColor

    suspend fun extractDominantColor(context: Context, uri: Uri?): Color {
        if (uri == null) return fallbackColor
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .allowHardware(false)
                    .size(100, 100)
                    .build()
                val result = context.imageLoader.execute(request)
                val bitmap = (result.image as? BitmapImage)?.bitmap
                    ?: return@withContext fallbackColor
                val palette = Palette.from(bitmap).generate()
                val dominantSwatch = palette.dominantSwatch
                    ?: palette.vibrantSwatch
                    ?: palette.mutedSwatch
                if (dominantSwatch != null) {
                    Color(dominantSwatch.rgb)
                } else {
                    Color(palette.getDominantColor(fallbackColor.value.toInt()))
                }
            } catch (e: Exception) {
                fallbackColor
            }
        }
    }
}
