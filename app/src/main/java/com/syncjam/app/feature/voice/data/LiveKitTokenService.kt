package com.syncjam.app.feature.voice.data

import android.util.Log
import com.syncjam.app.core.common.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class LiveKitTokenResponse(
    val token: String,
    val livekitUrl: String
)

@Singleton
class LiveKitTokenService @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private const val TAG = "LiveKitTokenService"
    }

    /**
     * Ruft ein LiveKit-Room-Token vom Sync-Server ab.
     *
     * Der Server generiert das JWT mit LIVEKIT_API_KEY + LIVEKIT_API_SECRET.
     * Gibt null zurück wenn der Server nicht konfiguriert ist (LIVEKIT_API_KEY leer).
     *
     * Server-Konfiguration (Railway/Fly.io ENV-Vars):
     *   LIVEKIT_API_KEY     = dein LiveKit API Key
     *   LIVEKIT_API_SECRET  = dein LiveKit API Secret
     *   LIVEKIT_URL         = wss://your-project.livekit.cloud
     *
     * LiveKit-Konto: https://livekit.io → kostenloser Free-Tier verfügbar
     */
    suspend fun fetchToken(
        sessionId: String,
        userId: String,
        displayName: String
    ): LiveKitTokenResponse? {
        return runCatching {
            val url = "${Constants.SYNC_SERVER_HTTP_URL}/voice/token" +
                    "?sessionId=${sessionId}&userId=${userId}&displayName=${displayName}"
            httpClient.get(url).body<LiveKitTokenResponse>()
        }.onFailure {
            Log.w(TAG, "Token-Abruf fehlgeschlagen: ${it.message} — Stub-Modus aktiv")
        }.getOrNull()
    }
}
