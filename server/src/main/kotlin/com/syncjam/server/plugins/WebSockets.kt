package com.syncjam.server.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 30.seconds
        maxFrameSize = 10 * 1024 * 1024
        masking = false
    }
}
