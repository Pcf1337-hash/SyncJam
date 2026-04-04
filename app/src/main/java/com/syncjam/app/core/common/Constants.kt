package com.syncjam.app.core.common

object Constants {
    // Production VPS IPv4 (port 80 via Caddy): ws://84.252.121.74
    // Production VPS IPv6 (port 80 via Caddy): ws://[2a0c:2500:571:5bc:63c7:4c96:3fd5:a0c8]
    // Dev (emulator local): ws://10.0.2.2:8080
    const val SYNC_SERVER_BASE_URL = "ws://84.252.121.74"
    const val SYNC_SERVER_HTTP_URL = "http://84.252.121.74"
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
