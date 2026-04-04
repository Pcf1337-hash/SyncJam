package com.syncjam.app.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SyncState(
    val isConnected: Boolean = false,
    val sessionId: String? = null,
    val clockOffset: Long = 0L
)

@Singleton
class SyncEngine @Inject constructor(
    private val ntpClockSync: NtpClockSync
) {
    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    fun getServerTime(): Long = ntpClockSync.getServerTime()
}
