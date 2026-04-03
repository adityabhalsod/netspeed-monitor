package com.netspeed.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * Listens for device boot events and restarts the speed monitoring service
 * if the user enabled "Start on Boot" in settings.
 */
public class BootReceiver extends BroadcastReceiver {

    // Tag for logging boot receiver events
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Only respond to boot-related broadcasts
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
                !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }

        Log.d(TAG, "Boot broadcast received: " + action);

        // Read the start-on-boot preference from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean startOnBoot = prefs.getBoolean("start_on_boot", false);

        if (startOnBoot) {
            // Build intent to start the foreground speed monitoring service
            Intent serviceIntent = new Intent(context, SpeedMonitorService.class);
            serviceIntent.setAction(SpeedMonitorService.ACTION_START);

            try {
                // startForegroundService is required on API 26+ (our minSdk)
                context.startForegroundService(serviceIntent);
                Log.d(TAG, "Foreground service start requested successfully");
            } catch (Exception e) {
                // On Android 12+ a ForegroundServiceStartNotAllowedException may be thrown
                // if the system denies the background start despite the BOOT_COMPLETED exemption
                Log.e(TAG, "Failed to start foreground service on boot: " + e.getMessage(), e);
            }
        } else {
            Log.d(TAG, "Start on boot is disabled, skipping service start");
        }
    }
}
