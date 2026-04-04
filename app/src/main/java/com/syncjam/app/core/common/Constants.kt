package com.syncjam.app.core.common

object Constants {
    // Production VPS Netcup (159.195.63.246, port 8080, keine Portblockierung)
    // Dev (emulator local): ws://10.0.2.2:8080
    const val SYNC_SERVER_BASE_URL = "ws://159.195.63.246:8080"
    const val SYNC_SERVER_HTTP_URL = "http://159.195.63.246:8080"
    const val SUPABASE_URL = "https://fvaxkleqbxuafbkymgmj.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ2YXhrbGVxYnh1YWZia3ltZ21qIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzUyNTE5MTksImV4cCI6MjA5MDgyNzkxOX0.gPIcos74Ow51PWa4IHS9MoAhrs2TJT2QubJzKyQzF7k"
    const val LIVEKIT_URL = "wss://YOUR_LIVEKIT_URL"
    const val GIPHY_API_KEY = "YOUR_GIPHY_KEY"
    const val HEARTBEAT_INTERVAL_MS = 2000L
    const val PING_INTERVAL_MS = 15000L
    const val SYNC_REFRESH_INTERVAL_MS = 30000L
    const val NTP_SAMPLE_COUNT = 5
    const val DRIFT_SMALL_MS = 150L
    const val DRIFT_MEDIUM_MS = 500L
    const val DRIFT_LARGE_MS = 2000L
    const val MAX_REACTIONS_ON_SCREEN = 20
    const val GIF_RATE_LIMIT_MS = 5000L
    const val CHAT_MAX_CHARS = 500
    const val MAX_PARTICIPANTS = 8
    const val SESSION_AUTO_END_MINUTES = 30L
}
