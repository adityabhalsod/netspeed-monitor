package com.netspeed.monitor;

/**
 * Static utility methods for formatting network speed and byte values.
 * Zero-dependency, pure Java — no libraries required.
 */
public final class SpeedUtils {

    // Private constructor prevents instantiation of utility class
    private SpeedUtils() {}

    // Byte size thresholds for unit conversion
    private static final double GB = 1_073_741_824.0;
    private static final double MB = 1_048_576.0;
    private static final double KB = 1024.0;

    /**
     * Formats bytes-per-second into a human-readable speed string (e.g., "1.2 MB/s").
     */
    public static String formatSpeed(double bytesPerSecond) {
        if (bytesPerSecond >= GB) {
            // Gigabytes per second for very high speeds
            return String.format("%.1f GB/s", bytesPerSecond / GB);
        } else if (bytesPerSecond >= MB) {
            // Megabytes per second for broadband speeds
            return String.format("%.1f MB/s", bytesPerSecond / MB);
        } else if (bytesPerSecond >= KB) {
            // Kilobytes per second for moderate speeds
            return String.format("%.1f KB/s", bytesPerSecond / KB);
        } else {
            // Raw bytes per second for very low speeds
            return String.format("%.0f B/s", bytesPerSecond);
        }
    }

    /**
     * Formats a byte count into human-readable format (e.g., "12.50 MB").
     */
    public static String formatBytes(long bytes) {
        if (bytes >= (long) GB) {
            return String.format("%.2f GB", bytes / GB);
        } else if (bytes >= (long) MB) {
            return String.format("%.2f MB", bytes / MB);
        } else if (bytes >= (long) KB) {
            return String.format("%.2f KB", bytes / KB);
        } else {
            return bytes + " B";
        }
    }

    /**
     * Returns compact speed value and unit as a 2-element String array.
     * Index 0 = numeric value (e.g., "1.2"), index 1 = unit (e.g., "MB/s").
     * Used for notification icons and gauge display.
     */
    public static String[] formatSpeedCompact(double bytesPerSecond) {
        if (bytesPerSecond >= GB) {
            // 1 GB+ — show whole number
            return new String[]{String.format("%.0f", bytesPerSecond / GB), "GB/s"};
        } else if (bytesPerSecond >= 100 * MB) {
            // 100 MB+ — show whole number
            return new String[]{String.format("%.0f", bytesPerSecond / MB), "MB/s"};
        } else if (bytesPerSecond >= MB) {
            // 1-99 MB — show one decimal
            return new String[]{String.format("%.1f", bytesPerSecond / MB), "MB/s"};
        } else if (bytesPerSecond >= 100 * KB) {
            // 100-1023 KB — show whole number
            return new String[]{String.format("%.0f", bytesPerSecond / KB), "KB/s"};
        } else if (bytesPerSecond >= KB) {
            // 1-99 KB — show one decimal
            return new String[]{String.format("%.1f", bytesPerSecond / KB), "KB/s"};
        } else if (bytesPerSecond > 0) {
            // Sub-kilobyte — show whole bytes
            return new String[]{String.format("%.0f", bytesPerSecond), "B/s"};
        } else {
            // Zero speed
            return new String[]{"0", "B/s"};
        }
    }

    /**
     * Extracts the numeric speed value in the most appropriate unit for gauge display.
     */
    public static double speedValue(double bytesPerSecond) {
        if (bytesPerSecond >= MB) {
            return bytesPerSecond / MB;
        } else if (bytesPerSecond >= KB) {
            return bytesPerSecond / KB;
        } else {
            return bytesPerSecond;
        }
    }

    /**
     * Returns the unit label for a given speed value.
     */
    public static String speedUnit(double bytesPerSecond) {
        if (bytesPerSecond >= MB) {
            return "MB/s";
        } else if (bytesPerSecond >= KB) {
            return "KB/s";
        } else {
            return "B/s";
        }
    }
}
