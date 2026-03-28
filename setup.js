#!/usr/bin/env node
/**
 * setup.js — build, run, and project setup helper for Net Speed Monitor
 *
 * Usage:
 *   node setup.js adb-setup  → pair + connect wireless ADB, save to device.ini
 *   node setup.js init       → generate local.properties from ANDROID_HOME
 *   node setup.js build      → assemble debug APK via Gradle
 *   node setup.js install    → install pre-built APK onto connected device
 *   node setup.js run        → install APK + launch app on device
 *   node setup.js deploy     → init + build + install + launch (full pipeline)
 *   node setup.js uninstall  → uninstall app from device
 *
 * Android SDK is resolved from the ANDROID_HOME environment variable.
 * ADB device is resolved from device.ini (written by adb-setup) or auto-detected.
 */

const { execSync, spawnSync } = require("child_process");
const fs = require("fs");
const path = require("path");
const os = require("os");
// Built-in readline used for interactive adb-setup prompts — no extra deps
const readline = require("readline");

// ─── paths ───────────────────────────────────────────────────────────────────

// Root of this project
const PROJECT_ROOT = __dirname;
// local.properties tells Gradle where the Android SDK lives
const LOCAL_PROPERTIES = path.join(PROJECT_ROOT, "local.properties");
// device.ini stores the wireless ADB device IP and port (written by adb-setup)
const DEVICE_INI = path.join(PROJECT_ROOT, "device.ini");

// ─── static config ───────────────────────────────────────────────────────────

// Package name (applicationId in build.gradle.kts)
const APP_PACKAGE = "com.netspeed.monitor";
// Main activity to launch after install
const MAIN_ACTIVITY = `${APP_PACKAGE}/.MainActivity`;
// Path to the built debug APK relative to project root
const APK_PATH = "app/build/outputs/apk/debug/app-debug.apk";
// Gradle wrapper command (platform-aware, executed from project root)
const GRADLEW = os.platform() === "win32" ? "gradlew.bat" : "./gradlew";

// ─── helpers ─────────────────────────────────────────────────────────────────

/**
 * Ask the user a question and return their answer as a trimmed string.
 * Opens a fresh readline interface per call so prompts work in sequence.
 */
function prompt(question) {
  // Create a one-shot readline interface reading from stdin
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  return new Promise((resolve) => {
    rl.question(question, (answer) => {
      // Close immediately so the next prompt can open its own interface
      rl.close();
      resolve(answer.trim());
    });
  });
}

/**
 * Resolve the Android SDK path from ANDROID_HOME or common default locations.
 * Exits with a helpful message if no SDK is found.
 */
function resolveAndroidSdk() {
  // Prefer the standard ANDROID_HOME environment variable
  const fromEnv = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT;
  if (fromEnv && fs.existsSync(fromEnv)) {
    return fromEnv;
  }

  // Fallback: check common default SDK locations per platform
  const home = os.homedir();
  const defaults =
    os.platform() === "darwin"
      ? [path.join(home, "Library", "Android", "sdk")]
      : os.platform() === "win32"
        ? [path.join(home, "AppData", "Local", "Android", "Sdk")]
        : [path.join(home, "Android", "Sdk")];

  // Return the first path that exists on disk
  for (const dir of defaults) {
    if (fs.existsSync(dir)) return dir;
  }

  // No SDK found anywhere — tell the user how to fix it
  console.error(
    "\n\x1b[31m✖  Android SDK not found.\x1b[0m\n\n" +
      "  Set the ANDROID_HOME environment variable:\n\n" +
      "    \x1b[36mexport ANDROID_HOME=$HOME/Android/Sdk\x1b[0m\n\n" +
      "  Add it to ~/.bashrc or ~/.zshrc to make it permanent.\n"
  );
  process.exit(1);
}

/**
 * Run a shell command, streaming output live to the console.
 * Optionally accepts a cwd to execute from a specific directory.
 * Exits on non-zero exit code so the pipeline stops cleanly.
 */
