package com.netspeed.monitor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netspeed.monitor.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI state data class for the Settings screen
data class SettingsUiState(
    // Whether to start monitoring automatically on device boot
    val startOnBoot: Boolean = false,
    // Whether to show download speed in notification
    val showDownloadSpeed: Boolean = true,
    // Whether to show upload speed in notification
    val showUploadSpeed: Boolean = true,
    // Speed update interval in milliseconds (500ms, 1000ms, 2000ms)
    val updateIntervalMs: Long = 1000L
)

// ViewModel for the Settings screen; handles reading and writing user preferences
@HiltViewModel
class SettingsViewModel @Inject constructor(
    // Injected repository for persistent key-value preferences
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // Backing mutable state flow for Settings screen state
    private val _uiState = MutableStateFlow(SettingsUiState())
    // Publicly exposed state for Compose screens to observe
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Load all persisted settings when ViewModel is created
        loadSettings()
    }

    // Reads all settings from the repository and updates UI state
    private fun loadSettings() {
        viewModelScope.launch {
            // Collect start-on-boot preference
            preferencesRepository.observeStartOnBoot().collect { value ->
                _uiState.value = _uiState.value.copy(startOnBoot = value)
            }
        }
        viewModelScope.launch {
            // Collect show-download-speed preference
            preferencesRepository.observeShowDownloadSpeed().collect { value ->
                _uiState.value = _uiState.value.copy(showDownloadSpeed = value)
            }
        }
        viewModelScope.launch {
            // Collect show-upload-speed preference
            preferencesRepository.observeShowUploadSpeed().collect { value ->
                _uiState.value = _uiState.value.copy(showUploadSpeed = value)
            }
        }
        viewModelScope.launch {
            // Collect update interval preference
            preferencesRepository.observeUpdateInterval().collect { value ->
                _uiState.value = _uiState.value.copy(updateIntervalMs = value)
            }
        }
    }

    // Persists the start-on-boot preference and optimistically updates UI state
    fun setStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setStartOnBoot(enabled)
        }
    }

    // Persists whether the notification shows download speed
    fun setShowDownloadSpeed(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowDownloadSpeed(enabled)
        }
    }

    // Persists whether the notification shows upload speed
    fun setShowUploadSpeed(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowUploadSpeed(enabled)
        }
    }

    // Persists the selected update interval
    fun setUpdateInterval(intervalMs: Long) {
        viewModelScope.launch {
            preferencesRepository.setUpdateInterval(intervalMs)
        }
    }
}
