package com.netspeed.monitor.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netspeed.monitor.domain.model.NetworkSpeed
import com.netspeed.monitor.domain.model.ServiceState
import com.netspeed.monitor.domain.repository.PreferencesRepository
import com.netspeed.monitor.domain.usecase.ObserveNetworkSpeedUseCase
import com.netspeed.monitor.service.SpeedMonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI state data class for the Home screen, representing all observable values
data class HomeUiState(
    // Latest network speed measurement
    val networkSpeed: NetworkSpeed = NetworkSpeed(),
    // Whether the monitoring service is currently active
    val isMonitoring: Boolean = false,
    // Whether notification permission has been granted (required on Android 13+)
    val hasNotificationPermission: Boolean = false
)

// ViewModel for the Home screen; survives configuration changes and manages UI state
@HiltViewModel
class HomeViewModel @Inject constructor(
    // Use case for streaming network speed data
    private val observeNetworkSpeedUseCase: ObserveNetworkSpeedUseCase,
    // Repository for reading/writing user preferences
    private val preferencesRepository: PreferencesRepository,
    // Application context for starting/stopping the service (no UI context reference)
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Backing mutable state flow for the full Home UI state
    private val _uiState = MutableStateFlow(HomeUiState())
    // Publicly exposed immutable state flow for UI to observe
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Observe the service-enabled preference as a StateFlow for toggle state
    val serviceEnabled: StateFlow<Boolean> = preferencesRepository
        .observeServiceEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    // Job reference for speed collection coroutine, cancelled when monitoring stops
    private var speedCollectionJob: Job? = null

    init {
        // Synchronize UI monitoring state and speed collection with service preference
        syncMonitoringState()
    }

    // Observes service-enabled preference and starts/stops speed collection accordingly
    private fun syncMonitoringState() {
        viewModelScope.launch {
            preferencesRepository.observeServiceEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(isMonitoring = enabled)
                if (enabled) {
                    startSpeedCollection()
                } else {
                    stopSpeedCollection()
                }
            }
        }
    }

    // Starts collecting speed data from the shared flow when monitoring is active
    private fun startSpeedCollection() {
        speedCollectionJob?.cancel()
        speedCollectionJob = viewModelScope.launch {
            observeNetworkSpeedUseCase().collect { speed ->
                _uiState.value = _uiState.value.copy(networkSpeed = speed)
            }
        }
    }

    // Stops speed collection and resets displayed speed to zero
    private fun stopSpeedCollection() {
        speedCollectionJob?.cancel()
        speedCollectionJob = null
        _uiState.value = _uiState.value.copy(networkSpeed = NetworkSpeed())
    }

    // Called when the user taps the toggle button to start/stop monitoring
    fun toggleMonitoring() {
        viewModelScope.launch {
            // Read current monitoring state and invert it
            val currentlyMonitoring = _uiState.value.isMonitoring
            val newState = !currentlyMonitoring

            // Persist the new monitoring preference
            preferencesRepository.setServiceEnabled(newState)

            // Build the service intent with start or stop action
            val serviceIntent = Intent(context, SpeedMonitorService::class.java).apply {
                action = if (newState) SpeedMonitorService.ACTION_START else SpeedMonitorService.ACTION_STOP
            }

            if (newState) {
                // Start the foreground service using the appropriate API level method
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                // Stop the service
                context.startService(serviceIntent)
            }
        }
    }

    // Updates whether the notification permission is granted (used for permission UI)
    fun updateNotificationPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasNotificationPermission = granted)
    }
}
