#!/usr/bin/env node
/**
 * setup.js — build, run, and project setup helper for Net Speed Monitor
 *
 * Usage:
 *   node setup.js init       → generate local.properties from device.ini
 *   node setup.js build      → assemble debug APK
 *   node setup.js run        → install + launch on connected device
 *   node setup.js deploy     → build + install + launch (full pipeline)
 *   node setup.js uninstall  → uninstall app from device
 *
 * Configuration is read from device.ini (see device.ini.example).
 */

const { execSync, spawnSync } = require("child_process");
const fs = require("fs");
const path = require("path");
const os = require("os");

// ─── paths ───────────────────────────────────────────────────────────────────

const PROJECT_ROOT = __dirname;
const DEVICE_INI = path.join(PROJECT_ROOT, "device.ini");
const DEVICE_INI_EXAMPLE = path.join(PROJECT_ROOT, "device.ini.example");
const LOCAL_PROPERTIES = path.join(PROJECT_ROOT, "local.properties");

// ─── static config ───────────────────────────────────────────────────────────

// Package name (applicationId in build.gradle.kts)
const APP_PACKAGE = "com.netspeed.monitor";
// Main activity to launch after install
const MAIN_ACTIVITY = `${APP_PACKAGE}/.MainActivity`;
// Path to the built debug APK relative to project root
const APK_PATH = "app/build/outputs/apk/debug/app-debug.apk";
// Gradle wrapper command (platform-aware)
const GRADLEW = os.platform() === "win32" ? "gradlew.bat" : "./gradlew";

// ─── device.ini parser ──────────────────────────────────────────────────────

/**
 * Parses a simple key=value INI file, ignoring comments (#) and blank lines.
 * Returns an object with trimmed keys and values.
 */
function parseIni(filePath) {
  const content = fs.readFileSync(filePath, "utf-8");
  const config = {};
  for (const line of content.split("\n")) {
    const trimmed = line.trim();
    // Skip empty lines and comments
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eqIndex = trimmed.indexOf("=");
    if (eqIndex === -1) continue;
    const key = trimmed.substring(0, eqIndex).trim();
    const value = trimmed.substring(eqIndex + 1).trim();
    config[key] = value;
  }
  return config;
}

/**
 * Reads device.ini and returns the parsed config.
 * Exits with helpful message if device.ini is missing.
 */
function loadDeviceConfig() {
  if (!fs.existsSync(DEVICE_INI)) {
    console.error(
      "\n\x1b[31m✖  device.ini not found.\x1b[0m\n\n" +
        "  Create it from the example:\n\n" +
        "    \x1b[36mcp device.ini.example device.ini\x1b[0m\n\n" +
        "  Then edit device.ini with your device IP, port, and SDK path.\n"
    );
    process.exit(1);
  }

  const config = parseIni(DEVICE_INI);

  // Validate required keys
  const required = ["device.ip", "device.port", "sdk.dir"];
  const missing = required.filter((k) => !config[k]);
  if (missing.length > 0) {
    console.error(
      `\n\x1b[31m✖  Missing keys in device.ini: ${missing.join(", ")}\x1b[0m\n` +
        "  See device.ini.example for the expected format.\n"
    );
    process.exit(1);
  }

  return config;
}

/**
 * Returns the ADB device serial string from device.ini (ip:port).
 */
function getDeviceSerial(config) {
  return `${config["device.ip"]}:${config["device.port"]}`;
}

// ─── helpers ─────────────────────────────────────────────────────────────────

/**
 * Run a shell command, streaming output live to the console.
 * Throws on non-zero exit so the pipeline stops cleanly.
 */
function run(cmd, label) {
  console.log(`\n\x1b[36m▶  ${label}\x1b[0m`);
  console.log(`   \x1b[90m${cmd}\x1b[0m\n`);
  const result = spawnSync(cmd, { shell: true, stdio: "inherit" });
  if (result.status !== 0) {
    console.error(`\n\x1b[31m✖  "${label}" failed (exit ${result.status})\x1b[0m`);
    process.exit(result.status ?? 1);
  }
  console.log(`\x1b[32m✔  ${label}\x1b[0m`);
}

/**
 * Connects to the ADB device and verifies it's online.
 * Returns the device serial string.
 */