function run(cmd, label, cwd = PROJECT_ROOT) {
  console.log(`\n\x1b[36m▶  ${label}\x1b[0m`);
  console.log(`   \x1b[90m${cmd}\x1b[0m\n`);
  // Spawn with shell so Gradle wrapper and other shell commands resolve correctly
  const result = spawnSync(cmd, { shell: true, stdio: "inherit", cwd });
  if (result.status !== 0) {
    console.error(`\n\x1b[31m✖  "${label}" failed (exit ${result.status})\x1b[0m`);
    process.exit(result.status ?? 1);
  }
  console.log(`\x1b[32m✔  ${label}\x1b[0m`);
}

/**
 * Detect the connected ADB device to use.
 * Priority: device.ini (set by adb-setup) → first auto-detected device.
 * Returns the serial string (e.g. "192.168.1.2:38613" or "emulator-5554").
 */
function getConnectedDevice() {
  // If device.ini exists, attempt to reconnect using the saved IP:port
  if (fs.existsSync(DEVICE_INI)) {
    const saved = parseSavedDevice();
    if (saved) {
      const serial = `${saved.ip}:${saved.port}`;
      console.log(`\x1b[36m▶  Reconnecting to saved device ${serial}…\x1b[0m`);
      try {
        // Attempt adb connect in case the wireless session dropped
        execSync(`adb connect ${serial}`, { stdio: "pipe" });
      } catch (_) {
        /* already connected or unreachable — verified below */
      }
      // Confirm the device is now listed as online
      const check = execSync("adb devices 2>&1").toString();
      if (check.includes(serial)) {
        console.log(`\x1b[32m✔  Using device: ${serial}\x1b[0m`);
        return serial;
      }
      console.log(
        `\x1b[33m⚠  Saved device ${serial} not reachable — falling back to auto-detect.\x1b[0m`
      );
    }
  }

  // Fall back to the first device reported by adb devices
  let output;
  try {
    // List all ADB devices with details
    output = execSync("adb devices -l 2>&1").toString();
  } catch (_) {
    console.error(
      "\n\x1b[31m✖  ADB not found.\x1b[0m\n" +
        "   Make sure Android SDK platform-tools are in your PATH.\n"
    );
    process.exit(1);
  }

  // Filter to lines that represent an online device
  const lines = output
    .split("\n")
    .filter((l) => l.length && !l.startsWith("List of") && l.includes("device "));

  if (lines.length === 0) {
    console.error(
      "\n\x1b[31m✖  No ADB device found.\x1b[0m\n" +
        "   Make sure:\n" +
        "   1. A device is connected via USB, or an emulator is running\n" +
        "   2. USB debugging is enabled in Developer Options\n" +
        "   3. You authorized the computer on the device\n" +
        "   Tip: run \x1b[36mnode setup.js adb-setup\x1b[0m for wireless pairing.\n"
    );
    process.exit(1);
  }

  // Use the first available device serial
  const serial = lines[0].split(/\s+/)[0];
  console.log(`\x1b[32m✔  Using device: ${serial}\x1b[0m`);
  return serial;
}

/**
 * Parse the saved device.ini file.
 * Returns { ip, port } or null if the file is missing/malformed.
 */
function parseSavedDevice() {
  try {
    const content = fs.readFileSync(DEVICE_INI, "utf-8");
    // Extract device.ip and device.port values from key=value lines
    const ip   = (content.match(/^device\.ip=(.+)$/m)   || [])[1]?.trim();
    const port = (content.match(/^device\.port=(.+)$/m) || [])[1]?.trim();
    // Only return a result when both fields are populated
    return ip && port ? { ip, port } : null;
  } catch (_) {
    return null;
  }
}

// ─── commands ────────────────────────────────────────────────────────────────

/**
 * Interactive wizard to pair and connect a wireless ADB device.
 * Prompts for pairing IP/port/code, then connect IP/port, then saves to device.ini.
 */
