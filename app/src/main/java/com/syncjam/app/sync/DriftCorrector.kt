package com.syncjam.app.sync

import com.syncjam.app.core.common.Constants

data class DriftAction(
    val type: DriftType,
    val correctedPositionMs: Long = 0L,
    val playbackRate: Float = 1.0f
)

enum class DriftType { NONE, RATE_ADJUST, SEEK, FULL_RESYNC }

object DriftCorrector {
    fun evaluate(currentPositionMs: Long, expectedPositionMs: Long): DriftAction {
        val drift = expectedPositionMs - currentPositionMs
        val absDrift = Math.abs(drift)
        return when {
            absDrift < Constants.DRIFT_SMALL_MS -> DriftAction(DriftType.NONE)
            absDrift < Constants.DRIFT_MEDIUM_MS -> DriftAction(
                type = DriftType.RATE_ADJUST,
                playbackRate = if (drift > 0) 1.02f else 0.98f
            )
            absDrift < Constants.DRIFT_LARGE_MS -> DriftAction(
                type = DriftType.SEEK,
                correctedPositionMs = expectedPositionMs
            )
            else -> DriftAction(DriftType.FULL_RESYNC)
        }
    }
}
