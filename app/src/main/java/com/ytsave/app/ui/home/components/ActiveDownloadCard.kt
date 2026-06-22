package com.ytsave.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ytsave.app.data.local.DownloadEntity
import com.ytsave.app.data.local.DownloadStatus

@Composable
fun ActiveDownloadCard(
    download: DownloadEntity,
    onCancel: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = download.channel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (download.status == DownloadStatus.DOWNLOADING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = download.progress / 100f,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .padding(end = 12.dp)
                    )
                    Text(
                        text = "${download.progress}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = download.downloadSpeed ?: "Calculating...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "ETA ${download.eta ?: "--:--"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val statusColor = when (download.status) {
                    DownloadStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant
                    DownloadStatus.MERGING -> MaterialTheme.colorScheme.secondary
                    DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
                
                Text(
                    text = download.status.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
                if (download.status == DownloadStatus.FAILED && download.errorMessage != null) {
                    Text(
                        text = download.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
