package com.netspeed.monitor.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark color scheme: deep navy with neon cyan accent and speed-themed highlights
private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,               // Vivid cyan for interactive elements
    onPrimary = DarkBackground,         // Dark text on primary surfaces
    surface = DarkSurface,              // Card and component background
    surfaceVariant = DarkSurfaceVariant, // Elevated or grouped surfaces
    onSurface = TextPrimary,            // Primary text on dark surfaces
    onSurfaceVariant = TextSecondary,   // Muted text for secondary info
    background = DarkBackground,        // App canvas background
    onBackground = TextPrimary,         // Text on background
    secondary = DownloadGreen,          // Green for download indicators
    tertiary = UploadOrange             // Orange for upload indicators
)

// Light color scheme for users who prefer light mode
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,              // Standard blue for light mode
    onPrimary = LightBackground,        // Light text on primary surfaces
    surface = LightSurface,             // White card background
    surfaceVariant = LightSurfaceVariant, // Light grey grouped surfaces
    onSurface = LightTextPrimary,       // Dark text on light surfaces
    onSurfaceVariant = LightTextSecondary, // Muted text on light surfaces
    background = LightBackground,       // Very light app background
    onBackground = LightTextPrimary,    // Dark text on light background
    secondary = DownloadGreen,          // Keep download indicator green
    tertiary = UploadOrange             // Keep upload indicator orange
)

// Main app theme composable that wraps all screens
@Composable
fun NetSpeedTheme(
    // Follow system dark/light mode by default
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Use Material You dynamic colors on Android 12+ if available
    dynamicColor: Boolean = true,
    // Slot for all child composables
    content: @Composable () -> Unit
) {
    // Resolve the appropriate color scheme based on settings and API level
    val colorScheme = when {
        // Use dynamic Material You colors on Android 12+ if enabled
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // Use our custom dark scheme if dark mode is active
        darkTheme -> DarkColorScheme
        // Otherwise use the custom light scheme
        else -> LightColorScheme
    }

    // Update the system status bar color to match the app theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar background to match app's background color
            window.statusBarColor = colorScheme.background.toArgb()
            // Set status bar icon appearance based on theme brightness
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Apply Material 3 theming with our color scheme and typography
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
