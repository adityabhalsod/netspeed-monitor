package com.netspeed.monitor.presentation.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DataUsage
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netspeed.monitor.domain.model.NetworkSpeed
import com.netspeed.monitor.presentation.theme.AccentCyan
import com.netspeed.monitor.presentation.theme.DownloadGreen
import com.netspeed.monitor.presentation.theme.NetSpeedTheme
import com.netspeed.monitor.presentation.theme.UploadOrange

// Modern data usage card showing session download/upload totals with gradient progress bars
@Composable
fun DataUsageCard(
    modifier: Modifier = Modifier,
    // Total bytes received in this monitoring session
    totalRxBytes: Long,
    // Total bytes transmitted in this monitoring session
    totalTxBytes: Long
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // Gradient background for modern depth effect
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // Card header with icon and title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DataUsage,
                    contentDescription = "Data usage",
                    tint = AccentCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Session Usage",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download usage row with gradient progress bar
            UsageRow(
                label = "Downloaded",
                bytes = totalRxBytes,
                color = DownloadGreen,
                totalBytes = totalRxBytes + totalTxBytes,
                icon = Icons.Default.ArrowDownward
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Upload usage row with gradient progress bar
            UsageRow(
                label = "Uploaded",
                bytes = totalTxBytes,
                color = UploadOrange,
                totalBytes = totalRxBytes + totalTxBytes,
                icon = Icons.Default.ArrowUpward
            )
        }
    }
}

// Single usage row with icon badge, label, byte value, and animated gradient progress bar
@Composable
private fun UsageRow(
    label: String,
    bytes: Long,
    color: Color,
    // Total bytes (rx + tx) for computing progress bar fraction
    totalBytes: Long,
    icon: ImageVector
) {
    // Compute progress fraction; zero if no data to avoid division by zero
    val fraction = if (totalBytes > 0L)
        (bytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    // Smooth animation when fraction changes
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "usageProgress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Colored icon badge with rounded background
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                // Row label ("Downloaded" or "Uploaded")
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Formatted byte count (e.g., "12.50 MB")
            Text(
                text = NetworkSpeed.formatBytes(bytes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Custom gradient progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.1f))
        ) {
            // Filled portion with horizontal gradient from dim to bright
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.6f),
                                color
                            )
                        )
                    )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E21)
@Composable
private fun DataUsageCardPreview() {
    NetSpeedTheme(darkTheme = true) {
        DataUsageCard(
            modifier = Modifier.padding(16.dp),
            totalRxBytes = 52_428_800L,
            totalTxBytes = 10_485_760L
        )
    }
}
