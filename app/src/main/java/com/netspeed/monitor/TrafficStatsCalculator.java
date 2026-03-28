package com.netspeed.monitor;

import android.net.TrafficStats;

/**
 * Calculates network speed from Android's TrafficStats kernel counters.
 * Uses a sliding-window approach: accumulates byte deltas over ~1 second
 * to produce stable speed readings even when polled every 100ms.
 * Thread-safe via synchronized methods. No external dependencies.
 */
public class TrafficStatsCalculator {

    // Window-start snapshot for computing speed over the sliding window
    private long windowRxBytes;
    private long windowTxBytes;
    private long windowTimestampNs;
    private boolean hasWindow;

    // Most recently computed speed values — returned between window resets
    private double lastDownloadSpeed;
    private double lastUploadSpeed;

    // Sliding window duration in nanoseconds (1 second)
    // TrafficStats counters update roughly once per second on most devices,
    // so a 1-second window ensures meaningful byte deltas are captured
    private static final long WINDOW_NS = 1_000_000_000L;

    // Session-start byte counts for tracking total usage since monitoring began
    private long sessionStartRxBytes;
    private long sessionStartTxBytes;
    private boolean sessionStarted;

    /**
     * Calculates current download and upload speed in bytes per second.
     * Uses a 1-second sliding window: accumulates deltas from the window start,
     * computes speed, then resets the window. Between window resets, returns
     * the last computed speed so the UI stays responsive without flicker.
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

        // First call: store baseline window snapshot, no speed available yet
        if (!hasWindow) {
            windowRxBytes = currentRxBytes;
            windowTxBytes = currentTxBytes;
            windowTimestampNs = currentTimeNs;
            hasWindow = true;
            return new double[]{0.0, 0.0};
        }

        // Elapsed nanoseconds since the window started
        long elapsedNs = currentTimeNs - windowTimestampNs;

        // Only recalculate speed once the window duration (~1 second) has elapsed
        // This ensures TrafficStats counters have had time to change meaningfully
        if (elapsedNs >= WINDOW_NS) {
            // Convert elapsed time to seconds for bytes-per-second calculation
            double elapsedSec = elapsedNs / 1_000_000_000.0;

            // Compute byte deltas over the window; clamp to zero for counter resets
            long rxDelta = Math.max(currentRxBytes - windowRxBytes, 0L);
            long txDelta = Math.max(currentTxBytes - windowTxBytes, 0L);

            // Speed in bytes per second over the full window period
            lastDownloadSpeed = rxDelta / elapsedSec;
            lastUploadSpeed = txDelta / elapsedSec;

            // Slide the window forward to the current reading
            windowRxBytes = currentRxBytes;
            windowTxBytes = currentTxBytes;
            windowTimestampNs = currentTimeNs;
        }

        // Return the most recent computed speed (stable between window resets)
        return new double[]{lastDownloadSpeed, lastUploadSpeed};
    }

    /**
     * Returns session-specific byte totals since monitoring started.
     * Returns long[2]: index 0 = received bytes, index 1 = transmitted bytes.
     */
    public synchronized long[] getSessionBytes() {
        // Read current cumulative byte counts
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

        // Return bytes received/transmitted since session start
        return new long[]{
                Math.max(rx - sessionStartRxBytes, 0L),
                Math.max(tx - sessionStartTxBytes, 0L)
        };
    }

    /**
     * Resets all calculator state for a fresh monitoring session.
     */
    public synchronized void reset() {
        // Clear the sliding window so speed starts from zero
        hasWindow = false;
        windowRxBytes = 0L;
        windowTxBytes = 0L;
        windowTimestampNs = 0L;
        // Clear last computed speed values
        lastDownloadSpeed = 0.0;
        lastUploadSpeed = 0.0;
        // Reset session byte tracking
        sessionStarted = false;
        sessionStartRxBytes = 0L;
        sessionStartTxBytes = 0L;
    }
}
