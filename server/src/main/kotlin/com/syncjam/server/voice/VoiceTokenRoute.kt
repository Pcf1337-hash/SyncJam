package com.syncjam.server.voice

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// ── LiveKit-Konfiguration via Umgebungsvariablen ──────────────────────────────
// Auf Railway/Fly.io in den ENV-Variablen setzen:
//   LIVEKIT_API_KEY     → dein LiveKit API Key  (aus livekit.io → Project Settings)
//   LIVEKIT_API_SECRET  → dein LiveKit API Secret
//   LIVEKIT_URL         → wss://your-project.livekit.cloud  (für den Client)
private val LIVEKIT_API_KEY    = System.getenv("LIVEKIT_API_KEY")    ?: ""
private val LIVEKIT_API_SECRET = System.getenv("LIVEKIT_API_SECRET") ?: ""
val LIVEKIT_URL                = System.getenv("LIVEKIT_URL")        ?: ""

@Serializable
private data class TokenResponse(
    val token: String,
    val livekitUrl: String
)

/**
 * Generiert ein LiveKit-Access-Token (JWT, HS256) für den angegebenen User/Room.
 *
 * LiveKit Token-Struktur:
 * Header:  {"alg":"HS256","typ":"JWT"}
 * Payload: {"iss":API_KEY, "sub":userId, "jti":uuid, "nbf":0, "exp":now+3600,
 *            "name":displayName, "video":{"room":sessionId,"roomJoin":true,...}}
 */
private fun generateLiveKitToken(
    sessionId: String,
    userId: String,
    displayName: String,
    canPublish: Boolean = true
): String {
    val now = System.currentTimeMillis() / 1000L
    val header = buildJsonObject { put("alg", "HS256"); put("typ", "JWT") }
    val payload = buildJsonObject {
        put("iss", LIVEKIT_API_KEY)
        put("sub", userId)
        put("jti", "${userId}_${System.nanoTime()}")
        put("nbf", 0)
        put("exp", now + 3_600)
        put("name", displayName)
        putJsonObject("video") {
            put("room", sessionId)
            put("roomJoin", true)
            put("canPublish", canPublish)
            put("canSubscribe", true)
        }
    }

    fun encode(obj: kotlinx.serialization.json.JsonObject) =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(obj.toString().toByteArray(Charsets.UTF_8))

    val headerEncoded  = encode(header)
    val payloadEncoded = encode(payload)
    val signing        = "$headerEncoded.$payloadEncoded"

    val mac = Mac.getInstance("HmacSHA256").apply {
        init(SecretKeySpec(LIVEKIT_API_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
    }
    val signature = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(mac.doFinal(signing.toByteArray(Charsets.UTF_8)))

    return "$signing.$signature"
}

/** Registriert die /voice/token Route. */
fun Route.voiceTokenRoute() {
    get("/voice/token") {
        if (LIVEKIT_API_KEY.isBlank() || LIVEKIT_API_SECRET.isBlank()) {
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                mapOf("error" to "LiveKit nicht konfiguriert — LIVEKIT_API_KEY / LIVEKIT_API_SECRET setzen")
            )
            return@get
        }
        val sessionId   = call.request.queryParameters["sessionId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "sessionId fehlt"))
        val userId      = call.request.queryParameters["userId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId fehlt"))
        val displayName = call.request.queryParameters["displayName"] ?: userId

        val token = generateLiveKitToken(sessionId, userId, displayName)
        call.respond(TokenResponse(token = token, livekitUrl = LIVEKIT_URL))
    }
}
