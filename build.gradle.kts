// Root project build script: applies top-level plugins without applying them to the root project itself
plugins {
    // Android Application plugin, versioned but not applied at root level
    id("com.android.application") version "8.3.0" apply false
    // Kotlin Android plugin for Kotlin support
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    // Kotlin Compose compiler plugin for Jetpack Compose support
    id("org.jetbrains.kotlin.plugin.compose") version "1.9.22" apply false
    // Hilt Android plugin for dependency injection
    id("com.google.dagger.hilt.android") version "2.51" apply false
    // Kotlin Symbol Processing (KSP) for annotation processing
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
