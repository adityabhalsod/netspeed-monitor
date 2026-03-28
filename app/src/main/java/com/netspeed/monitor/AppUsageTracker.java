package com.netspeed.monitor;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Queries per-app network usage data from NetworkStatsManager.
 * Provides sorted lists of apps with their download/upload bytes for a given time period.
 * Pure Java — no third-party dependencies.
 */
public class AppUsageTracker {

    // System service for querying detailed network stats per UID
    private final NetworkStatsManager statsManager;
    // Package manager for resolving app names and icons from UIDs
    private final PackageManager packageManager;

    // Time period constants for filtering usage data
    public static final int PERIOD_HOUR = 0;
    public static final int PERIOD_DAY = 1;
    public static final int PERIOD_WEEK = 2;
    public static final int PERIOD_MONTH = 3;
    public static final int PERIOD_YEAR = 4;

    /**
     * Represents a single app's network usage data for display in the list.
     */
    public static class AppUsageInfo implements Comparable<AppUsageInfo> {
        // Application display name
        public final String appName;
        // Application package name for identification
        public final String packageName;
        // Application icon drawable for list item display
        public final Drawable icon;
        // Total bytes received during the selected period
        public final long rxBytes;
        // Total bytes transmitted during the selected period
        public final long txBytes;
        // Combined download + upload for sorting
        public final long totalBytes;

        public AppUsageInfo(String appName, String packageName, Drawable icon,
                            long rxBytes, long txBytes) {
            this.appName = appName;
            this.packageName = packageName;
            this.icon = icon;
            this.rxBytes = rxBytes;
            this.txBytes = txBytes;
            // Pre-compute total for efficient sorting
            this.totalBytes = rxBytes + txBytes;
        }

        @Override
        public int compareTo(AppUsageInfo other) {
            // Sort descending by total bytes — heaviest users first
            return Long.compare(other.totalBytes, this.totalBytes);
        }
    }

    public AppUsageTracker(Context context) {
        // Obtain the system network stats manager
        statsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        // Obtain package manager for resolving UID to app info
        packageManager = context.getPackageManager();
    }

    /**
     * Queries per-app network usage for the given time period.
     * Returns a sorted list of AppUsageInfo with highest-usage apps first.
     */
    public List<AppUsageInfo> getAppUsage(int period) {
        // Calculate the start timestamp based on the selected period
        long endTime = System.currentTimeMillis();
        long startTime = getStartTime(period, endTime);

        // Accumulate bytes per UID across both WiFi and mobile networks
        Map<Integer, long[]> uidMap = new HashMap<>();

        // Query WiFi network stats for the time range
        queryNetworkType(ConnectivityManager.TYPE_WIFI, startTime, endTime, uidMap);
        // Query mobile network stats for the time range
        queryNetworkType(ConnectivityManager.TYPE_MOBILE, startTime, endTime, uidMap);

        // Convert UID map to a list of AppUsageInfo objects
        List<AppUsageInfo> result = new ArrayList<>();
        for (Map.Entry<Integer, long[]> entry : uidMap.entrySet()) {
            int uid = entry.getKey();
            long[] bytes = entry.getValue();
            long rx = bytes[0];
            long tx = bytes[1];

            // Skip UIDs with zero traffic — no point showing them
            if (rx + tx <= 0) continue;

            // Resolve the UID to package names (one UID can map to multiple packages)
            String[] packages = packageManager.getPackagesForUid(uid);
            if (packages == null || packages.length == 0) continue;

            // Use the first package name for this UID
            String pkgName = packages[0];
            try {
                // Load application metadata for display name and icon
                ApplicationInfo appInfo = packageManager.getApplicationInfo(pkgName, 0);
                String appName = packageManager.getApplicationLabel(appInfo).toString();
                Drawable icon = packageManager.getApplicationIcon(appInfo);
                result.add(new AppUsageInfo(appName, pkgName, icon, rx, tx));
            } catch (PackageManager.NameNotFoundException e) {
                // App was uninstalled — skip this entry
            }
        }

        // Sort by total usage descending — highest consumers at the top
        Collections.sort(result);
        return result;
    }

    /**
     * Queries NetworkStatsManager for a specific network type and accumulates bytes per UID.
     */
    private void queryNetworkType(int networkType, long startTime, long endTime,
                                  Map<Integer, long[]> uidMap) {
        NetworkStats stats = null;
        try {
            // Query the system for detailed per-UID network stats
            stats = statsManager.querySummary(networkType, null, startTime, endTime);
        } catch (RemoteException | SecurityException e) {
            // SecurityException if PACKAGE_USAGE_STATS not granted; RemoteException for IPC failure
            return;
        }

        if (stats == null) return;

        // Reusable bucket object to reduce allocations while iterating
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        while (stats.hasNextBucket()) {
            stats.getNextBucket(bucket);
            int uid = bucket.getUid();

            // Get or create the byte accumulator array for this UID
            long[] bytes = uidMap.get(uid);
            if (bytes == null) {
                bytes = new long[]{0L, 0L};
                uidMap.put(uid, bytes);
            }

            // Accumulate received bytes (download)
            bytes[0] += bucket.getRxBytes();
            // Accumulate transmitted bytes (upload)
            bytes[1] += bucket.getTxBytes();
        }
        // Close the stats cursor to free system resources
        stats.close();
    }

    /**
     * Calculates the start timestamp for the given period relative to endTime.
     */
    private long getStartTime(int period, long endTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(endTime);

        switch (period) {
            case PERIOD_HOUR:
                // Subtract 1 hour for recent usage
                cal.add(Calendar.HOUR_OF_DAY, -1);
                break;
            case PERIOD_DAY:
                // Subtract 24 hours for daily usage
                cal.add(Calendar.DAY_OF_YEAR, -1);
                break;
            case PERIOD_WEEK:
                // Subtract 7 days for weekly usage
                cal.add(Calendar.DAY_OF_YEAR, -7);
                break;
            case PERIOD_MONTH:
                // Subtract 1 month for monthly usage
                cal.add(Calendar.MONTH, -1);
                break;
            case PERIOD_YEAR:
                // Subtract 1 year for yearly usage
                cal.add(Calendar.YEAR, -1);
                break;
            default:
                // Default to 24 hours if unknown period
                cal.add(Calendar.DAY_OF_YEAR, -1);
                break;
        }

        return cal.getTimeInMillis();
    }
}
