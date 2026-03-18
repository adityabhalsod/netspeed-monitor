package com.netspeed.monitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.netspeed.monitor.MainActivity
import com.netspeed.monitor.R
import com.netspeed.monitor.domain.model.NetworkSpeed
import com.netspeed.monitor.domain.usecase.ObserveNetworkSpeedUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Foreground service that continuously monitors network speed and displays it in a sticky notification
@AndroidEntryPoint
class SpeedMonitorService : Service() {

    // Injected use case for observing network speed from the domain layer
    @Inject
    lateinit var observeNetworkSpeedUseCase: ObserveNetworkSpeedUseCase

    // Supervised coroutine scope tied to the service lifecycle; SupervisorJob prevents child failure from cancelling siblings
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Reference to system notification manager for building and updating notifications
    private lateinit var notificationManager: NotificationManager

    companion object {
        // Unique notification channel ID for network speed updates
        const val CHANNEL_ID = "speed_monitor_channel"
        // Unique ID for the persistent foreground notification
        const val NOTIFICATION_ID = 1001
        // Action string for the start intent to begin monitoring
        const val ACTION_START = "com.netspeed.monitor.action.START"
        // Action string for the stop intent to stop monitoring
        const val ACTION_STOP = "com.netspeed.monitor.action.STOP"
    }

    // Called when the service is first created; set up notification channel and manager
    override fun onCreate() {
        super.onCreate()
        // Obtain the system notification manager service
        notificationManager = getSystemService(NotificationManager::class.java)
        // Create the notification channel required for Android 8.0+ (API 26)
        createNotificationChannel()
    }

    // Called when startService/startForegroundService is invoked with an Intent
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            // Start monitoring: promote to foreground and begin speed observation
            ACTION_START -> startMonitoring()
            // Stop monitoring: clean up and stop the service
            ACTION_STOP -> stopMonitoring()
        }
        // START_STICKY ensures the system restarts the service if it's killed
        return START_STICKY
    }

    // Services do not support binding in this app; return null
    override fun onBind(intent: Intent?): IBinder? = null

    // Promotes the service to foreground with an initial notification and begins speed collection
    private fun startMonitoring() {
        // Build an initial notification to satisfy the foreground service requirement
        val notification = buildNotification(
            downloadSpeed = "0 B/s",
            uploadSpeed = "0 B/s"
        )

        // Start as a foreground service using ServiceCompat for API compatibility
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            // Specify DATA_SYNC type for network monitoring (required on API 34+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else 0
        )

        // Launch a coroutine to observe speed updates and refresh the notification
        serviceScope.launch {
            // Collect the continuous stream of NetworkSpeed values from the use case
            observeNetworkSpeedUseCase().collect { speed ->
                // Update the notification with the latest speed values
                updateNotification(speed)
            }
        }
    }

    // Cancels the coroutine scope, stops foreground promotion, and self-terminates
    private fun stopMonitoring() {
        // Cancel all running coroutines in the service scope
        serviceScope.cancel()
        // Remove the foreground notification and demote to background
        stopForeground(STOP_FOREGROUND_REMOVE)
        // Schedule service for termination
        stopSelf()
    }

    // Updates the persistent notification with the latest speed data
    private fun updateNotification(speed: NetworkSpeed) {
        // Format speed values to human-readable strings using domain model helpers
        val downloadText = NetworkSpeed.formatSpeed(speed.downloadSpeed)
        val uploadText = NetworkSpeed.formatSpeed(speed.uploadSpeed)

        // Build a new notification with updated speed values
        val notification = buildNotification(
            downloadSpeed = downloadText,
            uploadSpeed = uploadText
        )

        // Push the updated notification to the system; replaces the existing one by same ID
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // Builds a Notification with download and upload speed prominently displayed
    private fun buildNotification(downloadSpeed: String, uploadSpeed: String): Notification {
        // PendingIntent to open MainActivity when the notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop action intent for the notification action button
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, SpeedMonitorService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification with live speed as the primary content
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_speed_notification)
            // Speed values as the main title for maximum visibility in collapsed view
            .setContentTitle("↓ $downloadSpeed   ↑ $uploadSpeed")
            .setContentText("Tap to open Net Speed Monitor")
            // Subtext appears in the notification header area
            .setSubText("Net Speed")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            // Prevent re-alerting on each update
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // Categorize as a running service notification
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            // Show notification immediately on Android 12+ (no delay)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.notification_stop),
                stopPendingIntent
            )
            // Expanded view shows detailed speed breakdown
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("↓ $downloadSpeed   ↑ $uploadSpeed")
                    .bigText("Download: $downloadSpeed\nUpload: $uploadSpeed")
            )
            .build()
    }

    // Creates the notification channel required on Android 8.0 (API 26) and higher
    private fun createNotificationChannel() {
        // Channel with LOW importance: shows in shade but never makes sound
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Network Speed Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            // Human-readable description shown in system notification settings
            description = "Live network upload and download speed monitoring"
            // Disable vibration for frequent update notifications
            enableVibration(false)
            // Disable sound for silent status updates
            setSound(null, null)
        }
        // Register the channel with the system notification manager
        notificationManager.createNotificationChannel(channel)
    }

    // Called when the service is destroyed; cancel coroutines to prevent leaks
    override fun onDestroy() {
        super.onDestroy()
        // Clean up the coroutine scope to stop all ongoing work
        serviceScope.cancel()
    }
}
