package com.netspeed.monitor.presentation.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.netspeed.monitor.presentation.component.DataUsageCard
import com.netspeed.monitor.presentation.component.MonitorToggleButton
import com.netspeed.monitor.presentation.component.SpeedGaugeCard
import com.netspeed.monitor.presentation.theme.NetSpeedTheme
import com.netspeed.monitor.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

// Home screen: main dashboard displaying live speed gauges and session usage statistics
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    // ViewModel injected via Hilt for managing UI state and service control
    viewModel: HomeViewModel = hiltViewModel(),
    // Navigation callback to open the Settings screen
    onNavigateToSettings: () -> Unit = {}
) {
    // Collect the full UI state from the ViewModel as Compose state
    val uiState by viewModel.uiState.collectAsState()

    // Snackbar host for showing transient messages (e.g., permission denied)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Launcher for the POST_NOTIFICATIONS permission request on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Update the ViewModel when the user responds to the permission dialog
        viewModel.updateNotificationPermission(granted)
        if (!granted) {
            // Show a snackbar if the user denies the notification permission
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Notification permission required for live speed updates")
            }
        }
    }

    // On first composition, request notification permission if running Android 13+
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Pre-Android 13 always has notification permission
            viewModel.updateNotificationPermission(true)
        }
    }

    Scaffold(
        // Snackbar host positioned at the bottom of the screen
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Top app bar with app title and navigation to Settings
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Net Speed Monitor",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                // Settings icon button navigates to the settings screen
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                // Match top app bar to the app background for seamless look
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        // Scrollable column for the main content area
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Speed gauge card: circular arcs for download and upload speeds
            SpeedGaugeCard(
                modifier = Modifier.fillMaxWidth(),
                networkSpeed = uiState.networkSpeed,
                isMonitoring = uiState.isMonitoring
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Data usage card: shows session totals for downloaded/uploaded data
            DataUsageCard(
                modifier = Modifier.fillMaxWidth(),
                totalRxBytes = uiState.networkSpeed.totalRxBytes,
                totalTxBytes = uiState.networkSpeed.totalTxBytes
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle button to start or stop the monitoring foreground service
            MonitorToggleButton(
                modifier = Modifier.fillMaxWidth(),
                isMonitoring = uiState.isMonitoring,
                onToggle = { viewModel.toggleMonitoring() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NetSpeedTheme(darkTheme = true) {
        // HomeScreen preview without a real ViewModel
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                CenterAlignedTopAppBar(
                    title = { Text("Net Speed Monitor") }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Preview — connect real ViewModel to see live data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
