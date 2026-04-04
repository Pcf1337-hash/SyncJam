package com.syncjam.app.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

@Composable
fun SyncJamTheme(
    seedColor: Color = DefaultSeedColor,
    content: @Composable () -> Unit
) {
    DynamicMaterialTheme(
        seedColor = seedColor,
        useDarkTheme = true,
        animate = true,
        style = PaletteStyle.Expressive,
        typography = SyncJamTypography,
        content = content
    )
}
