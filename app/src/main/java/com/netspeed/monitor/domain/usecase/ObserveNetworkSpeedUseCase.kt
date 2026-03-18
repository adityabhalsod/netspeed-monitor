package com.netspeed.monitor.domain.usecase

import com.netspeed.monitor.domain.model.NetworkSpeed
import com.netspeed.monitor.domain.repository.SpeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Use case for observing real-time network speed updates as a continuous flow
// Encapsulates the single responsibility of streaming speed data to consumers
class ObserveNetworkSpeedUseCase @Inject constructor(
    // Injected repository providing network speed data
    private val speedRepository: SpeedRepository
) {
    // Invoked as a function: returns a Flow of NetworkSpeed for reactive observation
    operator fun invoke(): Flow<NetworkSpeed> {
        return speedRepository.observeSpeed()
    }
}
