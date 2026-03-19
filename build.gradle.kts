// Root project build script: only the Android Application plugin is needed (no Kotlin, Hilt, or KSP)
plugins {
    // Android Application plugin — the only required plugin for this pure-Java project
    id("com.android.application") version "8.3.0" apply false
}
