package com.netspeed.monitor.data.repository

import com.netspeed.monitor.data.native_.NativeSpeedBridge
import com.netspeed.monitor.domain.model.NetworkSpeed
import com.netspeed.monitor.domain.repository.SpeedRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

// Implementation of SpeedRepository that uses the native C++ calculator via JNI
@Singleton
class SpeedRepositoryImpl @Inject constructor(
    // Injected JNI bridge to the native C++ speed calculator
    private val nativeSpeedBridge: NativeSpeedBridge
) : SpeedRepository {

    // Default interval between speed measurements in milliseconds
    companion object {
        const val DEFAULT_INTERVAL_MS = 1000L
    }

    // Configurable update interval; can be changed at runtime
    var updateIntervalMs: Long = DEFAULT_INTERVAL_MS

    // Emits continuous NetworkSpeed readings at the configured interval
    override fun observeSpeed(): Flow<NetworkSpeed> = flow {
        // Infinite loop to continuously emit speed updates
        while (true) {
            // Calculate current speed using native C++ library via JNI
            val (downloadSpeed, uploadSpeed) = nativeSpeedBridge.calculateSpeed()
            // Get total byte counts for cumulative usage display
            val (totalRx, totalTx) = nativeSpeedBridge.getTotalBytes()

            // Emit a new NetworkSpeed snapshot with all measurements
            emit(
                NetworkSpeed(
                    downloadSpeed = downloadSpeed,
                    uploadSpeed = uploadSpeed,
                    totalRxBytes = totalRx,
                    totalTxBytes = totalTx
                )
            )

            // Wait for the configured interval before next measurement
            delay(updateIntervalMs)
        }
    }

    // Returns a single speed measurement snapshot
    override suspend fun getCurrentSpeed(): NetworkSpeed {
        // Calculate current speed using native library
        val (downloadSpeed, uploadSpeed) = nativeSpeedBridge.calculateSpeed()
        // Get total byte counts
        val (totalRx, totalTx) = nativeSpeedBridge.getTotalBytes()

        // Return a single NetworkSpeed snapshot
        return NetworkSpeed(
            downloadSpeed = downloadSpeed,
            uploadSpeed = uploadSpeed,
            totalRxBytes = totalRx,
            totalTxBytes = totalTx
        )
    }

    // Resets the native calculator, clearing accumulated delta state
    override fun resetCalculator() {
        nativeSpeedBridge.reset()
    }
}
