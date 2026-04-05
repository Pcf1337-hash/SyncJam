package com.syncjam.app.feature.social.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class GifResult(
    val id: String,
    val url: String,
    val previewUrl: String,
    val title: String
)
