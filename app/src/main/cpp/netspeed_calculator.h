#ifndef NETSPEED_CALCULATOR_H
#define NETSPEED_CALCULATOR_H

#include <cstdint>
#include <string>
#include <chrono>

// Structure holding a snapshot of network traffic bytes at a point in time
struct NetworkSnapshot {
    int64_t rxBytes;    // Total bytes received across all interfaces
    int64_t txBytes;    // Total bytes transmitted across all interfaces
    int64_t timestampNs; // Timestamp in nanoseconds when snapshot was taken
};

// Structure holding calculated upload and download speeds in bytes per second
struct SpeedResult {
    double downloadSpeed; // Download speed in bytes per second
    double uploadSpeed;   // Upload speed in bytes per second
    bool valid;           // Whether the calculation produced valid results
};

// High-performance network speed calculator that reads /proc/net/dev directly
class NetSpeedCalculator {
public:
    // Constructor: initializes the calculator with a zeroed-out previous snapshot
    NetSpeedCalculator();

    // Reads current network byte counts from /proc/net/dev and returns a snapshot
    NetworkSnapshot readNetworkStats();

    // Calculates speed in bytes/sec by comparing current snapshot with the previous one
    SpeedResult calculateSpeed();

    // Resets the stored previous snapshot to zero
    void reset();

private:
    // Stores the last network snapshot for delta calculation
    NetworkSnapshot previousSnapshot_;

    // Flag indicating whether we have a valid previous snapshot
    bool hasPrevious_;

    // Returns current monotonic timestamp in nanoseconds for accurate interval measurement
    static int64_t getCurrentTimeNs();

    // Parses a single line from /proc/net/dev to extract rx and tx bytes for one interface
    static bool parseProcNetDevLine(const std::string& line, int64_t& rxBytes, int64_t& txBytes);
};

#endif // NETSPEED_CALCULATOR_H
