package com.netspeed.monitor.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.netspeed.monitor.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property to create a DataStore instance scoped to the Application context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Implementation of PreferencesRepository using Jetpack DataStore for persistent key-value storage
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    // Application context for accessing DataStore (not Activity context to avoid leaks)
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    // Preference keys defined as constants for type-safe access
    companion object {
        // Key for start-on-boot boolean preference
        val KEY_START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        // Key for show-download-speed boolean preference
        val KEY_SHOW_DOWNLOAD = booleanPreferencesKey("show_download_speed")
        // Key for show-upload-speed boolean preference
        val KEY_SHOW_UPLOAD = booleanPreferencesKey("show_upload_speed")
        // Key for update interval in milliseconds
        val KEY_UPDATE_INTERVAL = longPreferencesKey("update_interval_ms")
        // Key for service enabled/disabled state
        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
    }

    // Observes start-on-boot preference, defaulting to false if not set
    override fun observeStartOnBoot(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_START_ON_BOOT] ?: false
        }
    }

    // Persists the start-on-boot preference
    override suspend fun setStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_START_ON_BOOT] = enabled
        }
    }

    // Observes show-download-speed preference, defaulting to true
    override fun observeShowDownloadSpeed(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_SHOW_DOWNLOAD] ?: true
        }
    }

    // Persists the show-download-speed preference
    override suspend fun setShowDownloadSpeed(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_DOWNLOAD] = enabled
        }
    }

    // Observes show-upload-speed preference, defaulting to true
    override fun observeShowUploadSpeed(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_SHOW_UPLOAD] ?: true
        }
    }

    // Persists the show-upload-speed preference
    override suspend fun setShowUploadSpeed(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_UPLOAD] = enabled
        }
    }

    // Observes update interval preference, defaulting to 1000ms (1 second)
    override fun observeUpdateInterval(): Flow<Long> {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_UPDATE_INTERVAL] ?: 1000L
        }
    }

    // Persists the update interval preference
    override suspend fun setUpdateInterval(intervalMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_UPDATE_INTERVAL] = intervalMs
        }
    }

    // Observes service enabled state, defaulting to false
    override fun observeServiceEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_SERVICE_ENABLED] ?: false
        }
    }

    // Persists the service enabled state
    override suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVICE_ENABLED] = enabled
        }
    }
}
