package com.netspeed.monitor.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netspeed.monitor.presentation.theme.DownloadGreen
import com.netspeed.monitor.presentation.theme.NetSpeedTheme

// Modern toggle button to start or stop the network monitoring service
@Composable
fun MonitorToggleButton(
    modifier: Modifier = Modifier,
    // Current monitoring state: true = active, false = stopped
    isMonitoring: Boolean,
    // Callback invoked when the button is clicked to toggle monitoring
    onToggle: () -> Unit
) {
    // Animate button color: green for start action, red for stop action
    val containerColor by animateColorAsState(
        targetValue = if (isMonitoring) Color(0xFFE53935) else DownloadGreen,
        animationSpec = tween(durationMillis = 300),
        label = "toggleColor"
    )

    Button(
        onClick = onToggle,
        modifier = modifier.height(56.dp),
        // Rounded rectangle for modern button shape
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White
        ),
        // Elevated shadow for depth effect
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        ),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Play or pause icon based on monitoring state
            Icon(
                imageVector = if (isMonitoring) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isMonitoring) "Stop monitoring" else "Start monitoring",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Descriptive button label
            Text(
                text = if (isMonitoring) "Stop Monitoring" else "Start Monitoring",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview
@Composable
private fun MonitorToggleButtonStartPreview() {
    NetSpeedTheme {
        MonitorToggleButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            isMonitoring = false,
            onToggle = {}
        )
    }
}

@Preview
@Composable
private fun MonitorToggleButtonStopPreview() {
    NetSpeedTheme {
        MonitorToggleButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            isMonitoring = true,
            onToggle = {}
        )
    }
}
