package com.netspeed.monitor.domain.repository

import kotlinx.coroutines.flow.Flow

// Repository interface for managing user preferences (settings)
interface PreferencesRepository {

    // Observes whether the monitoring service should auto-start on boot
    fun observeStartOnBoot(): Flow<Boolean>

    // Updates the start-on-boot preference
    suspend fun setStartOnBoot(enabled: Boolean)

    // Observes whether the notification should show download speed
    fun observeShowDownloadSpeed(): Flow<Boolean>

    // Updates the show-download-speed preference
    suspend fun setShowDownloadSpeed(enabled: Boolean)

    // Observes whether the notification should show upload speed
    fun observeShowUploadSpeed(): Flow<Boolean>

    // Updates the show-upload-speed preference
    suspend fun setShowUploadSpeed(enabled: Boolean)

    // Observes the update interval in milliseconds
    fun observeUpdateInterval(): Flow<Long>

    // Updates the speed monitoring interval in milliseconds
    suspend fun setUpdateInterval(intervalMs: Long)

    // Observes whether the monitoring service is currently enabled
    fun observeServiceEnabled(): Flow<Boolean>

    // Updates whether the monitoring service should be running
    suspend fun setServiceEnabled(enabled: Boolean)
}
