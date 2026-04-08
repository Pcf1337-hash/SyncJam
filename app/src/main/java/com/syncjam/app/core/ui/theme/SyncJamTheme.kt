package com.syncjam.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.syncjam.app.feature.settings.presentation.ThemeMode

@Composable
fun SyncJamTheme(
    seedColor: Color = DefaultSeedColor,
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    DynamicMaterialTheme(
        seedColor = seedColor,
        useDarkTheme = useDark,
        animate = true,
        style = PaletteStyle.Expressive,
        typography = SyncJamTypography,
        content = content
    )
}
