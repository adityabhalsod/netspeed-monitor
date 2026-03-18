package com.netspeed.monitor.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.netspeed.monitor.presentation.theme.NetSpeedTheme
import com.netspeed.monitor.presentation.viewmodel.SettingsViewModel

// Settings screen: allows users to configure monitoring behavior and notification content
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    // ViewModel injected via Hilt for reading and writing settings
    viewModel: SettingsViewModel = hiltViewModel(),
    // Navigation callback to go back to the Home screen
    onNavigateBack: () -> Unit = {}
) {
    // Collect the current settings state from the ViewModel
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                // Back navigation icon to return to Home screen
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Section: General settings
            SettingsSectionHeader(title = "General")

            // Card grouping general settings items
            SettingsCard {
                // Toggle for starting the monitoring service automatically on device boot
                SettingsToggleItem(
                    title = "Start on Boot",
                    subtitle = "Automatically start monitoring when the device boots",
                    checked = uiState.startOnBoot,
                    onCheckedChange = { viewModel.setStartOnBoot(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Notification settings
            SettingsSectionHeader(title = "Notification")

            // Card grouping notification-related settings
            SettingsCard {
                // Toggle to show download speed in the persistent notification
                SettingsToggleItem(
                    title = "Show Download Speed",
                    subtitle = "Display download speed (↓) in notification",
                    checked = uiState.showDownloadSpeed,
                    onCheckedChange = { viewModel.setShowDownloadSpeed(it) }
                )

                // Divider between list items for visual clarity
                Spacer(modifier = Modifier.height(1.dp))

                // Toggle to show upload speed in the persistent notification
                SettingsToggleItem(
                    title = "Show Upload Speed",
                    subtitle = "Display upload speed (↑) in notification",
                    checked = uiState.showUploadSpeed,
                    onCheckedChange = { viewModel.setShowUploadSpeed(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Performance settings
            SettingsSectionHeader(title = "Performance")

            // Card grouping performance-related settings
            SettingsCard {
                // Dropdown to select the update interval between speed readings
                UpdateIntervalDropdown(
                    currentInterval = uiState.updateIntervalMs,
                    onIntervalSelected = { viewModel.setUpdateInterval(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Section header text displayed above groups of settings
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

// Rounded card container for grouping related settings items
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            content()
        }
    }
}

// A single settings row with title, subtitle, and a toggle switch
@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            // Primary label for the setting
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            // Descriptive hint text below the title
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = {
            // Switch control for the setting toggle
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

// Dropdown selector for the speed update interval (500ms, 1000ms, 2000ms)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateIntervalDropdown(
    currentInterval: Long,
    onIntervalSelected: (Long) -> Unit
) {
    // Available update interval options with labels
    val options = listOf(
        500L to "500ms (Fast — high accuracy, more battery)",
        1000L to "1s (Balanced — recommended)",
        2000L to "2s (Slow — better battery life)"
    )

    // Track dropdown expanded state locally
    var expanded by remember { mutableStateOf(false) }

    // Find the display label for the current interval selection
    val currentLabel = options.find { it.first == currentInterval }?.second ?: "1s (Balanced)"

    ListItem(
        headlineContent = {
            Text("Update Interval", style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Spacer(modifier = Modifier.height(8.dp))

            // Material 3 exposed dropdown menu for interval selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                // Text field showing selected interval, anchored to the dropdown menu
                OutlinedTextField(
                    value = currentLabel,
                    onValueChange = {},
                    readOnly = true,  // User cannot type; only select from dropdown
                    label = { Text("Interval") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Interval"
                        )
                    },
                    // Anchor the dropdown to this text field
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                )

                // Dropdown menu displaying all available options
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { (interval, label) ->
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                // Notify the ViewModel of the selected interval and close dropdown
                                onIntervalSelected(interval)
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    NetSpeedTheme(darkTheme = true) {
        SettingsScreen()
    }
}