async function adbSetup() {
  console.log(
    "\n\x1b[1mWireless ADB Setup\x1b[0m\n" +
      "On your phone: \x1b[90mSettings → Developer Options → Wireless Debugging\x1b[0m\n"
  );

  // ── Step 1: Pair ──────────────────────────────────────────────────────────
  console.log("\x1b[1mStep 1: Pair\x1b[0m");
  console.log(
    "Tap \x1b[33m'Pair device with pairing code'\x1b[0m in Wireless Debugging.\n"
  );

  // Collect pairing credentials from the user
  const pairIp   = await prompt("  Pairing IP address  : ");
  const pairPort = await prompt("  Pairing port        : ");
  const pairCode = await prompt("  Pairing code        : ");

  console.log(
    `\n\x1b[36m▶  adb pair ${pairIp}:${pairPort} ${pairCode}\x1b[0m\n`
  );
  try {
    // Pass the pairing code as a CLI argument (adb pair supports this directly)
    execSync(`adb pair ${pairIp}:${pairPort} ${pairCode}`, { stdio: "inherit" });
  } catch (_) {
    // adb pair may exit non-zero on older ADB versions even on success;
    // the device list check in Step 2 will confirm the outcome
  }
  console.log("\x1b[32m✔  Pairing step done\x1b[0m");

  // ── Step 2: Connect ───────────────────────────────────────────────────────
  console.log(
    "\n\x1b[1mStep 2: Connect\x1b[0m\n" +
      "Use the IP and port shown under \x1b[33m'Wireless Debugging'\x1b[0m (not the pairing port).\n"
  );

  // The connect IP/port can differ from the pairing IP/port
  const connectIp   = await prompt("  Connect IP address  : ");
  const connectPort = await prompt("  Connect port        : ");

  console.log(
    `\n\x1b[36m▶  adb connect ${connectIp}:${connectPort}\x1b[0m\n`
  );
  try {
    execSync(`adb connect ${connectIp}:${connectPort}`, { stdio: "inherit" });
  } catch (err) {
    console.error(
      `\n\x1b[31m✖  adb connect failed: ${err.message}\x1b[0m\n` +
        "   Check that your phone is on the same Wi-Fi network.\n"
    );
    process.exit(1);
  }
  console.log("\x1b[32m✔  Connected\x1b[0m");

  // ── Step 3: Save to device.ini ────────────────────────────────────────────
  // Write a minimal INI with only the connect address (used for future reconnects)
  const iniContent =
    "# ─── ADB Wireless Device ────────────────────────────────────────────────\n" +
    "# Auto-generated by: node setup.js adb-setup\n" +
    "# DO NOT commit this file — it is git-ignored.\n" +
    "# ──────────────────────────────────────────────────────────────────────\n" +
    "\n" +
    "# Device IP address for ADB wireless debugging\n" +
    `device.ip=${connectIp}\n` +
    "\n" +
    "# ADB wireless debugging port (shown in Settings → Wireless Debugging)\n" +
    `device.port=${connectPort}\n`;

  fs.writeFileSync(DEVICE_INI, iniContent, "utf-8");
  console.log(`\n\x1b[32m✔  Saved connection to device.ini\x1b[0m`);
  console.log(`   device.ip   = ${connectIp}`);
  console.log(`   device.port = ${connectPort}`);
  console.log(
    `\n   \x1b[90mRun: \x1b[36mnode setup.js deploy\x1b[0m\n`
  );
}

/**
 * Generate local.properties from the detected Android SDK path.
 */
function init() {
  // Auto-detect SDK from ANDROID_HOME or default locations
  const sdkDir = resolveAndroidSdk();

  // Write local.properties with the sdk.dir path for Gradle
  const content =
    "## Auto-generated by setup.js — do not edit manually.\n" +
    "## Run: node setup.js init\n" +
    "\n" +
    "# Path to the Android SDK installation\n" +
    `sdk.dir=${sdkDir}\n`;

  fs.writeFileSync(LOCAL_PROPERTIES, content, "utf-8");
  console.log(`\x1b[32m✔  Generated local.properties\x1b[0m`);
  console.log(`   sdk.dir = ${sdkDir}`);
  console.log(
    `\n   \x1b[90mYou can now run: \x1b[36mnode setup.js deploy\x1b[0m\n`
  );
}