function connectDevice(config) {
  const serial = getDeviceSerial(config);

  // Attempt wireless ADB connection
  console.log(`\n\x1b[36m▶  Connecting to device ${serial}…\x1b[0m`);
  try {
    execSync(`adb connect ${serial}`, { stdio: "pipe" });
  } catch (_) {
    /* may already be connected */
  }

  // Verify the device is reachable
  const output = execSync("adb devices -l 2>&1").toString();
  const lines = output.split("\n").filter(
    (l) => l.length && !l.startsWith("List of") && l.includes("device ")
  );

  const deviceOnline = lines.some((l) => l.startsWith(serial));
  if (!deviceOnline) {
    // Try fallback to first available device
    if (lines.length > 0) {
      const fallback = lines[0].split(/\s+/)[0];
      console.log(
        `\x1b[33m⚠  Device ${serial} not found — using ${fallback}\x1b[0m`
      );
      return fallback;
    }
    console.error(
      `\n\x1b[31m✖  No ADB device found.\x1b[0m\n` +
        "   Make sure:\n" +
        "   1. Developer Mode + Wireless Debugging is ON on your phone\n" +
        `   2. device.ini has the correct IP and port\n` +
        `   3. Your phone and computer are on the same Wi-Fi network\n`
    );
    process.exit(1);
  }

  console.log(`\x1b[32m✔  Connected to ${serial}\x1b[0m`);
  return serial;
}

// ─── commands ────────────────────────────────────────────────────────────────

/**
 * Generate local.properties from device.ini config.
 */
function init() {
  const config = loadDeviceConfig();
  const sdkDir = config["sdk.dir"];
  const serial = getDeviceSerial(config);

  // Build local.properties content
  const content =
    "## Auto-generated by setup.js from device.ini — do not edit manually.\n" +
    "## Run: node setup.js init\n" +
    "\n" +
    "# Path to the Android SDK installation\n" +
    `sdk.dir=${sdkDir}\n`;

  fs.writeFileSync(LOCAL_PROPERTIES, content, "utf-8");
  console.log(`\x1b[32m✔  Generated local.properties\x1b[0m`);
  console.log(`   sdk.dir   = ${sdkDir}`);
  console.log(`   device    = ${serial}`);
  console.log(
    `\n   \x1b[90mYou can now run: \x1b[36mnode setup.js deploy\x1b[0m\n`
  );
}

/**
 * Assemble the debug APK using Gradle.
 */
function build() {
  // Ensure local.properties exists
  ensureLocalProperties();
  run(`${GRADLEW} assembleDebug`, "Gradle assembleDebug");
  console.log(`\n\x1b[32m✔  APK ready at: ${APK_PATH}\x1b[0m`);
}

/**
 * Install the pre-built APK onto the device and launch the app.
 */
function runOnDevice() {
  const config = loadDeviceConfig();
  const serial = connectDevice(config);
  const adb = `adb -s ${serial}`;

  run(`${adb} install -r ${APK_PATH}`, "Install APK");
  run(`${adb} shell am start -n ${MAIN_ACTIVITY}`, "Launch Net Speed Monitor");
  console.log(`\n\x1b[32m🚀  App running on device ${serial}\x1b[0m\n`);
}

/**
 * Uninstall the app from the connected device.
 */
function uninstall() {
  const config = loadDeviceConfig();
  const serial = connectDevice(config);
  const adb = `adb -s ${serial}`;

  run(`${adb} uninstall ${APP_PACKAGE}`, `Uninstall ${APP_PACKAGE}`);
  console.log(`\n\x1b[32m✔  App uninstalled from device ${serial}\x1b[0m\n`);
}

/**
 * Full pipeline: init → build → install → launch.
 */
function deploy() {
  ensureLocalProperties();
  build();
  runOnDevice();
}

/**
 * Auto-generates local.properties if it doesn't exist yet.
 */
function ensureLocalProperties() {
  if (!fs.existsSync(LOCAL_PROPERTIES)) {
    console.log(
      "\x1b[33m⚠  local.properties not found — generating from device.ini…\x1b[0m"
    );
    init();
  }
}

// ─── entry point ─────────────────────────────────────────────────────────────

const command = process.argv[2];
const commands = { init, build, run: runOnDevice, deploy, uninstall };

if (!command || !commands[command]) {
  console.log(
    "\n\x1b[1mNet Speed Monitor — setup helper\x1b[0m\n\n" +
      "  \x1b[36mnode setup.js init\x1b[0m        Generate local.properties from device.ini\n" +
      "  \x1b[36mnode setup.js build\x1b[0m       Compile & produce debug APK\n" +
      "  \x1b[36mnode setup.js run\x1b[0m         Install APK on device + launch app\n" +
      "  \x1b[36mnode setup.js deploy\x1b[0m      Build + install + launch (full pipeline)\n" +
      "  \x1b[36mnode setup.js uninstall\x1b[0m   Uninstall app from device\n"
  );
  process.exit(0);
}

commands[command]();
