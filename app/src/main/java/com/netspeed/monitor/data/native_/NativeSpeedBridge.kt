package com.netspeed.monitor.data.native_

import javax.inject.Inject
import javax.inject.Singleton

// Kotlin bridge class for JNI calls to the native C++ speed calculator library
@Singleton
class NativeSpeedBridge @Inject constructor() {

    companion object {
        // Load the native shared library (libnetspeed.so) on class initialization
        init {
            System.loadLibrary("netspeed")
        }
    }

    // Native method: calculates current download/upload speed in bytes per second
    // Returns a DoubleArray of size 2: [downloadSpeed, uploadSpeed]
    external fun nativeCalculateSpeed(): DoubleArray

    // Native method: reads total received and transmitted bytes from /proc/net/dev
    // Returns a LongArray of size 2: [rxBytes, txBytes]
    external fun nativeGetTotalBytes(): LongArray

    // Native method: resets the C++ calculator state for fresh calculations
    external fun nativeReset()

    // Kotlin-friendly wrapper: calculates speed and returns a Pair of download/upload speeds
    fun calculateSpeed(): Pair<Double, Double> {
        // Call native method and destructure the result array
        val result = nativeCalculateSpeed()
        // Return download speed (index 0) and upload speed (index 1) as a Pair
        return Pair(result[0], result[1])
    }

    // Kotlin-friendly wrapper: reads total bytes and returns a Pair of rx/tx totals
    fun getTotalBytes(): Pair<Long, Long> {
        // Call native method and destructure the result array
        val result = nativeGetTotalBytes()
        // Return received bytes (index 0) and transmitted bytes (index 1) as a Pair
        return Pair(result[0], result[1])
    }

    // Kotlin-friendly wrapper: resets the native calculator
    fun reset() {
        nativeReset()
    }
}
