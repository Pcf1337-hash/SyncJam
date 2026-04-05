package com.syncjam.app.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Einstellungen",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Audio ──────────────────────────────────────────────────────────
            item {
                SettingsSectionHeader("Audio")
            }

            item {
                SettingsItem(label = "Übertragungsqualität") {
                    SingleChoiceSegmentedButtonRow {
                        AudioQuality.entries.forEachIndexed { index, quality ->
                            SegmentedButton(
                                selected = state.audioQuality == quality,
                                onClick = { viewModel.setAudioQuality(quality) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = AudioQuality.entries.size
                                ),
                                label = {
                                    Text(
                                        text = when (quality) {
                                            AudioQuality.LOW -> "Niedrig"
                                            AudioQuality.MEDIUM -> "Mittel"
                                            AudioQuality.HIGH -> "Hoch"
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                SettingsItem(
                    label = "Push-to-Talk",
                    description = "Mikrofon nur bei gedrückter Taste aktivieren"
                ) {
                    Switch(
                        checked = state.pushToTalk,
                        onCheckedChange = { viewModel.setPushToTalk(it) }
                    )
                }
            }

            item { SettingsDivider() }

            // ── Benachrichtigungen ─────────────────────────────────────────────
            item {
                SettingsSectionHeader("Benachrichtigungen")
            }

            item {
                SettingsItem(
                    label = "Benachrichtigungstöne",
                    description = "Töne bei Session-Ereignissen abspielen"
                ) {
                    Switch(
                        checked = state.notificationSounds,
                        onCheckedChange = { viewModel.setNotificationSounds(it) }
                    )
                }
            }

            item {
                SettingsItem(
                    label = "Vibration",
                    description = "Bei Reaktionen und Benachrichtigungen vibrieren"
                ) {
                    Switch(
                        checked = state.vibration,
                        onCheckedChange = { viewModel.setVibration(it) }
                    )
                }
            }

            item { SettingsDivider() }

            // ── Erscheinungsbild ───────────────────────────────────────────────
            item {
                SettingsSectionHeader("Erscheinungsbild")
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Farbschema",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) }
                            )
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "Systemstandard"
                                    ThemeMode.LIGHT -> "Hell"
                                    ThemeMode.DARK -> "Dunkel"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item { SettingsDivider() }

            // ── Speicher ───────────────────────────────────────────────────────
            item {
                SettingsSectionHeader("Speicher")
            }

            item {
                SettingsItem(
                    label = "Cache",
                    description = "Temporäre Dateien: ${state.cacheSize}"
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.clearCache() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Leeren")
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SettingsItem(
    label: String,
    description: String? = null,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailingContent()
    }
}
