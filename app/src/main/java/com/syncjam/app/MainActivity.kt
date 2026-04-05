package com.syncjam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.syncjam.app.feature.onboarding.OnboardingViewModel
import com.syncjam.app.feature.settings.presentation.settingsDataStore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Determine start destination by reading DataStore before showing any UI.
            // null = still loading, true = completed, false = not yet shown.
            var startDestination by remember { mutableStateOf<Route?>(null) }

            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

            LaunchedEffect(Unit) {
                val key = OnboardingViewModel.KEY_ONBOARDING_COMPLETED
                applicationContext.settingsDataStore.data.collect { preferences ->
                    startDestination = if (preferences[key] == true) {
                        Route.Login
                    } else {
                        Route.Onboarding
                    }
                    // Stop after first emission — the start destination is fixed for
                    // the lifetime of this Activity instance.
                    return@collect
                }
            }

            // Only mount the NavGraph once the DataStore value is known,
            // avoiding a flash of the wrong start screen.
            startDestination?.let { destination ->
                SyncJamNavGraph(
                    startDestination = destination,
                    isExpandedScreen = isExpandedScreen
                )
            }
        }
    }
}
