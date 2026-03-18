package com.netspeed.monitor.data.speed

import android.net.TrafficStats
import javax.inject.Inject
import javax.inject.Singleton

// Speed calculator using Android's TrafficStats API
// Works on all Android versions including 10+ where /proc/net/dev is restricted
@Singleton
class TrafficStatsCalculator @Inject constructor() {

    // Previous snapshot for delta-based speed calculation
    private var previousRxBytes: Long = 0L
    private var previousTxBytes: Long = 0L
    private var previousTimestampNs: Long = 0L
    private var hasPrevious: Boolean = false

    // Session tracking: bytes at monitoring start for session-specific usage
    private var sessionStartRxBytes: Long = 0L
    private var sessionStartTxBytes: Long = 0L
    private var sessionStarted: Boolean = false

    // Calculates current download and upload speed in bytes per second
    // Returns Pair(downloadBytesPerSec, uploadBytesPerSec)
    @Synchronized
    fun calculateSpeed(): Pair<Double, Double> {
        // Read current cumulative byte counts from TrafficStats
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTxBytes = TrafficStats.getTotalTxBytes()
        val currentTimeNs = System.nanoTime()

        // Return zero if TrafficStats is unavailable on this device
        if (currentRxBytes == TrafficStats.UNSUPPORTED.toLong() ||
            currentTxBytes == TrafficStats.UNSUPPORTED.toLong()
        ) {
            return Pair(0.0, 0.0)
        }

        // Initialize session tracking on first measurement
        if (!sessionStarted) {
            sessionStartRxBytes = currentRxBytes
            sessionStartTxBytes = currentTxBytes
            sessionStarted = true
        }

        // First call stores baseline; returns zero speed since no delta is available
        if (!hasPrevious) {
            previousRxBytes = currentRxBytes
            previousTxBytes = currentTxBytes
            previousTimestampNs = currentTimeNs
            hasPrevious = true
            return Pair(0.0, 0.0)
        }

        // Calculate elapsed time in seconds from nanosecond delta
        val elapsedSec = (currentTimeNs - previousTimestampNs) / 1_000_000_000.0

        // Guard against division by zero or negligible time intervals
        if (elapsedSec <= 0.001) {
            return Pair(0.0, 0.0)
        }

        // Compute byte deltas; guard against counter resets producing negative values
        val rxDelta = (currentRxBytes - previousRxBytes).coerceAtLeast(0L)
        val txDelta = (currentTxBytes - previousTxBytes).coerceAtLeast(0L)

        // Speed in bytes per second
        val downloadSpeed = rxDelta / elapsedSec
        val uploadSpeed = txDelta / elapsedSec

        // Update stored snapshot for next measurement cycle
        previousRxBytes = currentRxBytes
        previousTxBytes = currentTxBytes
        previousTimestampNs = currentTimeNs

        return Pair(downloadSpeed, uploadSpeed)
    }

    // Returns session-specific byte totals (bytes since monitoring started)
    @Synchronized
    fun getSessionBytes(): Pair<Long, Long> {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()

        // Return zero if stats unavailable on this device
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) {
            return Pair(0L, 0L)
        }

        // Initialize session baseline on first call
        if (!sessionStarted) {
            sessionStartRxBytes = rx
            sessionStartTxBytes = tx
            sessionStarted = true
        }

        return Pair(
            (rx - sessionStartRxBytes).coerceAtLeast(0L),
            (tx - sessionStartTxBytes).coerceAtLeast(0L)
        )
    }

    // Returns total bytes received and transmitted since device boot
    fun getTotalBytes(): Pair<Long, Long> {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        return Pair(
            if (rx == TrafficStats.UNSUPPORTED.toLong()) 0L else rx,
            if (tx == TrafficStats.UNSUPPORTED.toLong()) 0L else tx
        )
    }

    // Resets all calculator state including session tracking for fresh measurements
    @Synchronized
    fun reset() {
        hasPrevious = false
        previousRxBytes = 0L
        previousTxBytes = 0L
        previousTimestampNs = 0L
        sessionStarted = false
        sessionStartRxBytes = 0L
        sessionStartTxBytes = 0L
    }
}
