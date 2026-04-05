package com.syncjam.app.sync

import com.syncjam.app.core.common.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NTP-style clock synchronisation against the SyncJam server.
 *
 * Algorithm:
 *   Client sends GET /time (t1 = send timestamp).
 *   Server responds with its current epoch millis.
 *   Client records receive time (t4).
 *   RTT   = t4 - t1
 *   Offset = serverTime - (t1 + RTT / 2)
 *
 * On session join: 5 samples, best (minimum-RTT) wins.
 * Background refresh: 3 samples every 30 s.
 */
@Singleton
class NtpClockSync @Inject constructor() {

    private var clockOffset: Long = 0L
    private var syncJob: Job? = null
    private var httpClient: HttpClient? = null

    data class NtpSample(val rtt: Long, val offset: Long)

    /** Must be called once with the Ktor client before any sync. */
    fun init(client: HttpClient) {
        httpClient = client
    }

    /** Full sync on session join: 5 samples, keep the minimum-RTT result. */
    suspend fun syncOnJoin(serverHttpUrl: String) = withContext(Dispatchers.IO) {
        val samples = (1..Constants.NTP_SAMPLE_COUNT).mapNotNull {
            runCatching { measureSample(serverHttpUrl) }.getOrNull()
        }
        if (samples.isNotEmpty()) {
            clockOffset = samples.minByOrNull { it.rtt }!!.offset
        }
    }

    /** Start periodic background refresh every 30 s (3 samples each round). */
    fun startPeriodicSync(scope: CoroutineScope, serverHttpUrl: String) {
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(Constants.SYNC_REFRESH_INTERVAL_MS)
                val samples = (1..3).mapNotNull {
                    runCatching { measureSample(serverHttpUrl) }.getOrNull()
                }
                if (samples.isNotEmpty()) {
                    clockOffset = samples.minByOrNull { it.rtt }!!.offset
                }
            }
        }
    }

    /** Cancel the background sync job (call on session leave). */
    fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
    }

    /** Reset offset (e.g. on disconnect). */
    fun reset() {
        stopPeriodicSync()
        clockOffset = 0L
    }

    /**
     * Returns the current time corrected to server clock domain.
     * All sync commands should use this instead of [System.currentTimeMillis].
     */
    fun getServerTime(): Long = System.currentTimeMillis() + clockOffset

    /** Raw offset value — useful for diagnostics. */
    fun getClockOffset(): Long = clockOffset

    // ── Manual offset apply (legacy / testing) ────────────────────────────────

    fun applyOffset(offset: Long) {
        clockOffset = offset
    }

    fun calculateOffset(t1: Long, t2: Long, t3: Long, t4: Long): Long =
        ((t2 - t1) + (t3 - t4)) / 2

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun measureSample(serverHttpUrl: String): NtpSample {
        val client = requireNotNull(httpClient) { "NtpClockSync not initialised — call init() first" }
        val t1 = System.currentTimeMillis()
        val serverTime = client.get("$serverHttpUrl/time").body<Long>()
        val t4 = System.currentTimeMillis()
        val rtt = t4 - t1
        val offset = serverTime - (t1 + rtt / 2)
        return NtpSample(rtt = rtt, offset = offset)
    }
}
