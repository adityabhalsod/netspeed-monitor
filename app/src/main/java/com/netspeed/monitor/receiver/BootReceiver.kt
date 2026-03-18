package com.netspeed.monitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.netspeed.monitor.service.SpeedMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Extension to get the settings DataStore (same name as PreferencesRepositoryImpl)
private val Context.dataStore by preferencesDataStore(name = "settings")

// BroadcastReceiver that listens for device boot events and restarts the monitoring service
class BootReceiver : BroadcastReceiver() {

    // Called when BOOT_COMPLETED or QUICKBOOT_POWERON intent is received
    override fun onReceive(context: Context, intent: Intent) {
        // Verify this is a boot-related broadcast before doing anything
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        // Use goAsync() to extend the time window for async DataStore read
        val pendingResult = goAsync()

        // Launch a coroutine on IO dispatcher to check the user's start-on-boot preference
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Read the start-on-boot preference from DataStore
                val prefs = context.dataStore.data.first()
                val startOnBoot = prefs[booleanPreferencesKey("start_on_boot")] ?: false

                // Only start the service if the user has enabled start-on-boot
                if (startOnBoot) {
                    // Build the intent to start the foreground monitoring service
                    val serviceIntent = Intent(context, SpeedMonitorService::class.java).apply {
                        action = SpeedMonitorService.ACTION_START
                    }
                    // Use startForegroundService for Android 8.0+ to satisfy foreground requirements
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            } finally {
                // Signal Android that the BroadcastReceiver has finished async processing
                pendingResult.finish()
            }
        }
    }
}
