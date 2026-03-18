package com.netspeed.monitor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Application class annotated with @HiltAndroidApp to trigger Hilt code generation
// This is the entry point for Hilt's dependency injection in the app
@HiltAndroidApp
class NetSpeedApp : Application() {
    // Hilt generates the necessary component code; no manual setup needed here
}
