package com.netspeed.monitor.domain.model

// Sealed interface representing possible states of the speed monitoring service
sealed interface ServiceState {
    // Service is currently running and actively monitoring network speed
    data object Running : ServiceState
    // Service is stopped and not monitoring
    data object Stopped : ServiceState
}
