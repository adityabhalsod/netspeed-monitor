# ============================================================
# Ultra-lightweight ProGuard/R8 rules for pure Java Android app
# No Kotlin, no AndroidX, no third-party libraries to worry about
# ============================================================

# Maximum optimization passes for smallest possible output
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Keep custom View referenced in XML layouts
-keep class com.netspeed.monitor.SpeedGaugeView { *; }

# Keep the SpeedCallback interface used across classes
-keep interface com.netspeed.monitor.SpeedMonitorService$SpeedCallback { *; }

# Keep inner data classes used by AppUsageTracker and DataUsageReport
-keep class com.netspeed.monitor.AppUsageTracker$AppUsageInfo { *; }
-keep class com.netspeed.monitor.DataUsageReport$DailyUsage { *; }

# Remove unused code aggressively
-dontwarn javax.annotation.**
