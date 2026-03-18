package com.netspeed.monitor.domain.usecase

import com.netspeed.monitor.domain.model.NetworkSpeed
import com.netspeed.monitor.domain.repository.SpeedRepository
import javax.inject.Inject

// Use case for retrieving a single snapshot of current network speed
// Used by the service for notification updates where only the latest value is needed
class GetCurrentSpeedUseCase @Inject constructor(
    // Injected repository providing network speed data
    private val speedRepository: SpeedRepository
) {
    // Invoked as a suspend function: returns the latest network speed snapshot
    suspend operator fun invoke(): NetworkSpeed {
        return speedRepository.getCurrentSpeed()
    }
}
