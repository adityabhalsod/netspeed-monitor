package com.netspeed.monitor;

import android.net.TrafficStats;

/**
 * Calculates network speed from Android's TrafficStats kernel counters.
 * Thread-safe via synchronized methods. No external dependencies.
 */
public class TrafficStatsCalculator {

    // Previous snapshot values for computing speed deltas
    private long previousRxBytes;
    private long previousTxBytes;
    private long previousTimestampNs;
    private boolean hasPrevious;

    // Session-start byte counts for tracking usage since monitoring began
    private long sessionStartRxBytes;
    private long sessionStartTxBytes;
    private boolean sessionStarted;

    /**
     * Calculates current download and upload speed in bytes per second.
     * Returns double[2]: index 0 = download, index 1 = upload.
     */
    public synchronized double[] calculateSpeed() {
        // Read current cumulative byte counts from kernel counters
        long currentRxBytes = TrafficStats.getTotalRxBytes();
        long currentTxBytes = TrafficStats.getTotalTxBytes();
        long currentTimeNs = System.nanoTime();

        // Return zero if TrafficStats is unavailable on this device
        if (currentRxBytes == TrafficStats.UNSUPPORTED ||
                currentTxBytes == TrafficStats.UNSUPPORTED) {
            return new double[]{0.0, 0.0};
        }

        // Initialize session tracking on first measurement
        if (!sessionStarted) {
            sessionStartRxBytes = currentRxBytes;
            sessionStartTxBytes = currentTxBytes;
            sessionStarted = true;
        }

        // First call stores baseline; no delta available yet
        if (!hasPrevious) {
            previousRxBytes = currentRxBytes;
            previousTxBytes = currentTxBytes;
            previousTimestampNs = currentTimeNs;
            hasPrevious = true;
            return new double[]{0.0, 0.0};
        }

        // Calculate elapsed time in seconds from nanosecond delta
        double elapsedSec = (currentTimeNs - previousTimestampNs) / 1_000_000_000.0;

        // Guard against division by zero or negligible time intervals
        if (elapsedSec <= 0.001) {
            return new double[]{0.0, 0.0};
        }

        // Compute byte deltas; clamp to zero to handle counter resets
        long rxDelta = Math.max(currentRxBytes - previousRxBytes, 0L);
        long txDelta = Math.max(currentTxBytes - previousTxBytes, 0L);

        // Speed in bytes per second
        double downloadSpeed = rxDelta / elapsedSec;
        double uploadSpeed = txDelta / elapsedSec;

        // Update snapshot for next measurement cycle
        previousRxBytes = currentRxBytes;
        previousTxBytes = currentTxBytes;
        previousTimestampNs = currentTimeNs;

        return new double[]{downloadSpeed, uploadSpeed};
    }

    /**
     * Returns session-specific byte totals since monitoring started.
     * Returns long[2]: index 0 = received bytes, index 1 = transmitted bytes.
     */
    public synchronized long[] getSessionBytes() {
        long rx = TrafficStats.getTotalRxBytes();
        long tx = TrafficStats.getTotalTxBytes();

        // Return zero if stats unavailable on this device
        if (rx == TrafficStats.UNSUPPORTED || tx == TrafficStats.UNSUPPORTED) {
            return new long[]{0L, 0L};
        }

        // Initialize session baseline on first call
        if (!sessionStarted) {
            sessionStartRxBytes = rx;
            sessionStartTxBytes = tx;
            sessionStarted = true;
        }

        return new long[]{
                Math.max(rx - sessionStartRxBytes, 0L),
                Math.max(tx - sessionStartTxBytes, 0L)
        };
    }

    /**
     * Resets all calculator state for a fresh monitoring session.
     */
    public synchronized void reset() {
        hasPrevious = false;
        previousRxBytes = 0L;
        previousTxBytes = 0L;
        previousTimestampNs = 0L;
        sessionStarted = false;
        sessionStartRxBytes = 0L;
        sessionStartTxBytes = 0L;
    }
}
