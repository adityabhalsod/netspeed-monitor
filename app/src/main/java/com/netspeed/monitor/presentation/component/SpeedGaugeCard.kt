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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.netspeed.monitor.presentation.theme.TextSecondary
import com.netspeed.monitor.presentation.theme.UploadOrange

// Modern speed gauge card with gradient background and glowing neon arcs
@Composable
fun SpeedGaugeCard(
    modifier: Modifier = Modifier,
    // Current network speed data to display on the gauges
    networkSpeed: NetworkSpeed,
    // Whether the monitoring service is actively running
    isMonitoring: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        // Gradient background for modern depth effect
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status pill indicating active/inactive monitoring
            StatusIndicator(isMonitoring = isMonitoring)

            Spacer(modifier = Modifier.height(20.dp))

            // Side-by-side download and upload speed gauges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Download gauge with green neon arc and downward arrow
                SpeedArcGauge(
                    label = "Download",
                    speed = networkSpeed.downloadSpeed,
                    color = DownloadGreen,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Download",
                            tint = DownloadGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                // Upload gauge with orange neon arc and upward arrow
                SpeedArcGauge(
                    label = "Upload",
                    speed = networkSpeed.uploadSpeed,
                    color = UploadOrange,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Upload",
                            tint = UploadOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

// Single speed gauge with animated arc, glow effect, and centered speed text
@Composable
private fun SpeedArcGauge(
    modifier: Modifier = Modifier,
    label: String,
    speed: Double,
    color: Color,
    icon: @Composable () -> Unit = {}
) {
    // Normalize speed to 0..1 fraction (gauge maximum: 100 MB/s)
    val maxSpeedBps = 100.0 * 1_048_576
    val rawFraction = (speed / maxSpeedBps).coerceIn(0.0, 1.0).toFloat()

    // Smooth arc animation for polished gauge movement
    val sweepFraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "speedArc"
    )

    // Dim arc when speed is zero for visual feedback
    val arcColor by animateColorAsState(
        targetValue = if (speed > 0) color else color.copy(alpha = 0.3f),
        animationSpec = tween(durationMillis = 400),
        label = "arcColor"
    )

    // Format speed value and unit label for display
    val speedText = NetworkSpeed.speedValue(speed)
    val unitText = NetworkSpeed.speedUnit(speed)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Layered box: canvas arc behind centered text overlay
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            // Draws track, glow, and active arc layers
            GlowingArcCanvas(
                sweepFraction = sweepFraction,
                color = arcColor,
                size = 150.dp,
                strokeWidth = 14.dp
            )
            // Speed value, unit, and icon centered inside the arc
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                icon()
                Spacer(modifier = Modifier.height(4.dp))
                // Large bold speed number
                Text(
                    text = if (speedText < 10.0) String.format("%.1f", speedText)
                    else String.format("%.0f", speedText),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Colored unit label below the number
                Text(
                    text = unitText,
                    style = MaterialTheme.typography.labelMedium,
                    color = arcColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Label below the gauge ("Download" or "Upload")
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

// Canvas drawing with three arc layers: dim track, glow halo, and bright foreground
@Composable
private fun GlowingArcCanvas(
    sweepFraction: Float,
    color: Color,
    size: Dp,
    strokeWidth: Dp
) {
    // Dim track color for the background arc
    val trackColor = color.copy(alpha = 0.1f)
    // Wider, semi-transparent layer for neon glow effect
    val glowColor = color.copy(alpha = 0.2f)

    Canvas(modifier = Modifier.size(size)) {
        val canvasSize = this.size
        val strokePx = strokeWidth.toPx()
        val glowStrokePx = strokePx + 10f
        val inset = glowStrokePx / 2
        val arcSize = Size(canvasSize.width - glowStrokePx, canvasSize.height - glowStrokePx)
        val topLeft = Offset(inset, inset)

        // Layer 1: full background track arc (270° sweep from bottom-left)
        drawArc(
            color = trackColor,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        if (sweepFraction > 0.001f) {
            // Layer 2: glow halo — wider arc with low opacity
            drawArc(
                color = glowColor,
                startAngle = 135f,
                sweepAngle = 270f * sweepFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = glowStrokePx, cap = StrokeCap.Round)
            )
            // Layer 3: main colored arc on top
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * sweepFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
    }
}

// Animated status pill showing monitoring state with colored dot
@Composable
private fun StatusIndicator(isMonitoring: Boolean) {
    // Animate indicator color between active green and muted grey
    val indicatorColor by animateColorAsState(
        targetValue = if (isMonitoring) DownloadGreen else TextSecondary,
        animationSpec = tween(durationMillis = 300),
        label = "statusColor"
    )

    // Pill-shaped surface with subtle tinted background
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = indicatorColor.copy(alpha = 0.15f),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Small colored dot indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            // Status label text
            Text(
                text = if (isMonitoring) "Monitoring" else "Stopped",
                style = MaterialTheme.typography.labelSmall,
                color = indicatorColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E21)
@Composable
private fun SpeedGaugeCardPreview() {
    NetSpeedTheme(darkTheme = true) {
        SpeedGaugeCard(
            modifier = Modifier.padding(16.dp),
            networkSpeed = NetworkSpeed(
                downloadSpeed = 2_500_000.0,
                uploadSpeed = 512_000.0
            ),
            isMonitoring = true
        )
    }
}


