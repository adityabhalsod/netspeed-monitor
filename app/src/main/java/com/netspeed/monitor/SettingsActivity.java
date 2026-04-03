package com.netspeed.monitor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Settings activity: allows the user to toggle "Start on Boot" preference.
 * Pure Android framework — no AndroidX, no fragments, no third-party libraries.
 */
public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // SharedPreferences for reading and writing settings
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // Back button returns to the previous activity
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Start on Boot toggle switch
        Switch switchBoot = findViewById(R.id.switch_boot);
        // Initialize switch state from saved preference
        switchBoot.setChecked(prefs.getBoolean("start_on_boot", false));
        // Persist preference and enable/disable BootReceiver component when the user toggles
        switchBoot.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the preference synchronously with commit() for reliable persistence before reboot
            prefs.edit().putBoolean("start_on_boot", isChecked).commit();
            // Programmatically enable or disable the BootReceiver component so the system
            // knows to deliver BOOT_COMPLETED broadcasts — required for reliable boot start
            setBootReceiverEnabled(isChecked);
        });
    }

    /**
     * Enables or disables the BootReceiver component via PackageManager.
     * This tells the Android system whether to deliver BOOT_COMPLETED to this app,
     * which is more reliable than only checking SharedPreferences at boot time.
     */
    private void setBootReceiverEnabled(boolean enabled) {
        // Resolve the BootReceiver component by class name
        ComponentName receiver = new ComponentName(this, BootReceiver.class);
        // Choose enabled or disabled state based on the toggle
        int newState = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        // Apply the change without killing the app process (DONT_KILL_APP)
        getPackageManager().setComponentEnabledSetting(receiver, newState,
                PackageManager.DONT_KILL_APP);
    }
}
