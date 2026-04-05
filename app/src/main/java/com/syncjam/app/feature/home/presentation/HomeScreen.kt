package com.syncjam.app.feature.home.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.syncjam.app.core.update.UpdateDialog
import com.syncjam.app.db.entity.SessionHistoryEntity
import com.syncjam.app.feature.library.presentation.LibraryScreen
import com.syncjam.app.feature.profile.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateSession: () -> Unit,
    onJoinSession: () -> Unit,
    onRejoinSession: (code: String, isHost: Boolean) -> Unit = { _, _ -> },
    onJoinPublicSession: (code: String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detectedClipboardCode by viewModel.detectedClipboardCode.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val clipboardManager = LocalClipboardManager.current
    LaunchedEffect(Unit) {
        viewModel.refreshLastSession()
        viewModel.fetchPublicSessions()
        val clipText = clipboardManager.getText()?.text?.trim()
        if (clipText != null && clipText.matches(Regex("[A-Z2-9]{6}"))) {
            viewModel.onClipboardCodeDetected(clipText)
        }
    }

    // Clipboard session-code paste dialog
    detectedClipboardCode?.let { code ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissClipboardDialog() },
            title = { Text("Session beitreten?") },
            text = { Text("Session-Code $code in der Zwischenablage gefunden. Möchtest du beitreten?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissClipboardDialog()
                    onJoinSession()
                }) { Text("Beitreten") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClipboardDialog() }) { Text("Ignorieren") }
            }
        )
    }

    uiState.availableUpdate?.let { release ->
        UpdateDialog(release = release, onDismiss = { viewModel.dismissUpdate() })
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; viewModel.fetchPublicSessions() },
                    icon = { Icon(if (selectedTab == 1) Icons.Filled.Public else Icons.Outlined.Public, null) },
                    label = { Text("Sessions") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(if (selectedTab == 2) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic, null) },
                    label = { Text("Bibliothek") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person, null) },
                    label = { Text("Profil") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> DashboardTab(
                uiState = uiState,
                onCreateSession = onCreateSession,
                onJoinSession = onJoinSession,
                onRejoinSession = onRejoinSession,
                onJoinPublicSession = onJoinPublicSession,
                onDeleteSession = { session ->
                    viewModel.deleteSession(
                        sessionId = session.id,
                        sessionCode = session.sessionCode,
                        userId = uiState.displayName
                    )
                },
                onRenameSession = { session, newName ->
                    viewModel.renameSession(
                        sessionCode = session.sessionCode,
                        newName = newName,
                        hostId = uiState.displayName
                    )
                },
                modifier = Modifier.padding(padding)
            )
            1 -> PublicSessionsTab(
                sessions = uiState.publicSessions,
                onJoin = onJoinPublicSession,
                onRefresh = { viewModel.fetchPublicSessions() },
                onCreateSession = onCreateSession,
                modifier = Modifier.padding(padding)
            )
            2 -> LibraryScreen(
                onCreateSession = onCreateSession,
                onJoinSession = onJoinSession,
                modifier = Modifier.padding(padding)
            )
            3 -> ProfileTab(
                onNavigateToSettings = onNavigateToSettings,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DashboardTab(
    uiState: HomeUiState,
    onCreateSession: () -> Unit,
    onJoinSession: () -> Unit,
    onRejoinSession: (code: String, isHost: Boolean) -> Unit,
    onJoinPublicSession: (code: String) -> Unit,
    onDeleteSession: (SessionHistoryEntity) -> Unit,
    onRenameSession: (SessionHistoryEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "SyncJam",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Gemeinsam Musik hören",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Schnell wieder beitreten
        if (uiState.lastSessionCode != null) {
            item {
                ElevatedCard(
                    onClick = { onRejoinSession(uiState.lastSessionCode, uiState.isLastSessionHost) },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Weiterjammen",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Session ${uiState.lastSessionCode} fortsetzen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Schnellaktionen
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.Add,
                    title = "Neue Session",
                    subtitle = "Jam starten",
                    onClick = onCreateSession,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                QuickActionCard(
                    icon = Icons.Default.GroupAdd,
                    title = "Beitreten",
                    subtitle = "Code eingeben",
                    onClick = onJoinSession,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // ── Session-Verlauf ───────────────────────────────────────────────────
        item {
            SectionHeader(icon = Icons.Default.History, title = "Letzte Sessions")
        }

        if (uiState.recentSessions.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VinylIdleAnimation()
                        Text(
                            "Noch keine Sessions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items = uiState.recentSessions, key = { it.id }) { session ->
                SessionHistoryCard(
                    session = session,
                    onJoin = {
                        if (session.isHost) onRejoinSession(session.sessionCode, true)
                        else onJoinPublicSession(session.sessionCode)
                    },
                    onDelete = { onDeleteSession(session) },
                    onRename = { newName -> onRenameSession(session, newName) }
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── PublicSessionsTab ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublicSessionsTab(
    sessions: List<PublicSessionUi>,
    onJoin: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Öffentliche Sessions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, "Aktualisieren")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Keine öffentlichen Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Starte eine öffentliche Session damit andere beitreten können.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(24.dp))
                FilledTonalButton(onClick = onCreateSession) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Session erstellen")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "${sessions.size} ${if (sessions.size == 1) "Session" else "Sessions"} aktiv",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(items = sessions, key = { it.sessionCode }) { session ->
                    PublicSessionCard(session = session, onClick = { onJoin(session.sessionCode) })
                }
            }
        }
    }
}

// ── Shared UI ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(24.dp), tint = contentColor)
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = contentColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.7f))
        }
    }
}

