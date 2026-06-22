package com.ytsave.app.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytsave.app.domain.model.VideoFormat
import com.ytsave.app.domain.model.VideoInfo
import com.ytsave.app.ui.util.FormatUtils
import com.ytsave.app.domain.model.FormatType

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPreviewCard(
    videoInfo: VideoInfo,
    defaultFormatType: FormatType,
    onDownload: (formatSpec: String, formatType: String) -> Unit
) {
    var isAdvancedOptionsExpanded by remember { mutableStateOf(false) }
    var selectedFormatType by remember(defaultFormatType) { mutableStateOf(defaultFormatType) }
    var selectedFormatId by remember { mutableStateOf<String?>(null) }
    var selectedAudioFormatId by remember { mutableStateOf<String?>(null) }

    val videoFormats = remember(videoInfo.formats) {
        videoInfo.formats
            .filter { it.vcodec != "none" && it.acodec != "none" }
            .distinctBy { it.resolution }
            .sortedByDescending { it.height }
    }

    val videoOnlyFormats = remember(videoInfo.formats) {
        videoInfo.formats
            .filter { it.vcodec != "none" }
            .distinctBy { it.resolution }
            .sortedByDescending { it.height }
    }

    val audioOnlyFormats = remember(videoInfo.formats) {
        videoInfo.formats
            .filter { it.vcodec == "none" && it.acodec != "none" }
            .distinctBy { it.abr }
            .sortedByDescending { it.abr }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = videoInfo.thumbnail,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier
                        .width(140.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = videoInfo.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = videoInfo.channel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatDuration(videoInfo.duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { onDownload("bestvideo+bestaudio/best", "video+audio") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Download Best Quality")
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = { isAdvancedOptionsExpanded = !isAdvancedOptionsExpanded }) {
                    Text("Advanced Options")
                    Icon(
                        imageVector = if (isAdvancedOptionsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isAdvancedOptionsExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider()
                    
                    Text("Format", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        FormatType.values().forEach { type ->
                            FilterChip(
                                selected = selectedFormatType == type,
                                onClick = {
                                    selectedFormatType = type
                                    selectedFormatId = null
                                },
                                label = { Text(type.label) }
                            )
                        }
                    }

                    if (selectedFormatType == FormatType.VIDEO_AUDIO) {
                        if (videoOnlyFormats.isNotEmpty() && audioOnlyFormats.isNotEmpty()) {
                            Text("Select Video Quality", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                videoOnlyFormats.forEach { format ->
                                    val sizeText = format.filesize?.let { " · ${FormatUtils.formatFileSize(it)}" } ?: ""
                                    FilterChip(
                                        selected = selectedFormatId == format.formatId,
                                        onClick = { selectedFormatId = format.formatId },
                                        label = { Text("${format.resolution}$sizeText") }
                                    )
                                }
                            }
                            
                            Text("Select Audio Quality", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                audioOnlyFormats.forEach { format ->
                                    val sizeText = format.filesize?.let { " · ${FormatUtils.formatFileSize(it)}" } ?: ""
                                    FilterChip(
                                        selected = selectedAudioFormatId == format.formatId,
                                        onClick = { selectedAudioFormatId = format.formatId },
                                        label = { Text("${format.resolution}$sizeText") }
                                    )
                                }
                            }
                        } else {
                            Text("Stitching not available.", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        val currentFormats = if (selectedFormatType == FormatType.VIDEO_ONLY) videoOnlyFormats else audioOnlyFormats
                        if (currentFormats.isNotEmpty()) {
                            Text("Available Qualities", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                currentFormats.forEach { format ->
                                    val sizeText = format.filesize?.let { " · ${FormatUtils.formatFileSize(it)}" } ?: ""
                                    FilterChip(
                                        selected = selectedFormatId == format.formatId,
                                        onClick = { selectedFormatId = format.formatId },
                                        label = { Text("${format.resolution}$sizeText") }
                                    )
                                }
                            }
                        } else {
                            Text("No direct formats available.", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Button(
                        onClick = {
                            if (selectedFormatType == FormatType.VIDEO_AUDIO) {
                                if (selectedFormatId != null && selectedAudioFormatId != null) {
                                    onDownload("${selectedFormatId}+${selectedAudioFormatId}", "video+audio")
                                }
                            } else {
                                selectedFormatId?.let { formatId ->
                                    val typeStr = if (selectedFormatType == FormatType.VIDEO_ONLY) "video_only" else "audio_only"
                                    onDownload(formatId, typeStr)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = if (selectedFormatType == FormatType.VIDEO_AUDIO) selectedFormatId != null && selectedAudioFormatId != null else selectedFormatId != null,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Download Selected")
                    }
                }
            }
        }
    }
}
