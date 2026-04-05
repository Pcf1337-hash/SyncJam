package com.syncjam.app.sync

import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.syncjam.app.core.common.Constants
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Categorised result of a drift correction attempt.
 * Replaces the old [DriftAction] data class — now a simple enum, matching CLAUDE.md.
 */
enum class DriftAction { NONE, RATE_ADJUSTMENT, SEEK, FULL_RESYNC }

/**
 * Applies drift corrections directly to an [ExoPlayer] instance.
 *
 * Thresholds (from CLAUDE.md):
 *   < 150 ms  → no action
 *   150–500 ms → adaptive playback rate (1.02× or 0.98×), pitch always 1.0
 *   500–2000 ms → seek correction (NEVER below 500 ms — audible glitch!)
 *   > 2000 ms  → request full re-sync via [onRequestResync] callback
 */
@Singleton
class DriftCorrector @Inject constructor() {

    companion object {
        private const val RATE_FAST   = 1.02f
        private const val RATE_SLOW   = 0.98f
        private const val RATE_NORMAL = 1.0f
    }

    /**
     * Evaluate and immediately apply the appropriate correction to [player].
     *
     * @param player         The ExoPlayer to correct.
     * @param driftMs        Positive = client is BEHIND server (needs to speed up).
     *                       Negative = client is AHEAD  of server (needs to slow down).
     * @param onRequestResync Called when drift > 2000 ms — caller should request a
     *                       [SyncCommand.StateSnapshot] from the server.
     * @return The [DriftAction] that was applied.
     */
    fun correct(player: ExoPlayer, driftMs: Long, onRequestResync: () -> Unit): DriftAction {
        val absDrift = abs(driftMs)
        return when {
            absDrift < Constants.DRIFT_SMALL_MS -> {
                ensureNormalRate(player)
                DriftAction.NONE
            }
            absDrift < Constants.DRIFT_MEDIUM_MS -> {
                // driftMs > 0 → client behind → speed up; < 0 → client ahead → slow down
                val rate = if (driftMs > 0) RATE_FAST else RATE_SLOW
                player.setPlaybackParameters(PlaybackParameters(rate, /* pitch */ 1.0f))
                DriftAction.RATE_ADJUSTMENT
            }
            absDrift < Constants.DRIFT_LARGE_MS -> {
                ensureNormalRate(player)
                val targetPos = player.currentPosition + driftMs
                if (targetPos in 0L..player.duration) player.seekTo(targetPos)
                DriftAction.SEEK
            }
            else -> {
                ensureNormalRate(player)
                onRequestResync()
                DriftAction.FULL_RESYNC
            }
        }
    }

    /**
     * Evaluate drift without touching the player — returns the recommended action.
     * Useful for unit testing or when the caller wants to decide what to do.
     */
    fun evaluate(currentPositionMs: Long, expectedPositionMs: Long): DriftAction {
        val drift = expectedPositionMs - currentPositionMs
        val absDrift = abs(drift)
        return when {
            absDrift < Constants.DRIFT_SMALL_MS  -> DriftAction.NONE
            absDrift < Constants.DRIFT_MEDIUM_MS -> DriftAction.RATE_ADJUSTMENT
            absDrift < Constants.DRIFT_LARGE_MS  -> DriftAction.SEEK
            else                                  -> DriftAction.FULL_RESYNC
        }
    }

    /** Restore normal speed/pitch on [player]. */
    fun resetRate(player: ExoPlayer) = ensureNormalRate(player)

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun ensureNormalRate(player: ExoPlayer) {
        if (player.playbackParameters.speed != RATE_NORMAL) {
            player.setPlaybackParameters(PlaybackParameters(RATE_NORMAL, 1.0f))
        }
    }
}
