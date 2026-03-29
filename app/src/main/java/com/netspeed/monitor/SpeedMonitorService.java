package com.netspeed.monitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Foreground service that monitors network speed every 100ms
 * and updates a persistent status bar notification with live speed values.
 * UI callbacks fire at 100ms for responsive gauges; notification updates
 * are throttled to every 500ms to avoid excessive system overhead.
 */
public class SpeedMonitorService extends Service {

    // Intent action constants for starting and stopping the service
    public static final String ACTION_START = "com.netspeed.monitor.action.START";
    public static final String ACTION_STOP = "com.netspeed.monitor.action.STOP";

    // Unique notification ID for the persistent foreground notification
    private static final int NOTIFICATION_ID = 1001;

    // Speed calculation interval in milliseconds (100ms = 10 updates/sec)
    private static final long SPEED_INTERVAL_MS = 100;
    // Notification update interval: update every Nth tick to reduce system overhead
    // 5 ticks × 100ms = 500ms between notification updates
    private static final int NOTIFICATION_THROTTLE_TICKS = 5;

    // Timer that fires speed calculation every 100ms
    private Timer timer;
    // Tick counter for throttling notification updates
    private int tickCount;
    // Handler for posting UI callbacks from the timer thread to the main thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // Reference to the notification manager for posting updates
    private NotificationManager notificationManager;
    // The shared traffic stats calculator
    private TrafficStatsCalculator calculator;

    // Static flag indicating whether the service is currently running
    private static volatile boolean running;

    /**
     * Callback interface for delivering speed updates to the UI.
     */
    public interface SpeedCallback {
        void onSpeedUpdate(double download, double upload, long sessionRx, long sessionTx);
    }

    // Static callback reference; the Activity registers/unregisters as listener
    private static volatile SpeedCallback callback;

    /** Registers a callback to receive live speed updates from the service. */
    public static void setCallback(SpeedCallback cb) {
        callback = cb;
    }

    /** Returns true if the service timer is actively running. */
    public static boolean isRunning() {
        return running;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
        calculator = NetSpeedApp.getCalculator();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_START.equals(intent.getAction())) {
                // Start foreground monitoring
                startMonitoring();
            } else if (ACTION_STOP.equals(intent.getAction())) {
                // Stop monitoring and shut down the service
                stopMonitoring();
            }
        }
        // START_STICKY: system restarts the service if it's killed
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // No binding supported
        return null;
    }

    /**
     * Promotes to foreground with an initial notification and starts the speed timer.
     * Guards against double-start: cancels any existing timer before creating a new one.
     */
    private void startMonitoring() {
        // Cancel any existing timer to prevent duplicate scheduling on rapid taps
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // Reset calculator for a fresh session
        NetSpeedApp.resetCalculator();
        calculator = NetSpeedApp.getCalculator();

        // Build initial zero-speed notification to satisfy foreground requirement
        Notification notification = buildNotification("0 B/s", "0 B/s", null);

        // Start foreground with service type for API 29+ compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        running = true;
        // Reset tick counter for notification throttling
        tickCount = 0;

        // Refresh home screen widgets to show "Live" status
        SpeedWidgetProvider.refreshAllWidgets(this);

        // Schedule speed calculation every 100ms for responsive UI updates
        timer = new Timer("SpeedTimer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Calculate current speed on the timer thread
                double[] speeds = calculator.calculateSpeed();
                long[] session = calculator.getSessionBytes();
                double dl = speeds[0];
                double ul = speeds[1];
                long sRx = session[0];
                long sTx = session[1];

                // Throttle notification updates to every 500ms (every 5th tick)
                // Notifications are heavyweight; updating too often wastes CPU and battery
                tickCount++;
                if (tickCount >= NOTIFICATION_THROTTLE_TICKS) {
                    tickCount = 0;

                    // Format speed for notification text
                    String dlText = SpeedUtils.formatSpeed(dl);
                    String ulText = SpeedUtils.formatSpeed(ul);

                    // Generate dynamic bitmap icon showing speed in status bar
                    Bitmap iconBitmap = SpeedIconGenerator.createSpeedIcon(dl);
                    Icon icon = Icon.createWithBitmap(iconBitmap);

                    // Update the notification with latest speed values
                    Notification n = buildNotification(dlText, ulText, icon);
                    notificationManager.notify(NOTIFICATION_ID, n);

                    // Broadcast speed to home screen widget (same 500ms throttle)
                    Intent widgetIntent = new Intent(SpeedWidgetProvider.ACTION_SPEED_UPDATE);
                    widgetIntent.setPackage(getPackageName());
                    widgetIntent.putExtra(SpeedWidgetProvider.EXTRA_DOWNLOAD_SPEED, dl);
                    widgetIntent.putExtra(SpeedWidgetProvider.EXTRA_UPLOAD_SPEED, ul);
                    sendBroadcast(widgetIntent);
                }

                // Deliver speed update to the UI callback on the main thread (every 100ms)
                mainHandler.post(() -> {
                    SpeedCallback cb = callback;
                    if (cb != null) {
                        cb.onSpeedUpdate(dl, ul, sRx, sTx);
                    }
                });
            }
        }, 0, SPEED_INTERVAL_MS);
    }

    /**
     * Stops the timer, removes the foreground notification, and terminates the service.
     */
    private void stopMonitoring() {
        running = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);

        // Refresh home screen widgets to show "Stopped" status and reset speed
        SpeedWidgetProvider.refreshAllWidgets(this);

        stopSelf();
    }

    /**
     * Builds a Notification with download/upload speed displayed.
     * Uses a dynamic bitmap icon when available, falls back to a static resource.
     */
    private Notification buildNotification(String downloadSpeed, String uploadSpeed, Icon dynamicIcon) {
        // PendingIntent to open MainActivity when notification is tapped
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // PendingIntent for the "Stop" action button in the notification
        Intent stopIntent = new Intent(this, SpeedMonitorService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Choose the notification icon: dynamic bitmap or static resource
        Icon smallIcon = (dynamicIcon != null)
                ? dynamicIcon
                : Icon.createWithResource(this, R.drawable.ic_speed_notification);

        // Build notification with speed values prominently displayed
        Notification.Builder builder = new Notification.Builder(this, NetSpeedApp.CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle("\u2193 " + downloadSpeed + "   \u2191 " + uploadSpeed)
                .setContentText("Tap to open Net Speed Monitor")
                .setSubText("Net Speed")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(Notification.PRIORITY_MAX)
                .setSortKey("0")
                .setBadgeIconType(Notification.BADGE_ICON_NONE)
                .setNumber(0)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_stop),
                        getString(R.string.notification_stop),
                        stopPendingIntent
                ).build())
                .setStyle(new Notification.BigTextStyle()
                        .setBigContentTitle("\u2193 " + downloadSpeed + "   \u2191 " + uploadSpeed)
                        .bigText("Download: " + downloadSpeed + "\nUpload: " + uploadSpeed));

        // Show notification immediately on Android 12+ (no 10-second delay)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        // Clean up timer to prevent leaks
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
