package com.syncjam.app.feature.session.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.syncjam.app.core.ui.components.SyncJamButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinSessionScreen(
    onSessionJoined: (sessionId: String, sessionCode: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sessionCode by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    LaunchedEffect(uiState.sessionId, uiState.sessionCode) {
        val id = uiState.sessionId
        val code = uiState.sessionCode
        if (id != null && code.isNotEmpty()) {
            onSessionJoined(id, code)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session beitreten") },
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
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Dein Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = sessionCode,
                onValueChange = { sessionCode = it.uppercase().take(6) },
                label = { Text("Session-Code") },
                singleLine = true,
                supportingText = { Text("6-stelliger Code vom Host") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            SyncJamButton(
                text = "Session beitreten",
                onClick = {
                    viewModel.onEvent(
                        SessionEvent.JoinSession(
                            code = sessionCode,
                            userId = "",
                            displayName = displayName.ifBlank { "Gast" }
                        )
                    )
                },
                enabled = sessionCode.length >= 6 && !uiState.isLoading,
                isLoading = uiState.isLoading
            )
            uiState.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
