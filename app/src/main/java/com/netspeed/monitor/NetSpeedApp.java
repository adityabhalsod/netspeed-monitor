package com.netspeed.monitor;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

/**
 * Application class: creates the notification channel on app start
 * and provides a shared TrafficStatsCalculator singleton.
 */
public class NetSpeedApp extends Application {

    // Notification channel ID shared between service and receiver
    public static final String CHANNEL_ID = "speed_monitor_channel";

    // Shared calculator instance used by both the service and the activity
    private static TrafficStatsCalculator calculator;

    @Override
    public void onCreate() {
        super.onCreate();
        // Create the notification channel at app launch (required for API 26+)
        createNotificationChannel();
    }

    /**
     * Returns the shared calculator, creating it lazily on first access.
     */
    public static synchronized TrafficStatsCalculator getCalculator() {
        if (calculator == null) {
            calculator = new TrafficStatsCalculator();
        }
        return calculator;
    }

    /**
     * Resets and recreates the calculator for a fresh monitoring session.
     */
    public static synchronized void resetCalculator() {
        if (calculator != null) {
            calculator.reset();
        }
        calculator = new TrafficStatsCalculator();
    }

    /**
     * Creates a high-priority notification channel so speed notifications stay on top.
     * Sound and vibration are disabled — only visual priority is elevated.
     */
    private void createNotificationChannel() {
        // DEFAULT importance: keeps notification visible and prominent without heads-up popup
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Network Speed Monitor",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Live network upload and download speed monitoring");
        channel.enableVibration(false);
        channel.setSound(null, null);
        // Disable heads-up pop-up by locking screen visibility
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        // Prevent the notification badge ("1") from appearing on the app icon
        channel.setShowBadge(false);

        // Register the channel with the system (delete first to allow importance upgrades)
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.deleteNotificationChannel(CHANNEL_ID);
            nm.createNotificationChannel(channel);
        }
    }
}
