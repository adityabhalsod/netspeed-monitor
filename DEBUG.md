# 🛠️ Build, Debug & Deploy Guide

Complete instructions for running **Net Speed Monitor** on a physical Android device using ADB — for both debug development and production release builds.

---

## 📋 Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [One-Time Setup](#2-one-time-setup)
3. [Connect Your Device via ADB](#3-connect-your-device-via-adb)
4. [Debug Build — Quick Run](#4-debug-build--quick-run)
5. [Install via ADB Manually](#5-install-via-adb-manually)
6. [Logcat — View Live Logs](#6-logcat--view-live-logs)
7. [Production Release Build](#7-production-release-build)
8. [Signing the Release APK](#8-signing-the-release-apk)
9. [Useful ADB Commands](#9-useful-adb-commands)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Prerequisites

Before building, make sure all of these are installed on your development machine:

| Tool | Minimum Version | How to Check |
|---|---|---|
| **JDK** | 17 | `java -version` |
| **Android SDK** | API 34 | Via Android Studio SDK Manager |
| **Android NDK** | r25c | Via Android Studio SDK Manager |
| **CMake** | 3.22.1 | Via Android Studio SDK Manager |
| **ADB** | Any recent | `adb version` |

> **Tip:** If you have Android Studio installed, JDK, NDK, CMake, and ADB are bundled with it. Add Android Studio's `bin` to your `PATH`:
> ```bash
> export PATH="$PATH:$HOME/Android/Sdk/platform-tools"
> ```

---

## 2. One-Time Setup

Run these once after cloning the repo:

```bash
# Step 1: Download the Gradle wrapper JAR (required to run ./gradlew)
curl -L https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar \
  -o gradle/wrapper/gradle-wrapper.jar

# Step 2: Make the Gradle wrapper executable
chmod +x gradlew

# Step 3: Verify the wrapper works (downloads Gradle 8.5 on first run)
./gradlew --version
```

Expected output:
```
Gradle 8.5
Kotlin: 1.9.22
...
```

---

## 3. Connect Your Device via ADB

### Option A — USB Connection
1. On your Android device: go to **Settings → About Phone → tap Build Number 7 times** to enable Developer Options.
2. Go to **Settings → Developer Options → enable USB Debugging**.
3. Connect via USB cable.
4. Accept the "Allow USB Debugging?" prompt on the device.

```bash
adb devices
# Expected output:
# List of devices attached
# XXXXXXXX    device
```

### Option B — Wi-Fi (Wireless ADB)

**Android 11+ (easiest):**
1. Go to **Settings → Developer Options → Wireless Debugging → Pair device with QR code or pairing code**.
2. Use Android Studio's "Pair Devices Using Wi-Fi" or run:
   ```bash
   adb pair <ip>:<port>
   # Enter the pairing code shown on screen, then:
   adb connect <ip>:<port>
   ```

**Android 8–10:**
```bash
# While the device is connected via USB first:
adb tcpip 5555
adb connect 192.168.1.2:5555       # replace with your device's IP
adb disconnect                      # unplug USB if desired
```

### Confirm Connection

```bash
adb devices
# Example output with both wired and wireless:
# 192.168.1.2:45283                               device
# adb-RZCW61S07FX-FPqh1w._adb-tls-connect._tcp   device
```

### Target a Specific Device

If multiple devices are listed, prefix every `adb` command with `-s <serial>`:

```bash
adb -s 192.168.1.2:45283   <command>
# or set it globally for the session:
export ANDROID_SERIAL=192.168.1.2:45283
```

---

## 4. Debug Build — Quick Run

The fastest way to build and install in one command:

```bash
./gradlew installDebug
```

This will:
1. Download all Gradle/Maven dependencies (~300 MB on first run)
2. Compile the Kotlin source files
3. Compile the C++ native library (`libnetspeed.so`) via CMake/NDK
4. Package into a debug APK
5. Install it directly to all connected devices

**Launch the app immediately after install:**

```bash
adb shell am start -n com.netspeed.monitor.debug/.MainActivity
```

> **Note:** The debug variant appends `.debug` to the package ID (`com.netspeed.monitor.debug`).  
> The release build uses `com.netspeed.monitor`.

---

## 5. Install via ADB Manually

If you want to build the APK first and install separately:

```bash
# Build only — does not install
./gradlew assembleDebug

# APK is produced at:
# app/build/outputs/apk/debug/app-debug.apk

# Install to a specific device
adb -s 192.168.1.2:45283 install -r app/build/outputs/apk/debug/app-debug.apk

# Flags:
#   -r  = replace existing app (re-install without uninstalling)
#   -d  = allow version downgrade (useful during dev)
#   -t  = allow test APKs
```

**Uninstall the app:**
```bash
adb uninstall com.netspeed.monitor.debug
```

---

## 6. Logcat — View Live Logs

### Filter to just this app

```bash
# All logs from the app
adb logcat --pid=$(adb shell pidof -s com.netspeed.monitor.debug)

# Or filter by tag (our C++ native layer uses these tags):
adb logcat -s NetSpeedNative:D NetSpeedJNI:D

# Filter all app tags at once
adb logcat | grep -E "NetSpeed|com.netspeed"
```

### Useful log tags in this project

| Tag | Source | What it shows |
|---|---|---|
| `NetSpeedNative` | `netspeed_calculator.cpp` | C++ /proc read failures |
| `NetSpeedJNI` | `netspeed_jni.cpp` | JNI init, reset events |
| `SpeedMonitorService` | `SpeedMonitorService.kt` | Service start/stop lifecycle |
| `BootReceiver` | `BootReceiver.kt` | Boot-triggered start events |

### Clear the log buffer before testing

```bash
adb logcat -c && adb logcat | grep -E "NetSpeed|com.netspeed"
```

### Save logs to a file

```bash
adb logcat -d > logs/$(date +%Y%m%d_%H%M%S)_debug.log
```

---

## 7. Production Release Build

> ⚠️ Release builds require a **signing keystore**. Never use a debug key for production.

### Step 1 — Generate a Keystore (once)

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias netspeed \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

You'll be prompted for:
- Keystore password
- Key password
- Name, organization, country

> **Important:** Store `release.keystore` securely. If lost, you cannot update your app on the Play Store.

### Step 2 — Configure Signing in `app/build.gradle.kts`

Add a `signingConfigs` block and reference it in `buildTypes.release`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")       // path to your keystore
            storePassword = System.getenv("KEYSTORE_PASS") // read from env var
            keyAlias = "netspeed"
            keyPassword = System.getenv("KEY_PASS")        // read from env var
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

> Use environment variables for passwords — never commit secrets to source control.

### Step 3 — Build the Signed Release APK

```bash
# Set credentials as environment variables
export KEYSTORE_PASS="your_keystore_password"
export KEY_PASS="your_key_password"

# Build signed release APK
./gradlew assembleRelease

# Output location:
# app/build/outputs/apk/release/app-release.apk
```

### Step 4 — Verify the Signature

```bash
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
# Expected: "Verified using v2 scheme (APK Signature Scheme v2): true"
```

### Step 5 — Install Release APK via ADB

```bash
adb -s 192.168.1.2:45283 install -r app/build/outputs/apk/release/app-release.apk

# Launch the release app:
adb shell am start -n com.netspeed.monitor/.MainActivity
```

---

## 8. Signing the Release APK

### Alternative: Build AAB (Android App Bundle) for Play Store

```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

Use `bundleRelease` instead of `assembleRelease` when submitting to Google Play — the Play Store handles APK splitting automatically.

---

## 9. Useful ADB Commands

```bash
# List connected devices
adb devices

# Get device Android version
adb shell getprop ro.build.version.release

# Get device API level
adb shell getprop ro.build.version.sdk

# Start the app
adb shell am start -n com.netspeed.monitor.debug/.MainActivity

# Stop the app forcefully
adb shell am force-stop com.netspeed.monitor.debug

# Start the monitoring foreground service manually
adb shell am startservice \
  -n com.netspeed.monitor.debug/.service.SpeedMonitorService \
  -a com.netspeed.monitor.action.START

# Stop the monitoring service manually
adb shell am startservice \
  -n com.netspeed.monitor.debug/.service.SpeedMonitorService \
  -a com.netspeed.monitor.action.STOP

# Simulate a device boot broadcast (test BootReceiver)
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED \
  -n com.netspeed.monitor.debug/.receiver.BootReceiver

# Grant notification permission without UI prompt (Android 13+, debug only)
adb shell pm grant com.netspeed.monitor.debug android.permission.POST_NOTIFICATIONS

# Revoke notification permission
adb shell pm revoke com.netspeed.monitor.debug android.permission.POST_NOTIFICATIONS

# Check if network stats are accessible on device
adb shell cat /proc/net/dev

# Take a screenshot
adb exec-out screencap -p > screenshot_$(date +%Y%m%d_%H%M%S).png

# Pull APK from device (if you need to extract an installed APK)
adb shell pm path com.netspeed.monitor.debug
adb pull <path from above>
```

---

## 10. Troubleshooting

### `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

The installed version was signed with a different key.

```bash
adb uninstall com.netspeed.monitor.debug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

### `adb: error: failed to get feature set: no devices/emulators found`

ADB cannot see your device.

```bash
# Re-plug USB or re-connect wireless
adb kill-server
adb start-server
adb devices
```

If using wireless: ensure your device and computer are on the same Wi-Fi network.

---

### Build fails: `NDK not configured`

The NDK path is missing from your SDK manager.

1. Open **Android Studio → SDK Manager → SDK Tools**.
2. Enable "NDK (Side by side)" and "CMake".
3. Click Apply.

Or install via CLI:
```bash
sdkmanager "ndk;25.2.9519653" "cmake;3.22.1"
```

---

### App crashes on launch: `java.lang.UnsatisfiedLinkError: No implementation found for...`

The native `.so` library failed to load. Ensure:

1. The `externalNativeBuild` block in `app/build.gradle.kts` points to the correct `CMakeLists.txt` path.
2. The `abiFilters` in the NDK block includes the ABI of your device:
   ```kotlin
   abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
   ```
3. Clean and rebuild:
   ```bash
   ./gradlew clean assembleDebug
   ```

---

### Service doesn't show notification

On **Android 13+**, the app needs `POST_NOTIFICATIONS` permission. Grant it manually via:

```bash
adb shell pm grant com.netspeed.monitor.debug android.permission.POST_NOTIFICATIONS
```

Or tap the permission dialog that appears on first launch.

---

### Speed always shows `0 B/s`

On emulators, `/proc/net/dev` may not reflect real traffic. **Test on a physical device.**

To confirm `/proc/net/dev` is accessible:
```bash
adb shell cat /proc/net/dev
```
You should see interface rows like `wlan0` or `rmnet0` with non-zero byte counters.

---

### Gradle build stuck / hanging

```bash
# Kill all running Gradle daemons and retry
./gradlew --stop
./gradlew assembleDebug
```

---

### Clean Build (when in doubt)

```bash
./gradlew clean
./gradlew assembleDebug
```

This deletes all cached build outputs and compiles everything from scratch.

---

*For architecture and feature documentation, see [README.md](README.md).*
