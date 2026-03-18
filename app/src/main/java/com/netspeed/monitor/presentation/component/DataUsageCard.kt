package com.netspeed.monitor.presentation.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netspeed.monitor.domain.model.NetworkSpeed
import com.netspeed.monitor.presentation.theme.DownloadGreen
import com.netspeed.monitor.presentation.theme.NetSpeedTheme
import com.netspeed.monitor.presentation.theme.UploadOrange

// Card displaying total data usage statistics for both download and upload
@Composable
fun DataUsageCard(
    modifier: Modifier = Modifier,
    // Total bytes received since device boot
    totalRxBytes: Long,
    // Total bytes transmitted since device boot
    totalTxBytes: Long
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card header row with icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DataUsage,
                    contentDescription = "Data usage",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Card title label
                Text(
                    text = "Session Usage",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Download usage row with progress indicator
            UsageRow(
                label = "Downloaded",
                bytes = totalRxBytes,
                color = DownloadGreen,
                totalBytes = totalRxBytes + totalTxBytes,
                icon = Icons.Default.ArrowDownward
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Upload usage row with progress indicator
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

// Single row showing label, formatted byte size, and an animated horizontal progress bar
@Composable
private fun UsageRow(
    label: String,
    bytes: Long,
    color: Color,
    // Total bytes (rx + tx) used to compute bar width fraction
    totalBytes: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    // Compute fraction for progress bar: 0.0 if total is zero (avoid division by zero)
    val fraction = if (totalBytes > 0L) (bytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    // Animate progress bar fill when fraction changes
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "usageProgress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Row with colored icon and label text
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Label: "Downloaded" or "Uploaded"
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
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Animated linear progress bar representing proportion of total usage
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
private fun DataUsageCardPreview() {
    NetSpeedTheme(darkTheme = true) {
        DataUsageCard(
            modifier = Modifier.padding(16.dp),
            totalRxBytes = 52_428_800L,   // 50 MB received for preview
            totalTxBytes = 10_485_760L    // 10 MB transmitted for preview
        )
    }
}
