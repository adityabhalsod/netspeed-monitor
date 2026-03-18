package com.netspeed.monitor.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netspeed.monitor.domain.model.NetworkSpeed
import com.netspeed.monitor.presentation.theme.DownloadGreen
import com.netspeed.monitor.presentation.theme.NetSpeedTheme
import com.netspeed.monitor.presentation.theme.UploadOrange

// Composite speed gauge card showing both download and upload speeds with arc visualizations
@Composable
fun SpeedGaugeCard(
    modifier: Modifier = Modifier,
    // Current network speed data to display
    networkSpeed: NetworkSpeed,
    // Whether the monitoring service is actively running
    isMonitoring: Boolean
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        // Rounded corners for modern card appearance
        shape = RoundedCornerShape(24.dp),
        // Use surface variant for subtle card elevation appearance
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        // Slight elevation for depth
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status indicator row at top of card
            StatusIndicator(isMonitoring = isMonitoring)

            Spacer(modifier = Modifier.height(16.dp))

            // Side-by-side download and upload speed gauges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Download speed gauge with green color and downward arrow icon
                SpeedArcGauge(
                    label = "Download",
                    speed = networkSpeed.downloadSpeed,
                    color = DownloadGreen,
                    icon = { ArrowIcon(isDownload = true) }
                )
                // Upload speed gauge with orange color and upward arrow icon
                SpeedArcGauge(
                    label = "Upload",
                    speed = networkSpeed.uploadSpeed,
                    color = UploadOrange,
                    icon = { ArrowIcon(isDownload = false) }
                )
            }
        }
    }
}

// Circular arc gauge displaying a single speed value with animated arc fill
@Composable
fun SpeedArcGauge(
    modifier: Modifier = Modifier,
    // Label text shown below the gauge ("Download" or "Upload")
    label: String,
    // Speed value in bytes per second
    speed: Double,
    // Accent color for the arc fill
    color: Color,
    // Custom icon slot above the speed text
    icon: @Composable () -> Unit = {}
) {
    // Normalize speed to a 0.0..1.0 sweep fraction (max visible threshold: 100 MB/s)
    val maxSpeedBps = 100.0 * 1_048_576 // 100 MB/s as the gauge maximum
    val rawFraction = (speed / maxSpeedBps).coerceIn(0.0, 1.0).toFloat()

    // Animate arc sweep angle changes smoothly for a polished gauge effect
    val sweepFraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "speedArc"
    )

    // Animate color saturation: slightly dim when speed is zero
    val arcColor by animateColorAsState(
        targetValue = if (speed > 0) color else color.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 400),
        label = "arcColor"
    )

    // Format the speed value for numeric display
    val speedText = NetworkSpeed.speedValue(speed)
    val unitText = NetworkSpeed.speedUnit(speed)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Box containing stack of the canvas arc and centered text
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            // Arc drawn on Canvas using drawArc with stroke style
            CircularArcCanvas(
                sweepFraction = sweepFraction,
                color = arcColor,
                size = 140.dp,
                strokeWidth = 12.dp
            )
            // Centered column with icon, numeric value, and unit
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                icon()
                Spacer(modifier = Modifier.height(2.dp))
                // Large numeric speed value
                Text(
                    text = if (speedText < 10.0) String.format("%.1f", speedText)
                           else String.format("%.0f", speedText),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                )
                // Unit label (KB/s, MB/s, B/s)
                Text(
                    text = unitText,
                    style = MaterialTheme.typography.labelSmall,
                    color = arcColor
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Label below the gauge circle
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Canvas-based arc that draws a background track and a colored foreground arc
@Composable
private fun CircularArcCanvas(
    sweepFraction: Float,
    color: Color,
    size: Dp,
    strokeWidth: Dp
) {
    // Background arc track color (dimmed version of the arc color)
    val trackColor = color.copy(alpha = 0.15f)

    Canvas(modifier = Modifier.size(size)) {
        val canvasSize = this.size
        val strokePx = strokeWidth.toPx()  // Convert dp to pixels for drawing
        val inset = strokePx / 2           // Inset to keep arc within bounds

        // Calculate arc bounds centered in the canvas
        val arcSize = Size(canvasSize.width - strokePx, canvasSize.height - strokePx)
        val topLeft = Offset(inset, inset)

        // Draw full background track arc (270 degree sweep starting from bottom-left)
        drawArc(
            color = trackColor,
            startAngle = 135f,       // Start at bottom-left of circle
            sweepAngle = 270f,       // Full gauge sweep
            useCenter = false,       // Draw as an arc not a pie slice
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // Draw filled foreground arc proportional to the current speed fraction
        drawArc(
            color = color,
            startAngle = 135f,
            sweepAngle = 270f * sweepFraction,  // Sweep matches normalized speed
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

// Directional arrow icon indicating download (down) or upload (up)
@Composable
private fun ArrowIcon(isDownload: Boolean) {
    Icon(
        imageVector = if (isDownload) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
        contentDescription = if (isDownload) "Download" else "Upload",
        tint = if (isDownload) DownloadGreen else UploadOrange,
        modifier = Modifier.size(16.dp)
    )
}

// Small status pill showing active/inactive monitoring state
@Composable
private fun StatusIndicator(isMonitoring: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Colored dot: green when active, grey when inactive
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isMonitoring) DownloadGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
        )
        Spacer(modifier = Modifier.size(6.dp))
        // Status label text
        Text(
            text = if (isMonitoring) "Monitoring" else "Stopped",
            style = MaterialTheme.typography.labelSmall,
            color = if (isMonitoring) DownloadGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Preview function showing the SpeedGaugeCard in a dark theme context
@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
private fun SpeedGaugeCardPreview() {
    NetSpeedTheme(darkTheme = true) {
        SpeedGaugeCard(
            modifier = Modifier.padding(16.dp),
            networkSpeed = NetworkSpeed(
                downloadSpeed = 2_500_000.0,  // 2.5 MB/s for preview
                uploadSpeed = 512_000.0       // 512 KB/s for preview
            ),
            isMonitoring = true
        )
    }
}
