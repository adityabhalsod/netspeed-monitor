package com.netspeed.monitor;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main activity: 3-tab layout with speed monitor, per-app usage, and data report.
 * Pure Android framework — no AndroidX, no Compose, no third-party libraries.
 */
public class MainActivity extends Activity implements SpeedMonitorService.SpeedCallback {

    // Permission request code for POST_NOTIFICATIONS (Android 13+)
    private static final int REQ_NOTIFICATION = 100;

    // --- Warning banner for missing notification permission ---
    private View warningNotification;

    // --- Tab 1: Speed monitor UI elements ---
    private View tabSpeed;
    private SpeedGaugeView gaugeDownload;
    private SpeedGaugeView gaugeUpload;
    private TextView tvStatus;
    private TextView tvSessionRx;
    private TextView tvSessionTx;
    private Button btnToggle;

    // --- Tab 2: App usage UI elements ---
    private View tabApps;
    private LinearLayout appListContainer;
    private View appPermissionPrompt;
    private View appLoading;
    private View appListScroll;
    private TextView[] filterPills;
    private int selectedPeriod = AppUsageTracker.PERIOD_DAY;

    // --- Tab 3: Data report UI elements ---
    private View tabReport;
    private TextView tvMonthLabel;
    private TextView tvTotalWifi;
    private TextView tvTotalMobile;
    private TextView tvTotalAll;
    private LinearLayout reportListContainer;
    private View reportPermissionPrompt;
    private View reportListScroll;
    private int reportYear;
    private int reportMonth;

    // --- Tab navigation ---
    private int currentTab = 0;
    private LinearLayout tabBtnSpeed;
    private LinearLayout tabBtnApps;
    private LinearLayout tabBtnReport;
    private ImageView tabIconSpeed;
    private ImageView tabIconApps;
    private ImageView tabIconReport;
    private TextView tabLabelSpeed;
    private TextView tabLabelApps;
    private TextView tabLabelReport;

    // Active tab color and inactive tab color
    private static final int COLOR_TAB_ACTIVE = 0xFF212121;
    private static final int COLOR_TAB_INACTIVE = 0xFF888888;

    // Shared preferences for persisting monitoring state
    private SharedPreferences prefs;

