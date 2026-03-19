// Ultra-lightweight app build: pure Java, no Kotlin, no Compose, no AndroidX, zero dependencies
plugins {
    // Only the Android application plugin — no Kotlin, Hilt, or KSP needed
    id("com.android.application")
}

android {
    // Namespace for generated R class
    namespace = "com.netspeed.monitor"
    // Compile against Android API 34
    compileSdk = 34

    defaultConfig {
        // Unique application identifier
        applicationId = "com.netspeed.monitor"
        // Minimum API 26 (Android 8.0) for notification channels and adaptive icons
        minSdk = 26
        // Target latest SDK for best behavior and security
        targetSdk = 34
        // Application version code for updates
        versionCode = 1
        // Human-readable version name
        versionName = "1.0.0"
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
        // Release: aggressive minification and resource shrinking for smallest APK
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
        // Debug: no minification for fast iteration
        debug {
            isMinifyEnabled = false
        }
    }

    // Java 11 for broad compatibility
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Exclude unnecessary metadata files from the APK
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*.kotlin_module"
            excludes += "kotlin/**"
            excludes += "DebugProbesKt.bin"
        }
    }
}

// Zero external dependencies — the app uses only the Android SDK
dependencies {
}
