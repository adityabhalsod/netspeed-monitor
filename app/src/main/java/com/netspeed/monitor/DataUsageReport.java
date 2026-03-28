package com.netspeed.monitor;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Queries daily network usage data broken down by WiFi and Mobile.
 * Provides a monthly calendar-style report showing per-day data consumption.
 * Pure Java — no third-party dependencies.
 */
public class DataUsageReport {

    // System service for querying network usage statistics
    private final NetworkStatsManager statsManager;

    /**
     * Represents one day's data usage with WiFi and Mobile breakdowns.
     */
    public static class DailyUsage {
        // The date label for display (e.g., "Mar 15, 2026")
        public final String dateLabel;
        // Day of month for sorting/grouping
        public final int dayOfMonth;
        // Bytes received + transmitted over WiFi
        public final long wifiBytes;
        // Bytes received + transmitted over mobile data
        public final long mobileBytes;
        // Combined total of WiFi + mobile for summary display
        public final long totalBytes;

        public DailyUsage(String dateLabel, int dayOfMonth,
                          long wifiBytes, long mobileBytes) {
            this.dateLabel = dateLabel;
            this.dayOfMonth = dayOfMonth;
            this.wifiBytes = wifiBytes;
            this.mobileBytes = mobileBytes;
            // Pre-compute total for sorted display
            this.totalBytes = wifiBytes + mobileBytes;
        }
    }

    public DataUsageReport(Context context) {
        // Obtain the system network stats manager
        statsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
    }

    /**
     * Returns daily usage data for the specified month (0-indexed) and year.
     * Each entry represents one day with WiFi and Mobile byte totals.
     */
    public List<DailyUsage> getMonthlyReport(int year, int month) {
        List<DailyUsage> result = new ArrayList<>();

        // Determine the number of days in the specified month
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Month name abbreviations for date labels
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String monthName = monthNames[month];

        // Query each day in the month individually
        for (int day = 1; day <= daysInMonth; day++) {
            // Set start of day: midnight (00:00:00.000)
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long dayStart = cal.getTimeInMillis();

            // Set end of day: next midnight
            cal.add(Calendar.DAY_OF_MONTH, 1);
            long dayEnd = cal.getTimeInMillis();

            // Skip future days — no data to show
            if (dayStart > System.currentTimeMillis()) break;

            // Query WiFi bytes for this single day
            long wifiBytes = queryDayBytes(ConnectivityManager.TYPE_WIFI, dayStart, dayEnd);
            // Query mobile bytes for this single day
            long mobileBytes = queryDayBytes(ConnectivityManager.TYPE_MOBILE, dayStart, dayEnd);

            // Format the date label for list display
            String dateLabel = monthName + " " + day + ", " + year;
            result.add(new DailyUsage(dateLabel, day, wifiBytes, mobileBytes));
        }

        return result;
    }

    /**
     * Returns the current year from the system calendar.
     */
    public static int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /**
     * Returns the current month (0-indexed: Jan=0, Dec=11).
     */
    public static int getCurrentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH);
    }

    /**
     * Queries total bytes (rx + tx) for a specific network type within a time range.
     */
    private long queryDayBytes(int networkType, long start, long end) {
        long totalBytes = 0;
        NetworkStats stats = null;
        try {
            // Query aggregated stats for all UIDs on this network type
            stats = statsManager.querySummary(networkType, null, start, end);
        } catch (RemoteException | SecurityException e) {
            // Permission not granted or IPC failure — return 0
            return 0;
        }

        if (stats == null) return 0;

        // Iterate all buckets and sum rx + tx bytes
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        while (stats.hasNextBucket()) {
            stats.getNextBucket(bucket);
            // Accumulate both received and transmitted bytes
            totalBytes += bucket.getRxBytes() + bucket.getTxBytes();
        }
        // Close the cursor to free system resources
        stats.close();

        return totalBytes;
    }
}
