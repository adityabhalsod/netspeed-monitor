package com.netspeed.monitor.domain.repository

import com.netspeed.monitor.domain.model.NetworkSpeed
import kotlinx.coroutines.flow.Flow

// Repository interface defining the contract for network speed data access
// Follows Interface Segregation: only exposes what consumers need
interface SpeedRepository {

    // Emits real-time network speed measurements as a continuous Flow
    // The flow produces new NetworkSpeed values at the configured update interval
    fun observeSpeed(): Flow<NetworkSpeed>

    // Returns a single snapshot of the current network speed
    suspend fun getCurrentSpeed(): NetworkSpeed

    // Resets the underlying speed calculator, clearing accumulated state
    fun resetCalculator()
}
