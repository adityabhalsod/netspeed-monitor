<div align="center">

# ⚡ Net Speed Monitor

**An ultra-lightweight, always-on Android internet speed monitor.**
Real-time upload & download speed — right in your status bar, every second.

![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Language-Java%2011-ED8B00?logo=openjdk&logoColor=white)
![APK Size](https://img.shields.io/badge/APK%20Size-~22%20KB-brightgreen)
![Dependencies](https://img.shields.io/badge/Dependencies-Zero-blue)
![License](https://img.shields.io/badge/License-MIT-blue)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-green)

**~22 KB release APK · Zero dependencies · Pure Android SDK**

</div>

---

## 👋 What does this app do?

Net Speed Monitor is an ultra-lightweight Android app that **continuously shows how fast your internet is — right now**.

- 📥 **Download speed** — how fast data is coming into your phone
- 📤 **Upload speed** — how fast data is going out from your phone
- 🔔 **Status bar notification** — always visible with dynamic speed icon, even when the app is closed
- 📊 **Session data usage** — total bytes received and transmitted since monitoring started
- ⚙️ **Works in the background** — no need to keep the app open
- 🚀 **Start on boot** — monitoring begins the moment your phone turns on

The app is designed to be **minimal, fast, and battery-friendly**. It uses no ads, no tracking, no internet permissions, and **zero third-party libraries** — only the Android SDK.

---

## ✨ Features

| Feature | Details |
|---|---|
| ⚡ Real-time speed | Updates every 1 second via `TrafficStats` API |
| 🔔 Sticky notification | Dynamic bitmap icon shows speed value in status bar |
| 🎨 Custom gauge UI | Hand-drawn arc gauges using `Canvas` API |
| 🔋 Battery friendly | Reads kernel byte counters — no packet inspection |
| 📶 All interfaces | Tracks Wi-Fi, mobile data, ethernet — all at once |
| 🔁 Start on boot | Auto-restarts monitoring after device reboot |
| 📦 Tiny footprint | ~22 KB release APK with R8 optimization |
| 🚫 No dependencies | Zero libraries — only Android SDK classes |

---

## 🏗️ Architecture

This is a **pure Java** Android application with a flat package structure. No Kotlin, no Compose, no AndroidX, no JNI/NDK — just Java and the Android SDK.

### Project Structure

```
netspeed-monitor/
├── app/
│   ├── build.gradle.kts                    # App build config (zero dependencies)
│   ├── proguard-rules.pro                  # R8 shrinking rules
│   └── src/main/
│       ├── AndroidManifest.xml             # Permissions, components
│       ├── java/com/netspeed/monitor/
│       │   ├── NetSpeedApp.java            # Application class, notification channel
│       │   ├── MainActivity.java           # Dashboard with gauges & toggle
│       │   ├── SettingsActivity.java       # Start-on-boot preference
│       │   ├── SpeedMonitorService.java    # Foreground service, 1s timer
│       │   ├── BootReceiver.java           # Restarts service after reboot
│       │   ├── TrafficStatsCalculator.java # Speed calculation from kernel counters
│       │   ├── SpeedIconGenerator.java     # Dynamic bitmap icon for status bar
│       │   ├── SpeedGaugeView.java         # Custom arc gauge View (Canvas)
│       │   └── SpeedUtils.java             # Speed/bytes formatting utilities
│       └── res/
│           ├── layout/                     # activity_main.xml, activity_settings.xml
│           ├── drawable/                   # Vector icons, shape backgrounds
│           ├── mipmap/                     # Adaptive launcher icons
│           └── values/                     # colors, strings, themes
├── build.gradle.kts                        # Root build config (AGP only)
├── settings.gradle.kts                     # Module config
├── gradle.properties                       # Build performance flags
├── .github/workflows/release.yml           # CI/CD: build, sign, release
├── DEBUG.md                                # Build & debug guide
├── COMMAND.md                              # ADB command reference
└── README.md                               # This file
```

### Component Overview

```
┌─────────────────────────────────────────────────────┐
│                    NetSpeedApp                       │
│          (Application, notification channel)         │
└───────────┬─────────────────────┬───────────────────┘
            │                     │
            ▼                     ▼
┌───────────────────┐   ┌────────────────────┐
│   MainActivity    │   │ SpeedMonitorService │
│  (Dashboard UI)   │   │ (Foreground Service)│
│                   │   │                     │
│ ┌───────────────┐ │   │  Timer (1s tick)    │
│ │SpeedGaugeView │ │   │       │             │
│ │ (Canvas arcs) │ │   │       ▼             │
│ └───────────────┘ │   │  calculateSpeed()   │
│                   │◄──│       │             │
│  SpeedCallback    │   │       ▼             │
│  onSpeedUpdate()  │   │  Notification       │
└───────────────────┘   │  (dynamic icon)     │
                        └────────┬────────────┘
                                 │
            ┌────────────────────┼────────────────────┐
            ▼                    ▼                    ▼
  TrafficStatsCalculator   SpeedIconGenerator    SpeedUtils
  (Android TrafficStats)   (Bitmap generation)   (Formatting)
```

---

## 🔬 How It Works

### Speed Calculation

The app uses Android's `TrafficStats` API to read cumulative byte counters from the Linux kernel:

1. Every **1 second**, a `Timer` thread reads `TrafficStats.getTotalRxBytes()` and `getTotalTxBytes()`
2. Delta bytes are computed by subtracting the previous snapshot from the current one
3. Speed = `ΔBytes / ΔTime` (bytes per second)
4. Negative deltas (counter resets) are guarded against
5. Results are delivered to the UI via a `SpeedCallback` interface on the main thread

### Dynamic Status Bar Icon

`SpeedIconGenerator` creates a 130×130 `Bitmap` (ALPHA_8) each second with the current download speed rendered as text. This bitmap is set as the notification's small icon via `Icon.createWithBitmap()`, giving a live speed readout directly in the status bar.

### Foreground Service

`SpeedMonitorService` runs as an Android foreground service with `FOREGROUND_SERVICE_TYPE_DATA_SYNC`:

- **START_STICKY** — system restarts the service if killed
- **1-second Timer** — fires `calculateSpeed()` and updates notification
- **SpeedCallback** — delivers updates to `MainActivity` when in foreground
- **Badge suppressed** — `setShowBadge(false)` on channel + `BADGE_ICON_NONE` on notification

### Boot Auto-Start

`BootReceiver` listens for `BOOT_COMPLETED` and `QUICKBOOT_POWERON`. If the user has enabled "Start on Boot" in settings (stored via `SharedPreferences`), the receiver starts the foreground service.

---

## 🔐 Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_NETWORK_STATE` | Reads active network interface information |
| `FOREGROUND_SERVICE` | Required to run a persistent background service |
| `FOREGROUND_SERVICE_DATA_SYNC` | Declares service type on Android 14+ |
| `RECEIVE_BOOT_COMPLETED` | Restarts monitoring after device reboot |
| `POST_NOTIFICATIONS` | Required on Android 13+ to show notifications |

> **Privacy:** This app reads only your device's own network byte counters from the Linux kernel. It does **not** access the internet, capture packet content, or collect any personal data.

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| UI | Android `View` + Custom `Canvas` drawing |
| Async | `java.util.Timer` + `android.os.Handler` |
| Storage | `SharedPreferences` |
| DI | None (manual singleton) |
| Build | Gradle 8.5 + AGP 8.3 + R8 |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 34 (Android 14) |
| Dependencies | **Zero** — only Android SDK |
| Release APK | **~22 KB** |

---

## 🔧 Requirements

| Requirement | Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 or newer |
| Gradle | 8.5 (via wrapper) |
| Android Device / Emulator | API 26+ (Android 8.0+) |

No NDK, CMake, or Kotlin plugin needed.

---

## 🚀 Build & Run

See **[DEBUG.md](DEBUG.md)** for the full guide covering prerequisites, ADB setup, logcat filtering, signing, and troubleshooting.

### Debug Build

```bash
chmod +x gradlew

# Build and install directly to connected device
./gradlew installDebug

# Or build APK only
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Launch after install
adb shell am start -n com.netspeed.monitor/.MainActivity
```

### Release Build

```bash
# Build release APK (unsigned, or signed if env vars set)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk

# With signing credentials
export KEYSTORE_FILE="/path/to/release.keystore"
export KEYSTORE_PASSWORD="your_password"
export KEY_ALIAS="release"
export KEY_PASSWORD="your_key_password"
./gradlew assembleRelease

# Install release APK
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.netspeed.monitor/.MainActivity
```

### Clean Build

```bash
./gradlew clean assembleDebug
./gradlew assembleRelease -x lint -x test
```

---

## 🔄 CI/CD

The project includes a GitHub Actions workflow ([`.github/workflows/release.yml`](.github/workflows/release.yml)) that:

1. **Triggers** on push to `main`, `beta`, or `alpha` branches
2. **Sets up** JDK 17 + Android SDK (no NDK required)
3. **Determines version** from `app/build.gradle.kts` `versionName`
4. **Builds** a release APK with R8 optimization
5. **Signs** using `KEYSTORE_BASE64` secret (or auto-generates a temporary keystore)
6. **Creates** a GitHub Release with auto-generated changelog
7. **Uploads** the APK as a release asset

### Release Channels

| Branch | Channel | Version Format | Example |
|---|---|---|---|
| `main` | Stable | `v1.0.0` | `v1.0.0` |
| `beta` | Beta | `v1.0.0-beta.N` | `v1.0.0-beta.3` |
| `alpha` | Alpha | `v1.0.0-alpha.N` | `v1.0.0-alpha.7` |

### Required Secrets (Optional)

| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Signing key alias |
| `KEY_PASSWORD` | Signing key password |

If no keystore secret is configured, the workflow auto-generates a temporary one for CI builds.

---

## 📦 APK Size Breakdown

The release APK is approximately **22 KB** thanks to:

- **Pure Java** — no Kotlin stdlib (~800 KB saved)
- **No AndroidX** — no AppCompat, Material, or Compose libraries (~5-15 MB saved)
- **No NDK** — no native `.so` libraries (~200 KB saved per ABI)
- **R8 optimization** — aggressive code shrinking and resource removal
- **Zero dependencies** — nothing to bundle

```
APK contents (~20 files):
├── AndroidManifest.xml
├── classes.dex          ← all Java code (~15 KB)
├── res/                 ← layouts, drawables, values
└── resources.arsc       ← compiled resources
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Keep the zero-dependency philosophy — avoid adding libraries
4. Test on a physical device (emulators may not report real traffic counters)
5. Submit a pull request with a clear description

---

## 📄 License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.

---

<div align="center">

Made with ❤️ · Pure Java · Android SDK · ~22 KB

</div>
