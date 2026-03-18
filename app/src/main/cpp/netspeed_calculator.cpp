#include "netspeed_calculator.h"
#include <fstream>
#include <sstream>
#include <cstring>
#include <android/log.h>

// Tag used for Android logcat debug output
#define LOG_TAG "NetSpeedNative"
// Macro for debug-level logging via Android logcat
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Constructor: initializes calculator with no previous snapshot available
NetSpeedCalculator::NetSpeedCalculator()
    : previousSnapshot_{0, 0, 0}, hasPrevious_(false) {
}

// Returns the current monotonic clock time in nanoseconds for precise interval measurement
int64_t NetSpeedCalculator::getCurrentTimeNs() {
    // Use steady_clock for monotonic time that won't be affected by system time changes
    auto now = std::chrono::steady_clock::now();
    // Convert to nanoseconds since epoch for high-resolution timing
    return std::chrono::duration_cast<std::chrono::nanoseconds>(
        now.time_since_epoch()
    ).count();
}

// Parses one line from /proc/net/dev to extract received and transmitted byte counts
// Format: "interface: rxBytes rxPackets ... txBytes txPackets ..."
// Returns true if parsing succeeded, false otherwise
bool NetSpeedCalculator::parseProcNetDevLine(const std::string& line, int64_t& rxBytes, int64_t& txBytes) {
    // Find the colon separator between interface name and stats
    size_t colonPos = line.find(':');
    // Skip lines without a colon (header lines)
    if (colonPos == std::string::npos) {
        return false;
    }

    // Extract the interface name and trim leading whitespace
    std::string ifaceName = line.substr(0, colonPos);
    // Remove leading whitespace from interface name
    size_t start = ifaceName.find_first_not_of(" \t");
    if (start != std::string::npos) {
        ifaceName = ifaceName.substr(start);
    }

    // Skip the loopback interface as it's only local traffic
    if (ifaceName == "lo") {
        return false;
    }

    // Parse the statistics portion after the colon
    std::string stats = line.substr(colonPos + 1);
    std::istringstream iss(stats);

    // /proc/net/dev fields after interface name:
    // rx: bytes packets errs drop fifo frame compressed multicast
    // tx: bytes packets errs drop fifo colls carrier compressed
    int64_t rx = 0, tx = 0;
    int64_t dummy; // Placeholder for fields we don't need

    // Read rxBytes (first field)
    if (!(iss >> rx)) return false;
    // Skip 7 rx fields (packets, errs, drop, fifo, frame, compressed, multicast)
    for (int i = 0; i < 7; ++i) {
        if (!(iss >> dummy)) return false;
    }
    // Read txBytes (9th field = first tx field)
    if (!(iss >> tx)) return false;

    // Store parsed values in output parameters
    rxBytes = rx;
    txBytes = tx;
    return true;
}

// Reads /proc/net/dev and sums byte counts across all non-loopback network interfaces
NetworkSnapshot NetSpeedCalculator::readNetworkStats() {
    NetworkSnapshot snapshot = {0, 0, 0};

    // Open the kernel's network device statistics virtual file
    std::ifstream procFile("/proc/net/dev");
    if (!procFile.is_open()) {
        // Log error if we can't access network stats
        LOGD("Failed to open /proc/net/dev");
        // Record timestamp even on failure for consistent state
        snapshot.timestampNs = getCurrentTimeNs();
        return snapshot;
    }

    std::string line;
    // Iterate over each line in /proc/net/dev
    while (std::getline(procFile, line)) {
        int64_t rx = 0, tx = 0;
        // Try to parse this line; skip if it's a header or loopback
        if (parseProcNetDevLine(line, rx, tx)) {
            // Accumulate bytes from all valid interfaces (wlan0, rmnet0, etc.)
            snapshot.rxBytes += rx;
            snapshot.txBytes += tx;
        }
    }

    // Record the exact timestamp of this reading
    snapshot.timestampNs = getCurrentTimeNs();
    return snapshot;
}

// Calculates current network speed by comparing with the previous snapshot
SpeedResult NetSpeedCalculator::calculateSpeed() {
    // Take a fresh snapshot of current network byte counts
    NetworkSnapshot current = readNetworkStats();

    SpeedResult result = {0.0, 0.0, false};

    // Can only calculate speed if we have a previous snapshot to compare against
    if (!hasPrevious_) {
        // Store this as the first snapshot for next calculation
        previousSnapshot_ = current;
        hasPrevious_ = true;
        // Return zero speed on first call since we have no delta
        return result;
    }

    // Calculate time elapsed between snapshots in seconds (from nanoseconds)
    double elapsedSec = static_cast<double>(current.timestampNs - previousSnapshot_.timestampNs) / 1e9;

    // Guard against division by zero or negative time deltas
    if (elapsedSec <= 0.001) {
        // Time interval too small for meaningful calculation
        return result;
    }

    // Calculate byte deltas between current and previous readings
    int64_t rxDelta = current.rxBytes - previousSnapshot_.rxBytes;
    int64_t txDelta = current.txBytes - previousSnapshot_.txBytes;

    // Protect against counter wraps or resets (negative deltas)
    if (rxDelta < 0) rxDelta = 0;
    if (txDelta < 0) txDelta = 0;

    // Calculate speeds in bytes per second
    result.downloadSpeed = static_cast<double>(rxDelta) / elapsedSec;
    result.uploadSpeed = static_cast<double>(txDelta) / elapsedSec;
    result.valid = true;

    // Update stored snapshot for next speed calculation
    previousSnapshot_ = current;

    return result;
}

// Resets the calculator state, clearing the previous snapshot
void NetSpeedCalculator::reset() {
    // Zero out the stored snapshot
    previousSnapshot_ = {0, 0, 0};
    // Mark that we no longer have a valid previous reading
    hasPrevious_ = false;
}
