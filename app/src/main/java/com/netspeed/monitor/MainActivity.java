package com.netspeed.monitor;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Main activity: displays live speed gauges, session usage, and monitoring toggle.
 * Pure Android framework — no AndroidX, no Compose, no third-party libraries.
 */
public class MainActivity extends Activity implements SpeedMonitorService.SpeedCallback {

    // Permission request code for POST_NOTIFICATIONS (Android 13+)
    private static final int REQ_NOTIFICATION = 100;

    // UI elements
    private SpeedGaugeView gaugeDownload;
    private SpeedGaugeView gaugeUpload;
    private TextView tvStatus;
    private TextView tvSessionRx;
    private TextView tvSessionTx;
    private Button btnToggle;

    // Shared preferences for persisting monitoring state
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SharedPreferences for app settings
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // Bind UI elements from the layout
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

        // Navigate to settings when the gear icon is tapped
        findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        // Request notification permission on Android 13+ (required for foreground service notification)
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
    protected void onResume() {
        super.onResume();
        // Register as the speed update listener when the activity is visible
        SpeedMonitorService.setCallback(this);
        // Sync UI with current monitoring state
        updateUiState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister callback to avoid leaking activity reference
        SpeedMonitorService.setCallback(null);
    }

    /**
     * Toggles the monitoring service on or off and updates the UI accordingly.
     */
    private void toggleMonitoring() {
        boolean wasMonitoring = prefs.getBoolean("service_enabled", false);
        boolean newState = !wasMonitoring;

        // Persist the new monitoring state
        prefs.edit().putBoolean("service_enabled", newState).apply();

        // Build the service intent with start or stop action
        Intent serviceIntent = new Intent(this, SpeedMonitorService.class);
        serviceIntent.setAction(newState
                ? SpeedMonitorService.ACTION_START
                : SpeedMonitorService.ACTION_STOP);

        if (newState) {
            // Start foreground service (API 26+ requires startForegroundService)
            startForegroundService(serviceIntent);
        } else {
            // Send stop action to the running service
            startService(serviceIntent);
        }

        // Update UI to reflect the new state
        updateUiState();
    }

    /**
     * Syncs all UI elements with the current monitoring state.
     */
    private void updateUiState() {
        boolean monitoring = SpeedMonitorService.isRunning();

        // Update status indicator pill
        if (monitoring) {
            tvStatus.setText("Monitoring");
            tvStatus.setTextColor(0xFF00E676);
            tvStatus.setBackgroundResource(R.drawable.bg_status_monitoring);
        } else {
            tvStatus.setText("Stopped");
            tvStatus.setTextColor(0xFF888888);
            tvStatus.setBackgroundResource(R.drawable.bg_status_stopped);
            // Reset gauges and session data when not monitoring
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

    /**
     * Called by SpeedMonitorService on the main thread with live speed data.
     */
    @Override
    public void onSpeedUpdate(double download, double upload, long sessionRx, long sessionTx) {
        // Update gauge displays with latest speed values
        gaugeDownload.setSpeed(download);
        gaugeUpload.setSpeed(upload);

        // Update session usage text
        tvSessionRx.setText(SpeedUtils.formatBytes(sessionRx));
        tvSessionTx.setText(SpeedUtils.formatBytes(sessionTx));

        // Ensure status shows monitoring state
        tvStatus.setText("Monitoring");
        tvStatus.setTextColor(0xFF00E676);
        tvStatus.setBackgroundResource(R.drawable.bg_status_monitoring);
    }
}