    // Background thread executor for loading usage data off the main thread
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // Handler for posting results back to the main thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Live refresh interval for Apps tab (3 seconds)
    private static final long APP_USAGE_REFRESH_INTERVAL = 3000;
    // Flag to avoid overlapping refresh queries
    private boolean isAppUsageLoading = false;
    // Runnable that periodically reloads app usage data while Apps tab is visible
    private final Runnable appUsageRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            // Only refresh if Apps tab is currently active
            if (currentTab == 1) {
                loadAppUsageSilent();
                // Schedule the next refresh
                mainHandler.postDelayed(this, APP_USAGE_REFRESH_INTERVAL);
            }
        }
    };

    // Month name labels for the data report header
    private static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SharedPreferences for app settings
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // Initialize the data report to the current month
        reportYear = DataUsageReport.getCurrentYear();
        reportMonth = DataUsageReport.getCurrentMonth();

        // --- Bind Tab 1: Speed monitor ---
        tabSpeed = findViewById(R.id.tab_speed);
        gaugeDownload = findViewById(R.id.gauge_download);
        gaugeUpload = findViewById(R.id.gauge_upload);
        tvStatus = findViewById(R.id.tv_status);
        tvSessionRx = findViewById(R.id.tv_session_rx);
        tvSessionTx = findViewById(R.id.tv_session_tx);
        btnToggle = findViewById(R.id.btn_toggle);

        // Configure download gauge: green arc with downward arrow
        gaugeDownload.setArcColor(0xFF00E676);
        gaugeDownload.setLabel("Download");
        gaugeDownload.setArrow("\u2193");

        // Configure upload gauge: orange arc with upward arrow
        gaugeUpload.setArcColor(0xFFFF9100);
        gaugeUpload.setLabel("Upload");
        gaugeUpload.setArrow("\u2191");

        // Toggle monitoring on button click
        btnToggle.setOnClickListener(v -> toggleMonitoring());

        // --- Bind Tab 2: App usage ---
        tabApps = findViewById(R.id.tab_apps);
        appListContainer = tabApps.findViewById(R.id.app_list_container);
        appPermissionPrompt = tabApps.findViewById(R.id.permission_prompt);
        appLoading = tabApps.findViewById(R.id.tv_loading);
        appListScroll = tabApps.findViewById(R.id.app_list_scroll);

        // Bind filter pills and set click listeners for each time period
        filterPills = new TextView[]{
                tabApps.findViewById(R.id.filter_hour),
                tabApps.findViewById(R.id.filter_day),
                tabApps.findViewById(R.id.filter_week),
                tabApps.findViewById(R.id.filter_month),
                tabApps.findViewById(R.id.filter_year)
        };
        for (int i = 0; i < filterPills.length; i++) {
            final int period = i;
            filterPills[i].setOnClickListener(v -> selectFilter(period));
        }

        // Grant permission button for Tab 2 — opens usage access page focused on our app
        tabApps.findViewById(R.id.btn_grant_permission).setOnClickListener(v ->
                openUsageAccessSettings());

        // Restricted settings button for Tab 2 — opens App Info to allow restricted settings
        tabApps.findViewById(R.id.btn_allow_restricted).setOnClickListener(v ->
                openAppInfoSettings());

        // --- Bind Tab 3: Data report ---
        tabReport = findViewById(R.id.tab_report);
        tvMonthLabel = tabReport.findViewById(R.id.tv_month_label);
        tvTotalWifi = tabReport.findViewById(R.id.tv_total_wifi);
        tvTotalMobile = tabReport.findViewById(R.id.tv_total_mobile);
        tvTotalAll = tabReport.findViewById(R.id.tv_total_all);
        reportListContainer = tabReport.findViewById(R.id.report_list_container);
        reportPermissionPrompt = tabReport.findViewById(R.id.report_permission_prompt);
        reportListScroll = tabReport.findViewById(R.id.report_list_scroll);

        // Month navigation buttons
        tabReport.findViewById(R.id.btn_prev_month).setOnClickListener(v -> navigateMonth(-1));
        tabReport.findViewById(R.id.btn_next_month).setOnClickListener(v -> navigateMonth(1));

        // Grant permission button for Tab 3 — opens usage access page focused on our app
        tabReport.findViewById(R.id.btn_report_grant).setOnClickListener(v ->
                openUsageAccessSettings());

        // Restricted settings button for Tab 3 — opens App Info to allow restricted settings
        tabReport.findViewById(R.id.btn_report_allow_restricted).setOnClickListener(v ->
                openAppInfoSettings());

        // --- Show restricted settings warning on Android 13+ for sideloaded apps ---
        if (isRestrictedSettingsApplicable()) {
            // Show the amber warning banner on both permission prompts
            tabApps.findViewById(R.id.restricted_warning).setVisibility(View.VISIBLE);
            tabReport.findViewById(R.id.report_restricted_warning).setVisibility(View.VISIBLE);

            // Update step instructions to include restricted settings as first step (Tab 2)
            ((TextView) tabApps.findViewById(R.id.tv_step1)).setText(
                    "1. Tap \"Allow Restricted Settings\" above first");
            ((TextView) tabApps.findViewById(R.id.tv_step2)).setText(
                    "2. In App Info, tap \u22EE menu \u2192 \"Allow restricted settings\"");
            ((TextView) tabApps.findViewById(R.id.tv_step3)).setText(
                    "3. Come back and tap \"Open Usage Access Settings\" below");
            ((TextView) tabApps.findViewById(R.id.tv_step4)).setText(
                    "4. Toggle the switch ON for Net Speed Monitor");

            // Update step instructions (Tab 3)
            ((TextView) tabReport.findViewById(R.id.tv_report_step1)).setText(
                    "1. Tap \"Allow Restricted Settings\" above first");
            ((TextView) tabReport.findViewById(R.id.tv_report_step2)).setText(
                    "2. In App Info, tap \u22EE menu \u2192 \"Allow restricted settings\"");
            ((TextView) tabReport.findViewById(R.id.tv_report_step3)).setText(
                    "3. Come back and tap \"Open Usage Access Settings\" below");
            ((TextView) tabReport.findViewById(R.id.tv_report_step4)).setText(
                    "4. Toggle the switch ON for Net Speed Monitor");
        }

        // --- Bottom tab bar navigation ---
        tabBtnSpeed = findViewById(R.id.tab_btn_speed);
        tabBtnApps = findViewById(R.id.tab_btn_apps);
        tabBtnReport = findViewById(R.id.tab_btn_report);
        tabIconSpeed = findViewById(R.id.tab_icon_speed);
        tabIconApps = findViewById(R.id.tab_icon_apps);
        tabIconReport = findViewById(R.id.tab_icon_report);
        tabLabelSpeed = findViewById(R.id.tab_label_speed);
        tabLabelApps = findViewById(R.id.tab_label_apps);
        tabLabelReport = findViewById(R.id.tab_label_report);

        tabBtnSpeed.setOnClickListener(v -> switchTab(0));
        tabBtnApps.setOnClickListener(v -> switchTab(1));
        tabBtnReport.setOnClickListener(v -> switchTab(2));

        // Navigate to settings when the gear icon is tapped
        findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        // --- Bind notification warning banner ---
        warningNotification = findViewById(R.id.warning_notification);

        // Grant button on notification warning banner
        findViewById(R.id.btn_grant_notification).setOnClickListener(v -> requestNotificationPermission());

        // Request notification permission on startup (Android 13+)
        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register as the speed update listener when the activity is visible
        SpeedMonitorService.setCallback(this);
        // Sync UI with current monitoring state
        updateUiState();

        // Refresh permission warning banners every time user returns to the app
        updatePermissionWarnings();

        // Refresh the currently visible tab's data (permission may have been granted)
        if (currentTab == 1) {
            loadAppUsage();
            startAppUsageRefresh();
        } else if (currentTab == 2) {
            loadDataReport();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister callback to avoid leaking activity reference
        SpeedMonitorService.setCallback(null);
        // Stop live refresh when activity is not visible
        stopAppUsageRefresh();
    }

    // ==================== Tab Switching ====================

    /**
     * Switches the visible tab and updates bottom bar highlight.
     */
    private void switchTab(int tab) {
        if (tab == currentTab) return;
        currentTab = tab;

        // Toggle visibility of the 3 tab content views
        tabSpeed.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        tabApps.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        tabReport.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

        // Update bottom bar icon and label colors
        updateTabBarColors(tab);

        // Load data when switching to a data tab
        if (tab == 1) {
            loadAppUsage();
            startAppUsageRefresh();
        } else {
            // Stop live refresh when leaving Apps tab
            stopAppUsageRefresh();
            if (tab == 2) {
                loadDataReport();
            }
        }
    }

    /**
     * Highlights the active tab icon/label and dims the others.
     */
    private void updateTabBarColors(int activeTab) {
        // Tab 1: Speed
        tabIconSpeed.setColorFilter(activeTab == 0 ? COLOR_TAB_ACTIVE : COLOR_TAB_INACTIVE);
        tabLabelSpeed.setTextColor(activeTab == 0 ? COLOR_TAB_ACTIVE : COLOR_TAB_INACTIVE);
        tabLabelSpeed.setTypeface(null, activeTab == 0 ? Typeface.BOLD : Typeface.NORMAL);

        // Tab 2: Apps
        tabIconApps.setColorFilter(activeTab == 1 ? COLOR_TAB_ACTIVE : COLOR_TAB_INACTIVE);
        tabLabelApps.setTextColor(activeTab == 1 ? COLOR_TAB_ACTIVE : COLOR_TAB_INACTIVE);
        tabLabelApps.setTypeface(null, activeTab == 1 ? Typeface.BOLD : Typeface.NORMAL);

        // Tab 3: Report
        tabIconReport.setColorFilter(activeTab == 2 ? COLOR_TAB_ACTIVE : COLOR_TAB_INACTIVE);
        tabLabelReport.setTextColor(activeTab == 2 ? COLOR_TAB_ACTIVE : COLOR_TAB_INACTIVE);
        tabLabelReport.setTypeface(null, activeTab == 2 ? Typeface.BOLD : Typeface.NORMAL);
    }

    // ==================== Tab 1: Speed Monitor ====================

    /**
     * Toggles the monitoring service on or off and updates the UI accordingly.
     */
    private void toggleMonitoring() {
        boolean isCurrentlyRunning = SpeedMonitorService.isRunning();

        if (isCurrentlyRunning) {
            // --- STOP ---
            prefs.edit().putBoolean("service_enabled", false).apply();
            Intent stopIntent = new Intent(this, SpeedMonitorService.class);
            stopIntent.setAction(SpeedMonitorService.ACTION_STOP);
            startService(stopIntent);
            applyUiState(false);
        } else {
            // --- START: request notification permission at runtime if not granted ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request permission — service will start after grant via onRequestPermissionsResult
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIFICATION
                );
                // Show toast explaining why this permission is needed
                Toast.makeText(this,
                        "\u26A0 Please allow notifications to see real-time speed in the status bar.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            // Permission already granted (or pre-Android 13) — start the service
            startMonitoringService();
        }
    }

    /**
     * Starts the foreground monitoring service and updates UI state.
     */
    private void startMonitoringService() {
        prefs.edit().putBoolean("service_enabled", true).apply();
        Intent startIntent = new Intent(this, SpeedMonitorService.class);
        startIntent.setAction(SpeedMonitorService.ACTION_START);
        startForegroundService(startIntent);
        applyUiState(true);
    }

    /**
     * Syncs all UI elements with the current monitoring state.
     */
    private void updateUiState() {
        applyUiState(SpeedMonitorService.isRunning());
    }

    /**
     * Applies the given monitoring state to all speed tab UI elements.
     */
    private void applyUiState(boolean monitoring) {
        // Update status indicator pill
        if (monitoring) {
            tvStatus.setText("Monitoring");
            tvStatus.setTextColor(0xFF00E676);
            tvStatus.setBackgroundResource(R.drawable.bg_status_monitoring);
        } else {
            tvStatus.setText("Stopped");
            tvStatus.setTextColor(0xFF888888);
            tvStatus.setBackgroundResource(R.drawable.bg_status_stopped);
            gaugeDownload.setSpeed(0);
            gaugeUpload.setSpeed(0);
            tvSessionRx.setText("0 B");
            tvSessionTx.setText("0 B");
        }

        // Update toggle button text and background
        if (monitoring) {
            btnToggle.setText("\u23F8  Stop Monitoring");
            btnToggle.setBackgroundResource(R.drawable.bg_button_stop);
        } else {
            btnToggle.setText("\u25B6  Start Monitoring");
            btnToggle.setBackgroundResource(R.drawable.bg_button_start);
        }
    }

    @Override
    public void onSpeedUpdate(double download, double upload, long sessionRx, long sessionTx) {
        gaugeDownload.setSpeed(download);
        gaugeUpload.setSpeed(upload);
        tvSessionRx.setText(SpeedUtils.formatBytes(sessionRx));
        tvSessionTx.setText(SpeedUtils.formatBytes(sessionTx));
        tvStatus.setText("Monitoring");
        tvStatus.setTextColor(0xFF00E676);
        tvStatus.setBackgroundResource(R.drawable.bg_status_monitoring);
    }

    // ==================== Tab 2: App Usage ====================

    /**
     * Checks usage access permission and loads app usage data on a background thread.
     * Shows loading indicator on first load.
     */
    private void loadAppUsage() {
        if (!hasUsageStatsPermission()) {
            // Show full-screen permission prompt, hide list
            appPermissionPrompt.setVisibility(View.VISIBLE);
            appListScroll.setVisibility(View.GONE);
            appLoading.setVisibility(View.GONE);
            return;
        }

        // Permission granted — show loading on first load, hide prompt
        appPermissionPrompt.setVisibility(View.GONE);
        appLoading.setVisibility(View.VISIBLE);
        appListScroll.setVisibility(View.GONE);

        // Query usage data off the main thread to avoid ANR
        isAppUsageLoading = true;
        executor.execute(() -> {
            AppUsageTracker tracker = new AppUsageTracker(this);
            List<AppUsageTracker.AppUsageInfo> apps = tracker.getAppUsage(selectedPeriod);

            // Post results back to main thread for UI update
            mainHandler.post(() -> {
                isAppUsageLoading = false;
                populateAppList(apps);
            });
        });
    }

    /**
     * Silently reloads app usage data without showing loading indicator.
     * Used by the live refresh timer to avoid flickering.
     */
    private void loadAppUsageSilent() {
        // Skip if permission missing or already loading
        if (!hasUsageStatsPermission() || isAppUsageLoading) return;

        isAppUsageLoading = true;
        executor.execute(() -> {
            AppUsageTracker tracker = new AppUsageTracker(this);
            List<AppUsageTracker.AppUsageInfo> apps = tracker.getAppUsage(selectedPeriod);

            mainHandler.post(() -> {
                isAppUsageLoading = false;
                // Only update if still on Apps tab
                if (currentTab == 1) {
                    populateAppList(apps);
                }
            });
        });
    }

    /**
     * Starts the periodic live refresh for app usage data.
     */
    private void startAppUsageRefresh() {
        // Remove any pending callbacks to avoid duplicates
        mainHandler.removeCallbacks(appUsageRefreshRunnable);
        // Schedule first refresh after the interval
        mainHandler.postDelayed(appUsageRefreshRunnable, APP_USAGE_REFRESH_INTERVAL);
    }

    /**
     * Stops the periodic live refresh for app usage data.
     */
    private void stopAppUsageRefresh() {
        mainHandler.removeCallbacks(appUsageRefreshRunnable);
    }

    /**
     * Updates the filter pill highlight and reloads app usage data.
     */
    private void selectFilter(int period) {
        selectedPeriod = period;

        // Update pill visual states
        for (int i = 0; i < filterPills.length; i++) {
            if (i == period) {
                filterPills[i].setBackgroundResource(R.drawable.bg_filter_active);
                filterPills[i].setTextColor(0xFFFFFFFF);
            } else {
                filterPills[i].setBackgroundResource(R.drawable.bg_filter_inactive);
                filterPills[i].setTextColor(0xFF888888);
            }
        }

        // Reload data with new period
        loadAppUsage();
    }

    /**
     * Builds the per-app usage list UI from the loaded data.
     * Each row shows: app icon, name, download/upload bytes, and total.
     */
    private void populateAppList(List<AppUsageTracker.AppUsageInfo> apps) {
        appListContainer.removeAllViews();
        appLoading.setVisibility(View.GONE);
        appListScroll.setVisibility(View.VISIBLE);

        if (apps.isEmpty()) {
            // Show empty state message
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("No app usage data for this period");
            emptyMsg.setTextSize(14);
            emptyMsg.setTextColor(0xFF888888);
            emptyMsg.setGravity(Gravity.CENTER);
            emptyMsg.setPadding(0, dpToPx(48), 0, 0);
            appListContainer.addView(emptyMsg);
            return;
        }

        // Create a card-style row for each app
        for (AppUsageTracker.AppUsageInfo app : apps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_card);
            int pad = dpToPx(12);
            row.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dpToPx(8);
            row.setLayoutParams(rowLp);
            row.setElevation(dpToPx(2));
            // Clip shadow to rounded card outline for clean edges
            row.setClipToOutline(true);
            row.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);

            // App icon
            ImageView icon = new ImageView(this);
            int iconSize = dpToPx(40);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconLp.setMarginEnd(dpToPx(12));
            icon.setLayoutParams(iconLp);
            icon.setImageDrawable(app.icon);
            row.addView(icon);

            // Text column: app name + download/upload detail
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textCol.setLayoutParams(textLp);

            // App name
            TextView name = new TextView(this);
            name.setText(app.appName);
            name.setTextSize(14);
            name.setTextColor(0xFF212121);
            name.setTypeface(null, Typeface.BOLD);
            name.setMaxLines(1);
            textCol.addView(name);

            // Download/upload breakdown
            TextView detail = new TextView(this);
            detail.setText("\u2193 " + SpeedUtils.formatBytes(app.rxBytes)
                    + "  \u2191 " + SpeedUtils.formatBytes(app.txBytes));
            detail.setTextSize(12);
            detail.setTextColor(0xFF888888);
            textCol.addView(detail);

            row.addView(textCol);

            // Total usage on the right
            TextView total = new TextView(this);
            total.setText(SpeedUtils.formatBytes(app.totalBytes));
            total.setTextSize(14);
            total.setTextColor(0xFF212121);
            total.setTypeface(null, Typeface.BOLD);
            row.addView(total);

            appListContainer.addView(row);
        }
    }

    // ==================== Tab 3: Data Report ====================

    /**
     * Navigates the month forward or backward and reloads the report.
     */
    private void navigateMonth(int delta) {
        reportMonth += delta;
        // Handle year rollover
        if (reportMonth > 11) {
            reportMonth = 0;
            reportYear++;
        } else if (reportMonth < 0) {
            reportMonth = 11;
            reportYear--;
        }
        loadDataReport();
    }

    /**
     * Checks usage access permission and loads the monthly report on a background thread.
     */
    private void loadDataReport() {
        // Update the month/year label in the navigation bar
        tvMonthLabel.setText(MONTH_NAMES[reportMonth] + " " + reportYear);

        if (!hasUsageStatsPermission()) {
            reportPermissionPrompt.setVisibility(View.VISIBLE);
            reportListScroll.setVisibility(View.GONE);
            return;
        }

        reportPermissionPrompt.setVisibility(View.GONE);
        reportListScroll.setVisibility(View.VISIBLE);

        // Query daily data off the main thread
        final int year = reportYear;
        final int month = reportMonth;
        executor.execute(() -> {
            DataUsageReport report = new DataUsageReport(this);
            List<DataUsageReport.DailyUsage> days = report.getMonthlyReport(year, month);

            mainHandler.post(() -> populateReportList(days));
        });
    }

    /**
     * Builds the daily usage list and updates the monthly summary totals.
     */
    private void populateReportList(List<DataUsageReport.DailyUsage> days) {
        reportListContainer.removeAllViews();

        // Accumulate monthly totals from all days
        long totalWifi = 0;
        long totalMobile = 0;
        for (DataUsageReport.DailyUsage day : days) {
            totalWifi += day.wifiBytes;
            totalMobile += day.mobileBytes;
        }

        // Update summary card with monthly totals
        tvTotalWifi.setText(SpeedUtils.formatBytes(totalWifi));
        tvTotalMobile.setText(SpeedUtils.formatBytes(totalMobile));
        tvTotalAll.setText(SpeedUtils.formatBytes(totalWifi + totalMobile));

        if (days.isEmpty()) {
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("No data for this month");
            emptyMsg.setTextSize(14);
            emptyMsg.setTextColor(0xFF888888);
            emptyMsg.setGravity(Gravity.CENTER);
            emptyMsg.setPadding(0, dpToPx(48), 0, 0);
            reportListContainer.addView(emptyMsg);
            return;
        }

        // Create a row for each day in the month
        for (DataUsageReport.DailyUsage day : days) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int hPad = dpToPx(16);
            int vPad = dpToPx(10);
            row.setPadding(hPad, vPad, hPad, vPad);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            // Alternate row background for readability
            if (day.dayOfMonth % 2 == 0) {
                row.setBackgroundColor(0xFFF5F5F5);
            }

            // Date label (weight 2)
            TextView dateView = new TextView(this);
            dateView.setText(day.dateLabel);
            dateView.setTextSize(13);
            dateView.setTextColor(0xFF212121);
            dateView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
            row.addView(dateView);

            // WiFi bytes (weight 1, right-aligned)
            TextView wifiView = new TextView(this);
            wifiView.setText(SpeedUtils.formatBytes(day.wifiBytes));
            wifiView.setTextSize(12);
            wifiView.setTextColor(0xFF4CAF50);
            wifiView.setGravity(Gravity.END);
            wifiView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(wifiView);

            // Mobile bytes (weight 1, right-aligned)
            TextView mobileView = new TextView(this);
            mobileView.setText(SpeedUtils.formatBytes(day.mobileBytes));
            mobileView.setTextSize(12);
            mobileView.setTextColor(0xFFFF9800);
            mobileView.setGravity(Gravity.END);
            mobileView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(mobileView);

            // Total bytes (weight 1, right-aligned)
            TextView totalView = new TextView(this);
            totalView.setText(SpeedUtils.formatBytes(day.totalBytes));
            totalView.setTextSize(12);
            totalView.setTextColor(0xFF2196F3);
            totalView.setTypeface(null, Typeface.BOLD);
            totalView.setGravity(Gravity.END);
            totalView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(totalView);

            reportListContainer.addView(row);
        }
    }

    // ==================== Utilities ====================

    /**
     * Opens the Usage Access Settings screen, focused directly on this app when possible.
     * On Android 10+ the package URI lets the system jump straight to our app's toggle.
     * Shows a Toast reminder so the user knows exactly what to do.
     */
    private void openUsageAccessSettings() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        // Pass our package URI so the system focuses directly on our app's toggle
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        try {
            startActivity(intent);
        } catch (Exception e) {
            // Some devices don't support the package-specific URI — fall back to generic page
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
        // Show Toast reminder so the user knows what to toggle
        Toast.makeText(this,
                "Please turn it ON for Net Speed Monitor to grant permission.",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Opens the App Info settings page for this app.
     * On Android 13+, the user can tap the 3-dot menu to "Allow restricted settings"
     * which unblocks special permissions like Usage Access for sideloaded apps.
     */
    private void openAppInfoSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        // Target our own package so the system opens our App Info directly
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
        // Guide the user to the restricted settings toggle in App Info
        Toast.makeText(this,
                "Tap \u22EE (3-dot menu) \u2192 \"Allow restricted settings\" to unblock permissions.",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Checks if restricted settings apply to this app.
     * On Android 13+ (API 33+), sideloaded apps have restricted settings enabled,
     * which blocks special permissions like PACKAGE_USAGE_STATS from being granted.
     * Returns true if the app is sideloaded on API 33+.
     */
    private boolean isRestrictedSettingsApplicable() {
        // Restricted settings only exist on Android 13+ (API 33)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false;

        try {
            // Check the install source — null initiating package means sideloaded
            InstallSourceInfo sourceInfo = getPackageManager()
                    .getInstallSourceInfo(getPackageName());
            String installer = sourceInfo.getInstallingPackageName();
            // If no installer (sideloaded via ADB/APK), restricted settings apply
            return installer == null;
        } catch (PackageManager.NameNotFoundException e) {
            // Package not found — shouldn't happen, but assume restricted as safe default
            return true;
        }
    }

    /**
     * Checks whether the app has PACKAGE_USAGE_STATS (Usage Access) permission.
     */
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    // ==================== Permission Handling ====================

    /**
     * Requests notification permission on Android 13+ if not already granted.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIFICATION
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (requestCode == REQ_NOTIFICATION) {
            if (granted) {
                // Permission just granted — auto-start monitoring if user tapped Start
                if (!SpeedMonitorService.isRunning()) {
                    startMonitoringService();
                }
            } else {
                // Denied — warn user with toast, banner stays visible for re-request
                Toast.makeText(this,
                        "\u26A0 Notification permission denied. Tap \u201cGrant\u201d in the banner or press Start to try again.",
                        Toast.LENGTH_LONG).show();
            }
        }

        // Refresh the notification warning banner
        updatePermissionWarnings();
    }

    /**
     * Updates notification permission warning banner visibility.
     * Called in onResume and after every permission result.
     */
    private void updatePermissionWarnings() {
        // Show notification warning banner on Android 13+ when permission is denied
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            warningNotification.setVisibility(View.VISIBLE);
        } else {
            warningNotification.setVisibility(View.GONE);
        }
    }

    /**
     * Converts dp to pixels using the display density.
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
