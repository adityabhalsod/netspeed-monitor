package com.netspeed.monitor.data.repository

import com.netspeed.monitor.data.speed.TrafficStatsCalculator
import com.netspeed.monitor.domain.model.NetworkSpeed
import com.netspeed.monitor.domain.repository.PreferencesRepository
import com.netspeed.monitor.domain.repository.SpeedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// SpeedRepository using Android TrafficStats API for reliable speed measurement on all devices
// Uses SharedFlow so multiple consumers (UI + Service) share a single data stream
@Singleton
class SpeedRepositoryImpl @Inject constructor(
    // TrafficStats-based calculator that works on Android 10+ where /proc/net/dev is restricted
    private val trafficStatsCalculator: TrafficStatsCalculator,
    // Preferences for reading the user's update interval setting
    private val preferencesRepository: PreferencesRepository
) : SpeedRepository {

    companion object {
        const val DEFAULT_INTERVAL_MS = 1000L
    }

    // Update interval updated reactively from user preferences
    @Volatile
    private var updateIntervalMs: Long = DEFAULT_INTERVAL_MS

    // Long-lived scope for the shared flow and preference observation
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Reactively observe interval preference changes and apply them
        repositoryScope.launch {
            preferencesRepository.observeUpdateInterval().collect { interval ->
                updateIntervalMs = interval
            }
        }
    }

    // Single upstream flow shared among all collectors via shareIn
    // Starts when first subscriber connects, stops 5s after last unsubscribes
    private val sharedSpeedFlow: Flow<NetworkSpeed> = flow {
        // Reset calculator to begin fresh session tracking
        trafficStatsCalculator.reset()
        while (true) {
            // Calculate current speed using TrafficStats API
            val (downloadSpeed, uploadSpeed) = trafficStatsCalculator.calculateSpeed()
            // Get session-specific byte totals (since monitoring started)
            val (sessionRx, sessionTx) = trafficStatsCalculator.getSessionBytes()
            emit(
                NetworkSpeed(
                    downloadSpeed = downloadSpeed,
                    uploadSpeed = uploadSpeed,
                    totalRxBytes = sessionRx,
                    totalTxBytes = sessionTx
                )
            )
            // Wait for the user-configured interval before next measurement
            delay(updateIntervalMs)
        }
    }.shareIn(
        scope = repositoryScope,
        started = SharingStarted.WhileSubscribed(5_000),
        replay = 1
    )

    // All collectors receive the same speed emissions from the shared flow
    override fun observeSpeed(): Flow<NetworkSpeed> = sharedSpeedFlow

    // Returns a single snapshot of current speed for one-off queries
    override suspend fun getCurrentSpeed(): NetworkSpeed {
        val (downloadSpeed, uploadSpeed) = trafficStatsCalculator.calculateSpeed()
        val (sessionRx, sessionTx) = trafficStatsCalculator.getSessionBytes()
        return NetworkSpeed(
            downloadSpeed = downloadSpeed,
            uploadSpeed = uploadSpeed,
            totalRxBytes = sessionRx,
            totalTxBytes = sessionTx
        )
    }

    // Resets the calculator, clearing speed deltas and session byte tracking
    override fun resetCalculator() {
        trafficStatsCalculator.reset()
    }
}
