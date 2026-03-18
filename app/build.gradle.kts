// App-level build configuration with Kotlin, Compose, C++ NDK, and Hilt support
plugins {
    // Apply Android application plugin for this module
    id("com.android.application")
    // Kotlin Android plugin for Kotlin compilation
    id("org.jetbrains.kotlin.android")
    // Hilt dependency injection plugin
    id("com.google.dagger.hilt.android")
    // KSP for annotation processing (used by Hilt)
    id("com.google.devtools.ksp")
}

android {
    // Target namespace for generated R class and BuildConfig
    namespace = "com.netspeed.monitor"
    // Compile against Android API 34
    compileSdk = 34
    // Pin to the NDK version installed at ~/Android/Sdk/ndk/27.1.12297006
    ndkVersion = "27.1.12297006"

    defaultConfig {
        // Unique application identifier
        applicationId = "com.netspeed.monitor"
        // Minimum supported Android version (API 26 = Android 8.0 for notification channels)
        minSdk = 26
        // Target SDK for latest behavior and optimizations
        targetSdk = 34
        // Application version code for updates
        versionCode = 1
        // Human-readable version name
        versionName = "1.0.0"

        // Instrumented test runner
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Vector drawable backward compatibility support
        vectorDrawables {
            useSupportLibrary = true
        }

        // NDK: Samsung device is arm64-v8a; include armeabi-v7a for older Samsung variants.
        // x86/x86_64 are emulator-only ABIs — excluded here to keep build times fast.
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // CMake arguments for native C++ build
        externalNativeBuild {
            cmake {
                // C++ standard version and optimization flags
                cppFlags += "-std=c++17 -O2"
                // Build arguments for parallel compilation
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_FILE")
            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: "release"
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        // Release build type: enable minification and shrinking
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig?.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
        }
        // Debug build type: keep debugging features enabled
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    // Java 17 compatibility for both source and target
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Kotlin JVM target version
    kotlinOptions {
        jvmTarget = "17"
    }

    // Enable Jetpack Compose build features
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Compose compiler extension version compatible with Kotlin 1.9.22
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    // Point to CMakeLists.txt for native C++ build configuration
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Packaging options to avoid duplicate file conflicts
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Dependency declarations
dependencies {
    // Compose BOM: aligns all Compose library versions
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    // Core AndroidX libraries
    implementation("androidx.core:core-ktx:1.12.0")             // Kotlin extensions for Android core
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Lifecycle-aware coroutine scopes
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0") // ViewModel integration for Compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")   // Lifecycle state collection in Compose
    implementation("androidx.activity:activity-compose:1.8.2")    // Compose integration for Activity

    // Jetpack Compose UI toolkit
    implementation("androidx.compose.ui:ui")                      // Core Compose UI framework
    implementation("androidx.compose.ui:ui-graphics")             // Compose graphics utilities
    implementation("androidx.compose.ui:ui-tooling-preview")      // Preview support for Compose
    implementation("androidx.compose.material3:material3")        // Material 3 design components
    implementation("androidx.compose.material:material-icons-extended") // Extended Material icons

    // Navigation for Compose screens
    implementation("androidx.navigation:navigation-compose:2.7.7") // Compose navigation framework

    // Hilt dependency injection
    implementation("com.google.dagger:hilt-android:2.51")         // Hilt Android runtime
    ksp("com.google.dagger:hilt-android-compiler:2.51")           // Hilt annotation processor via KSP
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0") // Hilt integration for Compose navigation

    // DataStore for preferences (lightweight key-value storage)
    implementation("androidx.datastore:datastore-preferences:1.0.0") // Preferences DataStore for settings

    // Coroutines for async programming
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0") // Android-optimized coroutines

    // Splash screen API for modern app launch experience
    implementation("androidx.core:core-splashscreen:1.0.1") // SplashScreen backward-compatible API

    // Debug-only Compose tooling (layout inspector, previews)
    debugImplementation("androidx.compose.ui:ui-tooling")         // Compose UI tooling for debug
    debugImplementation("androidx.compose.ui:ui-test-manifest")   // Test manifest for Compose

    // Unit testing dependencies
    testImplementation("junit:junit:4.13.2")                      // JUnit 4 for unit tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")    // AndroidX JUnit extension
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Espresso for UI tests
}