/**
 * Assemble the debug APK using Gradle.
 */
function build() {
  // Ensure local.properties exists before Gradle needs it
  ensureLocalProperties();
  // Run Gradle assembleDebug from the project root
  run(`${GRADLEW} assembleDebug`, "Gradle assembleDebug");
  console.log(`\n\x1b[32m✔  APK ready at: ${APK_PATH}\x1b[0m`);
}

/**
 * Install the pre-built APK onto the connected device (no launch).
 */
function install() {
  // Auto-detect the first connected ADB device
  const serial = getConnectedDevice();
  const adb = `adb -s ${serial}`;

  // Install the debug APK, replacing any existing version on device
  run(`${adb} install -r ${APK_PATH}`, "Install APK");
  console.log(`\n\x1b[32m✔  APK installed on ${serial}\x1b[0m\n`);
}

/**
 * Install the pre-built APK and launch the app on the device.
 */
function runOnDevice() {
  // Auto-detect the first connected ADB device
  const serial = getConnectedDevice();
  const adb = `adb -s ${serial}`;

  // Install APK then start the main activity
  run(`${adb} install -r ${APK_PATH}`, "Install APK");
  run(`${adb} shell am start -n ${MAIN_ACTIVITY}`, "Launch Net Speed Monitor");
  console.log(`\n\x1b[32m🚀  App running on ${serial}\x1b[0m\n`);
}

/**
 * Uninstall the app from the connected device.
 */
function uninstall() {
  // Auto-detect device and remove the package
  const serial = getConnectedDevice();
  const adb = `adb -s ${serial}`;

  run(`${adb} uninstall ${APP_PACKAGE}`, `Uninstall ${APP_PACKAGE}`);
  console.log(`\n\x1b[32m✔  App uninstalled from ${serial}\x1b[0m\n`);
}

/**
 * Full pipeline: init → build → install → launch.
 */
function deploy() {
  // Ensure local.properties is present, then build and run
  ensureLocalProperties();
  build();
  runOnDevice();
}

/**
 * Auto-generates local.properties if it doesn't already exist.
 */
function ensureLocalProperties() {
  if (!fs.existsSync(LOCAL_PROPERTIES)) {
    // Warn and auto-generate when missing
    console.log(
      "\x1b[33m⚠  local.properties not found — generating from ANDROID_HOME…\x1b[0m"
    );
    init();
  }
}

// ─── entry point ─────────────────────────────────────────────────────────────

// Parse the subcommand from CLI arguments
const command = process.argv[2];
// Map of available commands to their handler functions
// adbSetup is async; all others are sync — handled uniformly with Promise.resolve
const commands = { "adb-setup": adbSetup, init, build, install, run: runOnDevice, deploy, uninstall };

if (!command || !commands[command]) {
  console.log(
    "\n\x1b[1mNet Speed Monitor — setup helper\x1b[0m\n\n" +
      "  \x1b[36mnode setup.js adb-setup\x1b[0m   Pair + connect wireless ADB, save to device.ini\n" +
      "  \x1b[36mnode setup.js init\x1b[0m        Generate local.properties from ANDROID_HOME\n" +
      "  \x1b[36mnode setup.js build\x1b[0m       Compile & produce debug APK\n" +
      "  \x1b[36mnode setup.js install\x1b[0m     Install pre-built APK on device\n" +
      "  \x1b[36mnode setup.js run\x1b[0m         Install APK on device + launch app\n" +
      "  \x1b[36mnode setup.js deploy\x1b[0m      Build + install + launch (full pipeline)\n" +
      "  \x1b[36mnode setup.js uninstall\x1b[0m   Uninstall app from device\n"
  );
  process.exit(0);
}

// Execute the requested command; wrap in Promise.resolve so async commands
// (adb-setup) and sync commands are handled the same way
Promise.resolve(commands[command]()).catch((err) => {
  console.error(`\n\x1b[31m✖  ${err.message}\x1b[0m`);
  process.exit(1);
});
