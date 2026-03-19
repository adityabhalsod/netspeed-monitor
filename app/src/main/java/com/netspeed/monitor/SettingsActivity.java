package com.netspeed.monitor;

import android.app.Activity;
import android.content.SharedPreferences;
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
        // Persist preference when the user toggles the switch
        switchBoot.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("start_on_boot", isChecked).apply());
    }
}
