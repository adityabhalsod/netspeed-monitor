package com.netspeed.monitor.domain.model

// Data class representing a snapshot of current network speed measurements
data class NetworkSpeed(
    // Current download speed in bytes per second
    val downloadSpeed: Double = 0.0,
    // Current upload speed in bytes per second
    val uploadSpeed: Double = 0.0,
    // Total bytes received since device boot
    val totalRxBytes: Long = 0L,
    // Total bytes transmitted since device boot
    val totalTxBytes: Long = 0L,
    // Timestamp in milliseconds when this measurement was taken
    val timestamp: Long = System.currentTimeMillis()
) {
    // Formats a byte-per-second speed value into a human-readable string with appropriate unit
    companion object {
        fun formatSpeed(bytesPerSecond: Double): String {
            return when {
                // Display in GB/s for speeds >= 1 gigabyte per second
                bytesPerSecond >= 1_073_741_824 -> String.format("%.1f GB/s", bytesPerSecond / 1_073_741_824)
                // Display in MB/s for speeds >= 1 megabyte per second
                bytesPerSecond >= 1_048_576 -> String.format("%.1f MB/s", bytesPerSecond / 1_048_576)
                // Display in KB/s for speeds >= 1 kilobyte per second
                bytesPerSecond >= 1024 -> String.format("%.1f KB/s", bytesPerSecond / 1024)
                // Display in B/s for very low speeds
                else -> String.format("%.0f B/s", bytesPerSecond)
            }
        }

        // Formats a byte count into human-readable format (B, KB, MB, GB)
        fun formatBytes(bytes: Long): String {
            return when {
                // Display in GB for byte counts >= 1 gigabyte
                bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
                // Display in MB for byte counts >= 1 megabyte
                bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
                // Display in KB for byte counts >= 1 kilobyte
                bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
                // Display raw byte count for small values
                else -> "$bytes B"
            }
        }

        // Extracts just the numeric portion from a speed value for gauge display
        fun speedValue(bytesPerSecond: Double): Double {
            return when {
                bytesPerSecond >= 1_048_576 -> bytesPerSecond / 1_048_576 // Convert to MB/s
                bytesPerSecond >= 1024 -> bytesPerSecond / 1024           // Convert to KB/s
                else -> bytesPerSecond                                     // Keep as B/s
            }
        }

        // Returns the unit label string for a given speed value
        fun speedUnit(bytesPerSecond: Double): String {
            return when {
                bytesPerSecond >= 1_048_576 -> "MB/s" // Megabytes per second
                bytesPerSecond >= 1024 -> "KB/s"      // Kilobytes per second
                else -> "B/s"                          // Bytes per second
            }
        }
    }
}
