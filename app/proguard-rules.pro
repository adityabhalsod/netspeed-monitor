# Add project specific ProGuard rules here.

# ===== Hilt / Dagger =====
# Keep all classes annotated with Hilt/Dagger annotations
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ===== Kotlin Coroutines =====
# Retain coroutine debug information
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ===== DataStore =====
# Prevent stripping of DataStore preference keys
-keep class androidx.datastore.** { *; }

# ===== JNI / Native bridge =====
# Keep the NativeSpeedBridge class and all its native external methods
# so JNI registration works correctly at runtime
-keep class com.netspeed.monitor.data.native_.NativeSpeedBridge { *; }

# ===== Android Components =====
# Keep the Application, Activity, Service and BroadcastReceiver classes used by the manifest
-keep class com.netspeed.monitor.NetSpeedApp { *; }
-keep class com.netspeed.monitor.MainActivity { *; }
-keep class com.netspeed.monitor.service.SpeedMonitorService { *; }
-keep class com.netspeed.monitor.receiver.BootReceiver { *; }

# ===== Kotlin =====
# Suppress warning for Kotlin intrinsics
-dontwarn kotlin.Unit
-dontwarn kotlin.reflect.**
