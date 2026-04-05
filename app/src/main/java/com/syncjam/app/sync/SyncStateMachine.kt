package com.syncjam.app.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lifecycle states for a sync session.
 * Named [SessionSyncState] to avoid collision with [SyncState] (the data class in SyncEngine).
 */
enum class SessionSyncState {
    IDLE,
    CONNECTING,
    SYNCING,
    PLAYING,
    PAUSED,
    DISCONNECTED,
    RECONNECTING
}

/** Events that trigger state transitions. */
sealed interface SyncStateEvent {
    data object Connect    : SyncStateEvent
    data object Connected  : SyncStateEvent
    data object SyncComplete : SyncStateEvent
    data object Play       : SyncStateEvent
    data object Pause      : SyncStateEvent
    data object Disconnect : SyncStateEvent
    data object Reconnect  : SyncStateEvent
    data object Reset      : SyncStateEvent
}

/**
 * Finite-state machine that tracks the lifecycle of a SyncJam session.
 *
 * State transitions:
 *
 *   IDLE         --Connect-->      CONNECTING
 *   CONNECTING   --Connected-->    SYNCING
 *   CONNECTING   --Disconnect-->   DISCONNECTED
 *   SYNCING      --SyncComplete--> PAUSED
 *   SYNCING      --Play-->         PLAYING
 *   SYNCING      --Disconnect-->   DISCONNECTED
 *   PLAYING      --Pause-->        PAUSED
 *   PLAYING      --Disconnect-->   DISCONNECTED
 *   PAUSED       --Play-->         PLAYING
 *   PAUSED       --Disconnect-->   DISCONNECTED
 *   DISCONNECTED --Reconnect-->    RECONNECTING
 *   DISCONNECTED --Reset-->        IDLE
 *   RECONNECTING --Connected-->    SYNCING
 *   RECONNECTING --Disconnect-->   DISCONNECTED
 */
@Singleton
class SyncStateMachine @Inject constructor() {

    private val _state = MutableStateFlow(SessionSyncState.IDLE)
    val state: StateFlow<SessionSyncState> = _state.asStateFlow()

    val currentState: SessionSyncState get() = _state.value

    /** Apply [event] and move to the next state if a valid transition exists. */
    fun transition(event: SyncStateEvent) {
        val current = _state.value
        val next = resolveTransition(current, event) ?: return   // ignore invalid transitions
        if (next != current) _state.value = next
    }

    /** Convenience: reset directly to [SessionSyncState.IDLE]. */
    fun reset() = transition(SyncStateEvent.Reset)

    // ── Transition table ──────────────────────────────────────────────────────

    private fun resolveTransition(
        current: SessionSyncState,
        event: SyncStateEvent
    ): SessionSyncState? = when (current) {
        SessionSyncState.IDLE -> when (event) {
            is SyncStateEvent.Connect -> SessionSyncState.CONNECTING
            else -> null
        }
        SessionSyncState.CONNECTING -> when (event) {
            is SyncStateEvent.Connected  -> SessionSyncState.SYNCING
            is SyncStateEvent.Disconnect -> SessionSyncState.DISCONNECTED
            else -> null
        }
        SessionSyncState.SYNCING -> when (event) {
            is SyncStateEvent.SyncComplete -> SessionSyncState.PAUSED
            is SyncStateEvent.Play         -> SessionSyncState.PLAYING
            is SyncStateEvent.Disconnect   -> SessionSyncState.DISCONNECTED
            else -> null
        }
        SessionSyncState.PLAYING -> when (event) {
            is SyncStateEvent.Pause      -> SessionSyncState.PAUSED
            is SyncStateEvent.Disconnect -> SessionSyncState.DISCONNECTED
            else -> null
        }
        SessionSyncState.PAUSED -> when (event) {
            is SyncStateEvent.Play       -> SessionSyncState.PLAYING
            is SyncStateEvent.Disconnect -> SessionSyncState.DISCONNECTED
            else -> null
        }
        SessionSyncState.DISCONNECTED -> when (event) {
            is SyncStateEvent.Reconnect -> SessionSyncState.RECONNECTING
            is SyncStateEvent.Reset     -> SessionSyncState.IDLE
            else -> null
        }
        SessionSyncState.RECONNECTING -> when (event) {
            is SyncStateEvent.Connected  -> SessionSyncState.SYNCING
            is SyncStateEvent.Disconnect -> SessionSyncState.DISCONNECTED
            else -> null
        }
    }
}
