package com.netspeed.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.netspeed.monitor.presentation.screen.HomeScreen
import com.netspeed.monitor.presentation.screen.SettingsScreen
import com.netspeed.monitor.presentation.theme.NetSpeedTheme
import dagger.hilt.android.AndroidEntryPoint

// Route strings for the app's navigation graph
private object Route {
    const val HOME = "home"        // Route to the Home/Dashboard screen
    const val SETTINGS = "settings" // Route to the Settings screen
}

// Main Activity: single-activity host for the entire Compose UI
// @AndroidEntryPoint enables Hilt injection in this Activity
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen before calling super (required by SplashScreen API)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge rendering for immersive content experience
        enableEdgeToEdge()
        // Set the root Compose content with the app theme applied
        setContent {
            NetSpeedTheme {
                // Render the main navigation host
                NetSpeedNavHost()
            }
        }
    }
}

// Root composable: sets up the Navigation Controller and defines the navigation graph
@Composable
private fun NetSpeedNavHost() {
    // Create and remember the NavController for managing back stack
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        // Start navigation at the Home screen
        startDestination = Route.HOME
    ) {
        // Home screen destination: shows live speed gauges and toggle button
        composable(Route.HOME) {
            HomeScreen(
                // Navigate forward to the Settings screen
                onNavigateToSettings = { navController.navigate(Route.SETTINGS) }
            )
        }

        // Settings screen destination: configure notification and monitoring preferences
        composable(Route.SETTINGS) {
            SettingsScreen(
                // Pop the Settings screen and return to Home
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainActivityPreview() {
    NetSpeedTheme {
        NetSpeedNavHost()
    }
}
