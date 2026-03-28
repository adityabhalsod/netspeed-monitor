<div align="center">

<img src="assets/bandwidth.png" alt="Net Speed Monitor" width="120" />

# ⚡ Net Speed Monitor

**Ultra-lightweight, always-on Android network speed monitor.**
Real-time upload & download speed in your status bar — updated every second.

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Language-Java%2017-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![APK Size](https://img.shields.io/badge/APK%20Size-~182%20KB-brightgreen?style=for-the-badge)](https://github.com/adityabhalsod/netspeed-monitor/releases)
[![Dependencies](https://img.shields.io/badge/Dependencies-Zero-blue?style=for-the-badge)](#-tech-stack)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![CI](https://img.shields.io/github/actions/workflow/status/adityabhalsod/netspeed-monitor/release.yml?branch=main&style=for-the-badge&label=CI&logo=github)](https://github.com/adityabhalsod/netspeed-monitor/actions)

**~182 KB release APK · Zero dependencies · Pure Android SDK · No ads · No tracking**

[Download APK](https://github.com/adityabhalsod/netspeed-monitor/releases/latest) · [Report Bug](https://github.com/adityabhalsod/netspeed-monitor/issues/new?template=bug_report.md) · [Request Feature](https://github.com/adityabhalsod/netspeed-monitor/issues/new?template=feature_request.md)

</div>

---

## 📖 Table of Contents

- [What Does This App Do?](#-what-does-this-app-do)
- [Features](#-features)
- [3-Tab Dashboard](#-3-tab-dashboard)
- [Architecture](#️-architecture)
- [How It Works](#-how-it-works)
- [Permissions](#-permissions)
- [Tech Stack](#️-tech-stack)
- [Requirements](#-requirements)
- [Getting Started](#-getting-started)
- [Build & Run](#-build--run)
- [CI/CD Pipeline](#-cicd-pipeline)
- [APK Size Breakdown](#-apk-size-breakdown)
- [Configuration Reference](#️-configuration-reference)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)
- [Acknowledgements](#-acknowledgements)

---

## 👋 What Does This App Do?

**Net Speed Monitor** is an ultra-lightweight Android app that **continuously shows how fast your internet is — right now**. It sits in your status bar and gives you real-time visibility into your network activity without draining your battery or bloating your phone with unnecessary libraries.

- 📥 **Download speed** — how fast data is arriving on your phone
- 📤 **Upload speed** — how fast data is leaving your phone
- 🔔 **Status bar notification** — always visible with a dynamic speed icon, even when the app is closed
- 📊 **Per-app data usage** — see which apps consume the most data (hourly, daily, weekly, monthly, yearly)
- 📅 **Monthly data report** — daily WiFi + mobile breakdown for the current month
- ⚙️ **Works in the background** — no need to keep the app open
- 🚀 **Start on boot** — monitoring begins the moment your phone turns on
- 🔄 **Live auto-refresh** — app usage data updates every 3 seconds

> **Privacy first:** This app reads only your device's own kernel byte counters. It does **not** access the internet, capture packet content, or collect any personal data. No ads, no analytics, no tracking — ever.

---

## ✨ Features

| Feature | Details |
|---|---|
| ⚡ **Real-time speed** | Polls every 100ms, UI updates at 10 FPS, notification updates every 500ms |
| 🔔 **Sticky notification** | Dynamic bitmap icon renders live speed value directly in the status bar |
| 🎨 **Custom gauge UI** | Hand-drawn arc gauges with animated needles using `Canvas` API |
| 📱 **3-tab dashboard** | Speed monitor, per-app usage, and monthly data report |
| 📊 **Per-app data tracking** | Uses `NetworkStatsManager` to show per-app download/upload breakdown |
| 📅 **Monthly reports** | Day-by-day WiFi vs mobile data consumption with monthly totals |
| 🔄 **Live refresh** | App usage tab auto-refreshes every 3 seconds without UI flicker |
| 🔋 **Battery friendly** | Reads kernel byte counters — no packet inspection, no wake locks |
| 📶 **All interfaces** | Tracks WiFi, mobile data, ethernet — all at once |
| 🔁 **Start on boot** | `BootReceiver` auto-restarts monitoring after device reboot |
| 🔐 **Runtime permissions** | Smart permission flow with warning banners and one-tap grant |
| 📦 **Tiny footprint** | ~182 KB release APK with R8 aggressive optimization |
| 🚫 **No dependencies** | Zero libraries — only Android SDK classes |
| 🎯 **Sliding window** | 1-second sliding window averages smooth out speed fluctuations |

---

## 📱 3-Tab Dashboard

The app features a **3-tab layout** accessible via a bottom tab bar:

### Tab 1: Speed Monitor
> Real-time network speed gauges with session statistics

- **Download gauge** — green arc with live speed (bytes/sec → adaptive unit)
- **Upload gauge** — orange arc with live speed
- **Status pill** — shows "Monitoring" or "Stopped" state
- **Session stats** — total data received/transmitted since monitoring started
- **Start/Stop toggle** — one-tap control with runtime permission handling
- **Permission banner** — amber warning with "Grant" button when notification permission is denied

### Tab 2: Per-App Usage
> Which apps are using your data — updated live every 3 seconds

- **Filter bar** — time period selector: 1 Hour, Today, Week, Month, Year
- **App list** — sorted by total usage (download + upload), shows app icon, name, and byte counts
- **Live auto-refresh** — silent background refresh every 3 seconds (no loading spinner flicker)
- **Usage Access prompt** — guides user to enable Usage Access in system settings if needed

### Tab 3: Data Report
> Monthly overview with daily WiFi vs mobile breakdown

- **Month navigation** — ◀ / ▶ arrows to browse previous months
- **Summary card** — total month usage, WiFi total, mobile total
- **Daily breakdown** — per-day rows showing WiFi bytes, mobile bytes, and combined total

---

## 🏗️ Architecture

This is a **pure Java** Android application with a flat, single-package structure. No Kotlin, no Compose, no AndroidX, no JNI/NDK — just Java 17 and the Android SDK.

### Project Structure

```
netspeed-monitor/
├── app/
│   ├── build.gradle.kts                    # App build config (zero dependencies)
│   ├── proguard-rules.pro                  # R8 shrinking rules (5 optimization passes)
│   └── src/main/
│       ├── AndroidManifest.xml             # Permissions & component declarations
│       ├── java/com/netspeed/monitor/
│       │   ├── NetSpeedApp.java            # Application class, notification channel setup
│       │   ├── MainActivity.java           # 3-tab dashboard with live refresh system
│       │   ├── SettingsActivity.java       # Start-on-boot toggle preference
│       │   ├── SpeedMonitorService.java    # Foreground service, 100ms polling timer
│       │   ├── BootReceiver.java           # BOOT_COMPLETED → auto-start service
│       │   ├── TrafficStatsCalculator.java # Sliding-window speed from kernel counters
│       │   ├── SpeedIconGenerator.java     # 130×130 dynamic bitmap for status bar icon
│       │   ├── SpeedGaugeView.java         # Custom arc gauge View (Canvas drawing)
│       │   ├── SpeedUtils.java             # Speed/bytes formatting (B/s → GB/s)
│       │   ├── AppUsageTracker.java        # Per-app network stats via NetworkStatsManager
│       │   └── DataUsageReport.java        # Monthly daily WiFi + mobile breakdown
│       └── res/
│           ├── layout/                     # 4 layouts: main, settings, app usage, data report
│           │   ├── activity_main.xml       # 3-tab dashboard with bottom tab bar
│           │   ├── activity_settings.xml   # Settings screen with toggle
│           │   ├── content_app_usage.xml   # Per-app usage tab content
│           │   └── content_data_report.xml # Monthly data report tab content
│           ├── drawable/                   # 18 vector icons + shape backgrounds
│           ├── mipmap-*/                   # Adaptive launcher icons (5 densities)
│           └── values/
│               ├── colors.xml              # Download green, upload orange, background
│               ├── strings.xml             # All user-visible strings
│               └── themes.xml              # Material Light NoActionBar theme
├── assets/
│   └── bandwidth.png                       # App icon source asset
├── build.gradle.kts                        # Root build config (AGP 8.3.0 only)
├── settings.gradle.kts                     # Plugin & dependency repositories
├── gradle.properties                       # Build perf flags (parallel, 2GB heap)
├── gradle/wrapper/
│   └── gradle-wrapper.properties           # Gradle 8.5 distribution
├── device.ini.example                      # Device config template (copy → device.ini)
├── setup.js                                # Node.js build & deploy automation script
├── .github/workflows/
│   └── release.yml                         # CI/CD: build, sign, release, changelog
├── DEBUG.md                                # Complete build & debug guide
├── COMMAND.md                              # ADB command quick reference
├── LICENSE                                 # MIT License
└── README.md                               # This file
```

### Component Overview

```
┌──────────────────────────────────────────────────────────────┐
│                        NetSpeedApp                           │
│            (Application · Notification channel)              │
└──────────┬──────────────────────────┬────────────────────────┘
           │                          │
           ▼                          ▼
┌────────────────────────┐   ┌──────────────────────────┐
│     MainActivity       │   │   SpeedMonitorService    │
│    (3-Tab Dashboard)   │   │   (Foreground Service)   │
│                        │   │                          │
│  ┌──────────────────┐  │   │   Timer (100ms tick)     │
│  │  SpeedGaugeView  │  │   │         │                │
│  │ (Canvas arc draw) │  │   │         ▼                │
│  └──────────────────┘  │   │   calculateSpeed()       │
│                        │◄──│         │                │
│  SpeedCallback         │   │         ▼                │
│  onSpeedUpdate()       │   │   Notification update    │
│                        │   │   (dynamic icon)         │
│  ┌──────────────────┐  │   └─────────┬────────────────┘
│  │ AppUsageTracker   │  │             │
│  │ (per-app stats)   │  │             │
│  └──────────────────┘  │   ┌─────────┼─────────────────────┐
│                        │   │         │                     │
│  ┌──────────────────┐  │   ▼         ▼                     ▼
│  │ DataUsageReport   │  │ TrafficStats  SpeedIcon       SpeedUtils
│  │ (monthly report)  │  │ Calculator   Generator       (Formatting)
│  └──────────────────┘  │
└────────────────────────┘
           │
           ▼
   ┌──────────────┐    ┌──────────────┐
   │ BootReceiver │    │ Settings     │
   │ (auto-start) │    │ Activity     │
   └──────────────┘    └──────────────┘
```

### Class Responsibilities

| Class | Lines | Responsibility |
|---|---|---|
| `NetSpeedApp` | ~30 | Creates notification channel with badge suppression |
| `MainActivity` | ~500 | 3-tab dashboard, permission handling, live refresh timer |
| `SettingsActivity` | ~50 | Start-on-boot toggle via `SharedPreferences` |
| `SpeedMonitorService` | ~200 | Foreground service, 100ms Timer, notification updates |
| `BootReceiver` | ~25 | Handles `BOOT_COMPLETED` → starts service |
| `TrafficStatsCalculator` | ~100 | Sliding-window speed calculation from `TrafficStats` API |
| `SpeedIconGenerator` | ~80 | Renders 130×130 `ALPHA_8` bitmap with speed text |
| `SpeedGaugeView` | ~200 | Custom `View` with arc track, filled arc, and text labels |
| `SpeedUtils` | ~40 | Format bytes/speed to human-readable strings (B/s → GB/s) |
| `AppUsageTracker` | ~150 | Queries `NetworkStatsManager` for per-app usage by UID |
| `DataUsageReport` | ~120 | Queries daily WiFi + mobile totals for a given month |

---

## 🔬 How It Works

### Speed Calculation (Sliding Window)

The app uses Android's `TrafficStats` API to read cumulative byte counters from the Linux kernel:

```
    ┌─────────────────────────────────────────────────────┐
    │              Kernel Byte Counters                   │
    │  /proc/net/xt_qtaguid/stats or TrafficStats API    │
    └────────────────────┬────────────────────────────────┘
                         │ polled every 100ms
                         ▼
    ┌─────────────────────────────────────────────────────┐
    │         TrafficStatsCalculator (Thread-safe)        │
    │                                                     │
    │  1. Read getTotalRxBytes() + getTotalTxBytes()      │
    │  2. Compare against 1-second window start           │
    │  3. If window elapsed (≥1s):                        │
    │     speed = ΔBytes / ΔTime (bytes/sec)              │
    │     Reset window start                              │
    │  4. Guard against negative deltas (counter resets)  │
    │  5. Return last computed speed between windows      │
    └────────────────────┬────────────────────────────────┘
                         │ SpeedCallback (main thread)
                         ▼
    ┌─────────────────────────────────────────────────────┐
    │  UI Update: Gauges + Session Stats + Notification   │
    └─────────────────────────────────────────────────────┘
```

**Why a sliding window?** Raw `TrafficStats` counters update roughly once per second on most devices. Polling every 100ms gives responsive UI, while the 1-second window ensures meaningful byte deltas for accurate speed readings.

### Dynamic Status Bar Icon

`SpeedIconGenerator` creates a 130×130 `Bitmap` (`ALPHA_8` format) each update with the current speed rendered as compact text:

- **Two-line layout:** Speed value on top (90px font), unit below (52px font)
- **Condensed bold typeface** with negative letter spacing for readability at small sizes
- **Reusable bitmap** — cleared and redrawn each call to avoid GC pressure
- Set as notification small icon via `Icon.createWithBitmap()` → live speed value directly in the status bar

### Foreground Service

`SpeedMonitorService` runs as an Android foreground service with `FOREGROUND_SERVICE_TYPE_DATA_SYNC`:

| Aspect | Detail |
|---|---|
| **Return type** | `START_STICKY` — system restarts the service if killed |
| **Polling rate** | 100ms `Timer` fires `calculateSpeed()` for responsive UI |
| **Notification throttle** | Updates every 5 ticks (500ms) to reduce system overhead |
| **UI callback** | `SpeedCallback.onSpeedUpdate()` delivers data to `MainActivity` on main thread |
| **Badge** | Suppressed via `setShowBadge(false)` on channel + `BADGE_ICON_NONE` on builder |
| **Stop action** | Notification includes a "Stop" button with `PendingIntent` |

### Boot Auto-Start

`BootReceiver` listens for `BOOT_COMPLETED` and `QUICKBOOT_POWERON`. If the user has enabled "Start on Boot" in settings (stored via `SharedPreferences`), the receiver starts the foreground service immediately after device boot.

### Per-App Usage Tracking

`AppUsageTracker` uses Android's `NetworkStatsManager` API:

1. Queries network stats for **all UIDs** over WiFi (`TYPE_WIFI`) and mobile (`TYPE_MOBILE`)
2. Groups bytes by UID → resolves package name → looks up app name and icon via `PackageManager`
3. Supports 5 time periods: **1 Hour**, **Today**, **Week**, **Month**, **Year**
4. Returns a sorted list (highest usage first) of `AppUsageInfo` objects
5. **Requires Usage Access permission** — the app guides users to the system settings page

### Monthly Data Report

`DataUsageReport` queries `NetworkStatsManager` day-by-day for a given month:

1. Iterates each day of the selected month
2. Queries WiFi and mobile buckets separately per day
3. Returns `DailyUsage` objects with date label, WiFi bytes, mobile bytes, and total
4. Supports month navigation (previous/next) for historical analysis

---

## 🔐 Permissions

| Permission | Purpose | Required? |
|---|---|---|
| `ACCESS_NETWORK_STATE` | Reads active network interface information | Yes |
| `FOREGROUND_SERVICE` | Required to run a persistent background service | Yes |
| `FOREGROUND_SERVICE_DATA_SYNC` | Declares service type on Android 14+ | Yes (API 34+) |
| `RECEIVE_BOOT_COMPLETED` | Restarts monitoring after device reboot | Yes |
| `POST_NOTIFICATIONS` | Shows live speed notification (Android 13+) | Runtime prompt |
| `PACKAGE_USAGE_STATS` | Per-app network usage data via `NetworkStatsManager` | Special access* |

> \* `PACKAGE_USAGE_STATS` is a special permission that requires the user to manually enable **Usage Access** in system settings. The app displays a prompt with a button to open the settings page.

### Permission Flow

```
App Launch
    │
    ├── Check POST_NOTIFICATIONS → if denied → show amber warning banner
    │                                          with "Grant" button
    │
    ├── User taps "Start Monitoring"
    │   └── if POST_NOTIFICATIONS not granted → request permission → on grant → start service
    │
    └── User taps "Apps" tab
        └── if PACKAGE_USAGE_STATS not granted → show "Enable Usage Access" prompt
```

### Privacy Policy

This app respects your privacy completely:

- ❌ **No internet access** — the app has no `INTERNET` permission
- ❌ **No data collection** — nothing is sent anywhere
- ❌ **No analytics or tracking** — no Firebase, no Crashlytics, nothing
- ❌ **No ads** — no ad SDKs bundled
- ✅ **Reads only kernel counters** — `/proc/net/xt_qtaguid/stats` via `TrafficStats`
- ✅ **All data stays on device** — stored only in `SharedPreferences`

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| **Language** | Java 17 (source compatibility: Java 11) |
| **UI Framework** | Android `View` system + Custom `Canvas` drawing |
| **Custom Views** | `SpeedGaugeView` — 270° arc gauge with `Paint`/`Canvas` |
| **Async** | `java.util.Timer` (100ms) + `android.os.Handler` (main thread) |
| **Background** | `ExecutorService` (single thread) for off-thread data queries |
| **Storage** | `SharedPreferences` for settings |
| **DI** | None — manual singleton pattern |
| **Build System** | Gradle 8.5 + AGP 8.3.0 |
| **Code Shrinking** | R8 with 5 optimization passes |
| **Min SDK** | API 26 (Android 8.0 Oreo) |
| **Target SDK** | API 34 (Android 14) |
| **Dependencies** | **Zero** — only `android.jar` from the Android SDK |
| **Release APK** | **~182 KB** |

### Why Zero Dependencies?

| What we avoided | Size saved | Alternative used |
|---|---|---|
| Kotlin stdlib | ~800 KB | Pure Java 17 |
| AndroidX AppCompat | ~1.5 MB | `android.app.Activity` directly |
| Material Components | ~2 MB | Custom XML drawables |
| Jetpack Compose | ~5-8 MB | `Canvas` API + custom `View` |
| Hilt / Dagger | ~500 KB | Manual singletons |
| Room / SQLite | ~500 KB | `SharedPreferences` |
| OkHttp / Retrofit | ~800 KB | Not needed (no network calls) |
| **Total saved** | **~11-14 MB** | **~182 KB APK** |

---

## 🔧 Requirements

### Development Environment

| Requirement | Minimum Version | Recommended |
|---|---|---|
| **Android Studio** | Hedgehog (2023.1.1) | Latest stable |
| **JDK** | 17 | Temurin 17 LTS |
| **Gradle** | 8.5 (via wrapper) | Included in repo |
| **Node.js** | 14+ | 18 LTS (for `setup.js`) |
| **ADB** | Included in Android SDK | Latest platform-tools |

### Target Device

| Requirement | Version |
|---|---|
| **Android** | 8.0+ (API 26+) |
| **Architecture** | Any (no native code) |
| **Storage** | < 1 MB installed |

> No NDK, CMake, or Kotlin plugin needed. The project builds with just the Android SDK + JDK 17.

---

## 🚀 Getting Started

### Prerequisites

1. **Install JDK 17** — [Adoptium Temurin](https://adoptium.net/)
2. **Install Android SDK** — via [Android Studio](https://developer.android.com/studio) or [command-line tools](https://developer.android.com/studio#command-line-tools-only)
3. **Install Node.js** (optional) — only needed for the `setup.js` helper script
4. **Enable Developer Options** on your Android device:
   - Go to **Settings → About Phone** → tap **Build Number** 7 times
   - Enable **USB Debugging** or **Wireless Debugging**

### Clone & Setup

```bash
# Clone the repository
git clone https://github.com/adityabhalsod/netspeed-monitor.git
cd netspeed-monitor

# Create your local device config
cp device.ini.example device.ini

# Edit device.ini with your values:
#   device.ip   → Your phone's IP address
#   device.port → Wireless debugging port
#   sdk.dir     → Path to your Android SDK
```

### One-Command Deploy

```bash
# Initialize + build + install + launch
node setup.js deploy
```

That's it! The app will compile, install on your connected device, and launch automatically.

---

## 🔨 Build & Run

### Using `setup.js` (Recommended)

The project includes a Node.js automation script that handles ADB connection, build, install, and launch:

| Command | What it does |
|---|---|
| `node setup.js init` | Reads `device.ini` → generates `local.properties` |
| `node setup.js build` | Compiles the debug APK via Gradle |
| `node setup.js run` | Connects ADB, installs APK, launches app |
| `node setup.js deploy` | **Full pipeline:** init → build → install → launch |

### Using Gradle Directly

```bash
# Make wrapper executable
chmod +x gradlew

# Debug build + install to connected device
./gradlew installDebug

# Debug APK only (no install)
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Launch after install
adb shell am start -n com.netspeed.monitor/.MainActivity
```

### Release Build

```bash
# Unsigned release APK
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk

# Signed release APK (provide environment variables)
export KEYSTORE_FILE="/path/to/release.keystore"
export KEYSTORE_PASSWORD="your_password"
export KEY_ALIAS="release"
export KEY_PASSWORD="your_key_password"
./gradlew assembleRelease

# Install release APK on device
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.netspeed.monitor/.MainActivity
```

### Clean Build

```bash
# Clean + rebuild debug
./gradlew clean assembleDebug

# Fast release (skip lint and tests)
./gradlew assembleRelease -x lint -x test
```

### Logcat Filtering

```bash
# View only Net Speed Monitor logs
adb logcat -s NetSpeedMonitor:V

# View all app process logs
adb logcat --pid=$(adb shell pidof com.netspeed.monitor)
```

> See **[DEBUG.md](DEBUG.md)** for the complete build, debug, and deploy guide with ADB setup instructions and troubleshooting.

---

## 🔄 CI/CD Pipeline

The project includes a fully automated GitHub Actions workflow at [`.github/workflows/release.yml`](.github/workflows/release.yml).

### Pipeline Steps

```
Push to main / beta / alpha
        │
        ▼
┌─ GitHub Actions ─────────────────────────────┐
│  1. Checkout (full history for changelog)    │
│  2. Setup JDK 17 (Temurin)                  │
│  3. Setup Android SDK (no NDK)              │
│  4. Cache Gradle dependencies               │
│  5. Determine version from build.gradle.kts │
│  6. Generate changelog from git commits     │
│  7. Decode/generate signing keystore        │
│  8. Build release APK (R8 optimized)        │
│  9. Create git tag                          │
│ 10. Create GitHub Release + upload APK      │
└──────────────────────────────────────────────┘
```

### Release Channels

| Branch | Channel | Version Format | Pre-release | Example |
|---|---|---|---|---|
| `main` | **Stable** | `v1.0.0` | No | `v1.0.0` |
| `beta` | **Beta** | `v1.0.0-beta.N` | Yes | `v1.0.0-beta.3` |
| `alpha` | **Alpha** | `v1.0.0-alpha.N` | Yes | `v1.0.0-alpha.7` |

### Auto-Generated Changelog

The workflow categorizes commits by conventional commit prefixes:

| Prefix | Section |
|---|---|
| `feat:` | ✨ Features |
| `fix:` | 🐛 Bug Fixes |
| `perf:` | ⚡ Performance |
| Other | 📦 Other Changes |

### Required Repository Secrets

| Secret | Purpose | Required? |
|---|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release keystore file | Optional |
| `KEYSTORE_PASSWORD` | Keystore password | Optional |
| `KEY_ALIAS` | Signing key alias | Optional |
| `KEY_PASSWORD` | Signing key password | Optional |

> If no keystore secrets are configured, the workflow **auto-generates a temporary keystore** so CI builds always produce an installable signed APK.

### Concurrency

The workflow uses `concurrency.group` per branch — pushing again while a release is building will cancel the in-progress run.

---

## 📦 APK Size Breakdown

The release APK is approximately **~182 KB** — smaller than most app icons.

### What's Inside

```
APK contents (~20 files):
├── AndroidManifest.xml          ← component declarations
├── classes.dex                  ← all 11 Java classes (~15 KB)
├── res/
│   ├── layout/                  ← 4 XML layout files
│   ├── drawable/                ← 18 vector drawables + shapes
│   └── mipmap-*/               ← adaptive launcher icons (5 densities)
├── resources.arsc               ← compiled resource table
└── META-INF/                    ← signing certificate
```

### Size Optimization Techniques

| Technique | Impact |
|---|---|
| **Pure Java** | No Kotlin stdlib ~800 KB |
| **No AndroidX** | No AppCompat/Material ~3.5 MB |
| **No Compose** | No Compose runtime ~5–8 MB |
| **No NDK** | No `.so` native libraries ~200 KB/ABI |
| **R8 + 5 passes** | Aggressive dead code elimination |
| **Resource shrinking** | Removes unused drawables and strings |
| `repackageclasses ''` | Flattens package hierarchy for better compression |
| **Non-transitive R** | Smaller R class, no dependency R fields |

---

## ⚙️ Configuration Reference

### `device.ini`

Machine-specific device configuration (not committed to git):

```ini
# Device IP for ADB wireless debugging
# Find: Settings → About Phone → IP Address
device.ip=192.168.1.6

# Wireless debugging port
# Find: Settings → Developer Options → Wireless Debugging → port number
device.port=44581

# Path to your Android SDK installation
# macOS: /Users/you/Library/Android/sdk
# Linux: /home/you/Android/Sdk
# Windows: C:\\Users\\you\\AppData\\Local\\Android\\Sdk
sdk.dir=/home/youruser/Android/Sdk
```

### `gradle.properties`

Build performance configuration:

| Property | Value | Purpose |
|---|---|---|
| `android.nonTransitiveRClass` | `true` | Smaller R classes, faster builds |
| `org.gradle.jvmargs` | `-Xmx2048m` | 2 GB heap for Gradle daemon |
| `org.gradle.parallel` | `true` | Parallel project execution |

### `proguard-rules.pro`

R8 code shrinking rules:

| Rule | Purpose |
|---|---|
| `-optimizationpasses 5` | Maximum optimization iterations |
| `-allowaccessmodification` | Enable access modification for better inlining |
| `-repackageclasses ''` | Flatten package structure for compression |
| `-keep SpeedGaugeView` | Referenced by XML layouts |
| `-keep SpeedCallback` | Interface used across classes |
| `-keep AppUsageInfo` | Data class accessed reflectively |
| `-keep DailyUsage` | Data class accessed reflectively |

---

## 🔍 Troubleshooting

### Common Issues

<details>
<summary><b>ADB not connecting wirelessly</b></summary>

1. Ensure device and computer are on the same WiFi network
2. On your Android device: **Settings → Developer Options → Wireless Debugging** → toggle ON
3. Note the IP and port displayed, update `device.ini` accordingly
4. Run `adb connect <ip>:<port>` manually to test

</details>

<details>
<summary><b>Speed shows 0 B/s even with active internet</b></summary>

- Some emulators don't report real `TrafficStats` counters — **use a physical device**
- Ensure WiFi or mobile data is active
- Check that the service is running: look for the notification icon in the status bar

</details>

<details>
<summary><b>Notification not showing on Android 13+</b></summary>

- The app requires `POST_NOTIFICATIONS` runtime permission on Android 13+
- Tap "Start Monitoring" — a permission dialog will appear
- If denied: tap the amber "Grant" banner → system notification settings → enable
- Alternatively: **Settings → Apps → Net Speed Monitor → Notifications → Allow**

</details>

<details>
<summary><b>Per-app usage shows "Enable Usage Access"</b></summary>

- The Apps tab requires special `PACKAGE_USAGE_STATS` permission
- Tap the "Enable" button → system Usage Access settings → toggle ON for Net Speed Monitor
- This is a one-time setup; the data will load immediately after granting

</details>

<details>
<summary><b>Build fails with "JDK not found"</b></summary>

- This project requires **JDK 17** (not 8, not 11, not 21)
- Install [Adoptium Temurin 17](https://adoptium.net/)
- Ensure `JAVA_HOME` points to JDK 17: `export JAVA_HOME=/path/to/jdk-17`

</details>

<details>
<summary><b>Gradle wrapper permission denied</b></summary>

```bash
chmod +x gradlew
```

</details>

---

## 🤝 Contributing

Contributions are welcome! Here's how to get started:

### Development Workflow

1. **Fork** the repository
2. **Clone** your fork: `git clone https://github.com/adityabhalsod/netspeed-monitor.git`
3. **Create** a feature branch: `git checkout -b feature/my-feature`
4. **Make** your changes following the guidelines below
5. **Test** on a physical device (emulators may not report real traffic counters)
6. **Commit** using [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `perf:`, etc.
7. **Push** and open a **Pull Request** with a clear description

### Guidelines

| Guideline | Detail |
|---|---|
| **Zero dependencies** | Do not add any third-party libraries — this is a core principle |
| **Pure Java** | No Kotlin, no Compose, no AndroidX |
| **Keep it small** | The APK should stay under 200 KB |
| **Physical device testing** | Emulators don't always report real `TrafficStats` data |
| **Conventional commits** | `feat:`, `fix:`, `docs:`, `refactor:`, `perf:`, `test:`, `chore:` |
| **One feature per PR** | Keep pull requests focused and reviewable |

### Ideas for Contribution

- 🌍 **Localization** — translate `strings.xml` to your language
- 📊 **Graph visualization** — speed history graph on the Speed tab
- 🎨 **Theming** — dark mode support
- 🔔 **Speed alerts** — notify when speed drops below a threshold
- 📱 **Widget** — home screen speed widget
- 🧪 **Unit tests** — test `TrafficStatsCalculator` and `SpeedUtils`

---

## 📄 License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.

```
MIT License

Copyright (c) 2026 Aditya

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software...
```

---

## 🙏 Acknowledgements

- **Android `TrafficStats` API** — for providing kernel-level byte counters without root access
- **Android `NetworkStatsManager`** — for per-app and per-day usage breakdowns
- **R8 Compiler** — for aggressive code shrinking that keeps the APK under 200 KB
- **GitHub Actions** — for free CI/CD with automated release publishing

---

<div align="center">

**[⬆ Back to Top](#-net-speed-monitor)**

Made with ❤️ by [Aditya](https://github.com/adityabhalsod) · Pure Java · Android SDK · ~182 KB

[![GitHub stars](https://img.shields.io/github/stars/adityabhalsod/netspeed-monitor?style=social)](https://github.com/adityabhalsod/netspeed-monitor/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/adityabhalsod/netspeed-monitor?style=social)](https://github.com/adityabhalsod/netspeed-monitor/network/members)

</div>
