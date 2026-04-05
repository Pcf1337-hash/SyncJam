package com.syncjam.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.livekit.android.LiveKit

@HiltAndroidApp
class SyncJamApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // LiveKit SDK initialisieren — muss vor Room.create() aufgerufen werden.
        // Lädt native WebRTC-Bibliotheken und setzt internen Context.
        LiveKit.init(appContext = this)
    }
}
