// Plugin management: defines repositories for Gradle plugin resolution
pluginManagement {
    // Repositories to search for Gradle plugins
    repositories {
        google()          // Google's Maven repo for Android Gradle Plugin, etc.
        mavenCentral()    // Central Maven for community plugins
        gradlePluginPortal() // Gradle Plugin Portal for general Gradle plugins
    }
}

// Dependency resolution management: defines repositories for project dependencies
dependencyResolutionManagement {
    // Fail if any subproject defines its own repositories
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // Repositories for resolving dependencies
    repositories {
        google()          // Google's Maven repo for AndroidX, Compose, etc.
        mavenCentral()    // Central Maven for community libraries
    }
}

// Root project name
rootProject.name = "NetSpeedMonitor"

// Include the app module
include(":app")
