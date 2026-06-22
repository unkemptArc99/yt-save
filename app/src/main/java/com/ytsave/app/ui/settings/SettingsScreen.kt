package com.ytsave.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ytsave.app.domain.model.FormatType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val defaultFormat by viewModel.defaultFormat.collectAsStateWithLifecycle()

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Default Format Preference",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Pre-selected when expanding Advanced Options",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormatType.values().forEach { type ->
                            FilterChip(
                                selected = defaultFormat == type.id,
                                onClick = { viewModel.updateDefaultFormat(type.id) },
                                label = { Text(type.label) }
                            )
                        }
                    }
                }
            }


            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ElevatedCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEachIndexed { index, (id, label) ->
                                    SegmentedButton(
                                        selected = themeMode == id,
                                        onClick = { viewModel.updateThemeMode(id) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                                    ) {
                                        Text(label)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ElevatedCard {
                        Column {
                            ListItem(
                                headlineContent = { Text("Version") },
                                supportingContent = { Text("1.0.0") }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ListItem(
                                headlineContent = { Text("Licenses") },
                                trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                                modifier = Modifier.clickable { /* Future: show licenses */ }
                            )
                        }
                    }
                }
            }
        }
    }
}
