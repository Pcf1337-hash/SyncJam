package com.syncjam.app.feature.session.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.syncjam.app.core.ui.components.SyncJamButton

private enum class AutoDeleteOption(val label: String, val hours: Int) {
    NEVER("Nie", 0),
    ONE_DAY("1 Tag", 24),
    ONE_WEEK("1 Woche", 168),
    ONE_MONTH("1 Monat", 720)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionScreen(
    onSessionCreated: (sessionId: String, sessionCode: String, displayName: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sessionName by remember { mutableStateOf("") }
    var autoDeleteOption by remember { mutableStateOf(AutoDeleteOption.ONE_DAY) }
    var isPublic by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState.sessionId, uiState.sessionCode) {
        val id = uiState.sessionId
        val code = uiState.sessionCode
        if (id != null && code.isNotEmpty()) {
            viewModel.clearCreatedSession()
            onSessionCreated(id, code, uiState.pendingDisplayName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session erstellen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text("Session-Name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Session automatisch löschen nach",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AutoDeleteOption.entries.forEach { option ->
                    FilterChip(
                        selected = autoDeleteOption == option,
                        onClick = { autoDeleteOption = option },
                        label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Öffentliche Session", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "In der öffentlichen Liste anzeigen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = isPublic, onCheckedChange = { isPublic = it })
            }
            AnimatedVisibility(visible = isPublic) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Passwort (optional)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        supportingText = { Text("Leer lassen = keine Zugangsbeschränkung") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            SyncJamButton(
                text = "Jam Session starten",
                onClick = {
                    viewModel.onEvent(
                        SessionEvent.CreateSession(
                            name = sessionName.ifBlank { "Jam Session" },
                            userId = "",
                            displayName = "",
                            autoDeleteAfterHours = autoDeleteOption.hours,
                            isPublic = isPublic,
                            password = password
                        )
                    )
                },
                enabled = !uiState.isLoading,
                isLoading = uiState.isLoading
            )
            uiState.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
