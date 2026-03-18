#include <jni.h>
#include "netspeed_calculator.h"
#include <android/log.h>
#include <memory>
#include <mutex>

// Tag for JNI-layer logcat messages
#define LOG_TAG "NetSpeedJNI"
// Debug logging macro
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Global singleton instance of the C++ speed calculator, lazily initialized
static std::unique_ptr<NetSpeedCalculator> g_calculator;
// Mutex to protect the global calculator from concurrent access by multiple threads
static std::mutex g_mutex;

// Helper: ensures the global calculator instance exists, creates it if needed
static NetSpeedCalculator* getCalculator() {
    // Lock the mutex to prevent race conditions during lazy initialization
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_calculator) {
        // Create the calculator on first use
        g_calculator = std::make_unique<NetSpeedCalculator>();
        LOGD("NetSpeedCalculator instance created");
    }
    return g_calculator.get();
}

// JNI function prefix matches the Kotlin class: com.netspeed.monitor.data.native_.NativeSpeedBridge
// The underscore in "native_" is encoded as "_1" in JNI naming convention
extern "C" {

// JNI method: calculates current speed and returns a double array [downloadSpeed, uploadSpeed]
JNIEXPORT jdoubleArray JNICALL
Java_com_netspeed_monitor_data_native_1_NativeSpeedBridge_nativeCalculateSpeed(
    JNIEnv* env,
    jobject /* this */
) {
    // Get or create the calculator singleton
    NetSpeedCalculator* calc = getCalculator();

    // Perform the speed calculation using /proc/net/dev readings
    SpeedResult result = calc->calculateSpeed();

    // Allocate a Java double array with 2 elements: [download, upload]
    jdoubleArray jResult = env->NewDoubleArray(2);
    if (jResult == nullptr) {
        // Return null if array allocation fails (out of memory)
        return nullptr;
    }

    // Populate the array with download speed at index 0 and upload speed at index 1
    jdouble speeds[2];
    speeds[0] = result.downloadSpeed;  // Bytes per second download
    speeds[1] = result.uploadSpeed;    // Bytes per second upload

    // Copy the native array into the Java array
    env->SetDoubleArrayRegion(jResult, 0, 2, speeds);

    return jResult;
}

// JNI method: reads total received and transmitted bytes, returns [rxBytes, txBytes]
JNIEXPORT jlongArray JNICALL
Java_com_netspeed_monitor_data_native_1_NativeSpeedBridge_nativeGetTotalBytes(
    JNIEnv* env,
    jobject /* this */
) {
    // Get or create the calculator singleton
    NetSpeedCalculator* calc = getCalculator();

    // Read current network statistics snapshot
    NetworkSnapshot snapshot = calc->readNetworkStats();

    // Allocate a Java long array with 2 elements: [rxBytes, txBytes]
    jlongArray jResult = env->NewLongArray(2);
    if (jResult == nullptr) {
        // Return null if allocation fails
        return nullptr;
    }

    // Fill with total received bytes and total transmitted bytes
    jlong bytes[2];
    bytes[0] = snapshot.rxBytes;  // Total received bytes
    bytes[1] = snapshot.txBytes;  // Total transmitted bytes

    // Copy native array into Java array
    env->SetLongArrayRegion(jResult, 0, 2, bytes);

    return jResult;
}

// JNI method: resets the calculator state, clearing any stored previous snapshot
JNIEXPORT void JNICALL
Java_com_netspeed_monitor_data_native_1_NativeSpeedBridge_nativeReset(
    JNIEnv* /* env */,
    jobject /* this */
) {
    // Reset the calculator to start fresh speed calculations
    getCalculator()->reset();
    LOGD("NetSpeedCalculator reset");
}

} // extern "C"