// ── SessionHistoryCard ────────────────────────────────────────────────────────

@Composable
private fun SessionHistoryCard(
    session: SessionHistoryEntity,
    onJoin: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    val isActive = session.endedAt == null
    val dateStr = remember(session.startedAt) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN).format(Date(session.startedAt))
    }

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Umbenennen-Dialog
    if (showRenameDialog) {
        var nameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Session umbenennen") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Neuer Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            onRename(nameInput.trim())
                            showRenameDialog = false
                        }
                    }
                ) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    // Löschen-Bestätigung
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Session löschen?") },
            text = { Text("Session ${session.sessionCode} wird aus dem Verlauf und vom Server entfernt.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") }
            }
        )
    }

    // Eigene Sessions (isHost) → tertiaryContainer, andere → surfaceContainer
    val containerColor = when {
        session.isHost -> MaterialTheme.colorScheme.tertiaryContainer
        isActive       -> MaterialTheme.colorScheme.primaryContainer
        else           -> MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        onClick = onJoin,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            session.isHost -> MaterialTheme.colorScheme.tertiary
                            isActive       -> MaterialTheme.colorScheme.primary
                            else           -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        session.isHost -> Icons.Default.Star
                        isActive       -> Icons.Default.MusicNote
                        else           -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
                        session.isHost -> MaterialTheme.colorScheme.onTertiary
                        isActive       -> MaterialTheme.colorScheme.onPrimary
                        else           -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            // Infos
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Session ${session.sessionCode}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (session.isHost) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "MEINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "AKTIV",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                session.lastTrackTitle?.let { title ->
                    Text(
                        "Zuletzt: $title${session.lastTrackArtist?.let { " · $it" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Rechte Seite: Stats + Admin-Menü
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${session.participantCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (session.participantCount == 1) "Person" else "Personen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${session.tracksPlayed} Tracks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Drei-Punkte-Menü (nur für eigene Sessions)
            if (session.isHost) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Session-Optionen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Umbenennen") },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                            onClick = { showMenu = false; showRenameDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Löschen", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete, null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { showMenu = false; showDeleteConfirm = true }
                        )
                    }
                }
            }
        }
    }
}

// ── PublicSessionCard ─────────────────────────────────────────────────────────

@Composable
private fun PublicSessionCard(session: PublicSessionUi, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        session.sessionName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (session.isPasswordProtected) {
                        Icon(Icons.Default.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                session.currentTrackTitle?.let { title ->
                    Text(
                        "$title${session.currentTrackArtist?.let { " · $it" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "Code: ${session.sessionCode}  ·  ${session.participantCount} ${if (session.participantCount == 1) "Person" else "Personen"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── ProfileTab ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTab(
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val state by profileViewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) profileViewModel.uploadAvatar(uri)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.avatarUrl != null) {
                            AsyncImage(
                                model = state.avatarUrl,
                                contentDescription = "Profilbild",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initial = state.displayName.firstOrNull()?.uppercase() ?: "?"
                            Text(
                                initial,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (state.isUploadingAvatar) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                    // Camera edit button
                    Surface(
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, "Foto ändern", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                if (state.avatarUrl != null) {
                    TextButton(onClick = { profileViewModel.removeAvatar() }) {
                        Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Profilbild entfernen", style = MaterialTheme.typography.labelMedium)
                    }
                }
                state.avatarError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            Text(
                state.email.ifBlank { "Gast" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Anzeigename",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = { profileViewModel.onDisplayNameChange(it) },
                    label = { Text("Dein Name") },
                    singleLine = true,
                    supportingText = {
                        if (state.isSaved) Text("Gespeichert ✓", color = MaterialTheme.colorScheme.primary)
                        else Text("Wird in Sessions als Anzeigename genutzt")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                FilledTonalButton(
                    onClick = { profileViewModel.saveDisplayName() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.displayName.isNotBlank()
                ) { Text("Speichern") }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Einstellungen")
            }
        }
    }
}

// ── VinylIdleAnimation ────────────────────────────────────────────────────────

@Composable
fun VinylIdleAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_idle")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "rot"
    )
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { rotationZ = rotation }
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}
