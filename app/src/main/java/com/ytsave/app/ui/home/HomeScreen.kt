package com.ytsave.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ytsave.app.ui.home.components.ActiveDownloadCard
import com.ytsave.app.ui.home.components.VideoPreviewCard

@Composable
fun HomeScreen(
    sharedUrl: String?,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            viewModel.updateUrl(sharedUrl)
            viewModel.fetchVideoInfo(sharedUrl)
        }
    }

    LaunchedEffect(uiState.fetchState) {
        if (uiState.fetchState is FetchState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState.fetchState as FetchState.Error).message
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.downloadEvents.collect { title ->
            snackbarHostState.showSnackbar(
                message = "Download completed: $title",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF4CAF50)
                        )
                        Text(data.visuals.message)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "YTSave",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.url,
                            onValueChange = { viewModel.updateUrl(it) },
                            placeholder = { Text("Paste YouTube URL") },
                            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = { viewModel.fetchVideoInfo(uiState.url) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.url.isNotBlank() && uiState.fetchState !is FetchState.Loading
                        ) {
                            if (uiState.fetchState is FetchState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Fetch")
                            }
                        }
                    }
                }
            }

            if (uiState.fetchState is FetchState.Success) {
                item {
                    VideoPreviewCard(
                        videoInfo = (uiState.fetchState as FetchState.Success).videoInfo,
                        defaultFormatType = uiState.defaultFormat,
                        onDownload = { formatSpec, formatType ->
                            viewModel.startDownload(formatSpec, formatType)
                        }
                    )
                }
            }

            if (uiState.activeDownloads.isNotEmpty()) {
                item {
                    Text(
                        text = "Currently Downloading",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                items(uiState.activeDownloads, key = { it.id }) { download ->
                    ActiveDownloadCard(
                        download = download,
                        onCancel = { viewModel.cancelDownload(download.id) }
                    )
                }
            }
        }
    }
}
