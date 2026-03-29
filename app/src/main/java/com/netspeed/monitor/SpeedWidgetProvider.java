package com.netspeed.monitor;

import android.app.PendingIntent;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Calendar;

/**
 * Home screen widget showing live download/upload speed and today's data usage.
 * Updates are driven by:
 *   - AppWidgetManager's updatePeriodMillis (minimum 30 min) for data usage refresh
 *   - SpeedMonitorService broadcasts for live speed updates when monitoring is active
 * Pure Java — no AndroidX, no third-party dependencies.
 */
public class SpeedWidgetProvider extends AppWidgetProvider {

    // Tag for error logging
    private static final String TAG = "SpeedWidget";

    // Broadcast action sent by SpeedMonitorService to push live speed to the widget
    public static final String ACTION_SPEED_UPDATE =
            "com.netspeed.monitor.action.WIDGET_SPEED_UPDATE";

    // Intent extras for speed values passed from the service
    public static final String EXTRA_DOWNLOAD_SPEED = "extra_download_speed";
    public static final String EXTRA_UPLOAD_SPEED = "extra_upload_speed";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update each widget instance; wrapped in try-catch to prevent widget add failure
        for (int widgetId : appWidgetIds) {
            try {
                updateWidget(context, appWidgetManager, widgetId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update widget " + widgetId, e);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // Handle live speed broadcast from SpeedMonitorService
        try {
            if (ACTION_SPEED_UPDATE.equals(intent.getAction())) {
                double download = intent.getDoubleExtra(EXTRA_DOWNLOAD_SPEED, 0.0);
                double upload = intent.getDoubleExtra(EXTRA_UPLOAD_SPEED, 0.0);
                updateSpeedValues(context, download, upload);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle speed broadcast", e);
        }
    }

    /**
     * Full widget update: sets click intent, speed values, status, and today's data usage.
     * Called on widget add and periodic refresh.
     */
    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_speed);

        // Tapping the widget opens the main activity
        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        // Show monitoring status based on whether the service is running
        boolean monitoring = SpeedMonitorService.isRunning();
        // Resolve status color from resources (auto-switches with dark/light mode)
        int stoppedColor = context.getColor(R.color.widget_status_stopped);
        if (monitoring) {
            views.setTextViewText(R.id.widget_status, "\u25CF Live");
            views.setTextColor(R.id.widget_status, 0xFF00E676);
        } else {
            views.setTextViewText(R.id.widget_status, "Stopped");
            views.setTextColor(R.id.widget_status, stoppedColor);
            // Reset speed display when not monitoring
            views.setTextViewText(R.id.widget_download_speed, "0 B/s");
            views.setTextViewText(R.id.widget_upload_speed, "0 B/s");
        }

        // Query and display today's data usage (WiFi + Mobile breakdown)
        loadTodayUsage(context, views);

        // Push the update to the widget
        appWidgetManager.updateAppWidget(widgetId, views);
    }

    /**
     * Lightweight update: only refreshes speed values and status indicator.
     * Called on every speed broadcast from the service for live display.
     */
    private void updateSpeedValues(Context context, double download, double upload) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, SpeedWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(widget);

        // Skip if no widgets are placed on the home screen
        if (widgetIds == null || widgetIds.length == 0) return;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_speed);

        // Format and display current download and upload speed
        views.setTextViewText(R.id.widget_download_speed, SpeedUtils.formatSpeed(download));
        views.setTextViewText(R.id.widget_upload_speed, SpeedUtils.formatSpeed(upload));

        // Update status indicator to show live monitoring
        boolean monitoring = SpeedMonitorService.isRunning();
        // Resolve status color from resources (auto-switches with dark/light mode)
        int stoppedColor = context.getColor(R.color.widget_status_stopped);
        if (monitoring) {
            views.setTextViewText(R.id.widget_status, "\u25CF Live");
            views.setTextColor(R.id.widget_status, 0xFF00E676);
        } else {
            views.setTextViewText(R.id.widget_status, "Stopped");
            views.setTextColor(R.id.widget_status, stoppedColor);
        }

        // Partial update: only changes the fields set above, preserves the rest
        manager.partiallyUpdateAppWidget(widgetIds, views);
    }

    /**
     * Queries today's WiFi and Mobile usage and populates the widget TextViews.
     * Uses the same NetworkStatsManager approach as DataUsageReport.
     */
    private void loadTodayUsage(Context context, RemoteViews views) {
        // Calculate today's start timestamp (midnight)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long dayStart = cal.getTimeInMillis();
        long now = System.currentTimeMillis();

        // Query WiFi and mobile usage for today
        long wifiBytes = queryTotalBytes(context, ConnectivityManager.TYPE_WIFI, dayStart, now);
        long mobileBytes = queryTotalBytes(context, ConnectivityManager.TYPE_MOBILE, dayStart, now);
        long totalBytes = wifiBytes + mobileBytes;

        // Display formatted byte values in the widget
        views.setTextViewText(R.id.widget_wifi_usage, SpeedUtils.formatBytes(wifiBytes));
        views.setTextViewText(R.id.widget_mobile_usage, SpeedUtils.formatBytes(mobileBytes));
        views.setTextViewText(R.id.widget_total_usage, SpeedUtils.formatBytes(totalBytes));
    }

    /**
     * Queries total bytes (rx + tx) for a network type within a time range.
     * Returns 0 if permission is missing or query fails.
     */
    private long queryTotalBytes(Context context, int networkType, long start, long end) {
        NetworkStatsManager statsManager =
                (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        if (statsManager == null) return 0;

        long total = 0;
        NetworkStats stats = null;
        try {
            // Query aggregated network stats for all UIDs
            stats = statsManager.querySummary(networkType, null, start, end);
        } catch (RemoteException | SecurityException e) {
            // SecurityException if PACKAGE_USAGE_STATS not granted
            return 0;
        }

        if (stats == null) return 0;

        // Sum all rx + tx bytes across all buckets
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        while (stats.hasNextBucket()) {
            stats.getNextBucket(bucket);
            total += bucket.getRxBytes() + bucket.getTxBytes();
        }
        // Release system resources
        stats.close();

        return total;
    }

    /**
     * Static helper: triggers a full update on all widget instances.
     * Called from SpeedMonitorService when monitoring starts/stops to refresh status.
     */
    public static void refreshAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, SpeedWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(widget);

        if (widgetIds != null && widgetIds.length > 0) {
            // Fire an UPDATE broadcast to trigger onUpdate for all widget instances
            Intent updateIntent = new Intent(context, SpeedWidgetProvider.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
            context.sendBroadcast(updateIntent);
        }
    }
}
