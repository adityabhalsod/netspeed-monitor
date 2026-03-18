package com.netspeed.monitor.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netspeed.monitor.presentation.theme.DownloadGreen
import com.netspeed.monitor.presentation.theme.NetSpeedTheme
import com.netspeed.monitor.presentation.theme.UploadOrange

// Toggle button that starts or stops the network monitoring service
@Composable
fun MonitorToggleButton(
    modifier: Modifier = Modifier,
    // Current monitoring state: true = active, false = stopped
    isMonitoring: Boolean,
    // Callback invoked when the button is clicked to toggle state
    onToggle: () -> Unit
) {
    // Animate background color transition between active and inactive states
    val buttonColor by animateColorAsState(
        targetValue = if (isMonitoring) UploadOrange else DownloadGreen,
        animationSpec = tween(durationMillis = 300),
        label = "toggleColor"
    )

    Button(
        onClick = onToggle,
        modifier = modifier,
        // Pill-shaped button for modern look
        shape = RoundedCornerShape(50.dp),
        // Use animated color for contextual feedback
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Show play or pause icon based on current monitoring state
            Icon(
                imageVector = if (isMonitoring) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isMonitoring) "Stop monitoring" else "Start monitoring",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Label text for accessibility and clarity
            Text(
                text = if (isMonitoring) "Stop" else "Start",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Preview
@Composable
private fun MonitorToggleButtonStartPreview() {
    NetSpeedTheme {
        MonitorToggleButton(isMonitoring = false, onToggle = {})
    }
}

@Preview
@Composable
private fun MonitorToggleButtonStopPreview() {
    NetSpeedTheme {
        MonitorToggleButton(isMonitoring = true, onToggle = {})
    }
}
