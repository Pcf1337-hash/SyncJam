package com.syncjam.app.sync

class NtpClockSync {
    private var clockOffset: Long = 0L

    fun getServerTime(): Long = System.currentTimeMillis() + clockOffset

    fun applyOffset(offset: Long) {
        clockOffset = offset
    }

    fun calculateOffset(t1: Long, t2: Long, t3: Long, t4: Long): Long {
        return ((t2 - t1) + (t3 - t4)) / 2
    }
}
