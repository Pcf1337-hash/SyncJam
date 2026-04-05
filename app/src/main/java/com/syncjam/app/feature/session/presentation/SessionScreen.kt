package com.syncjam.app.feature.session.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.syncjam.app.feature.voice.presentation.VoiceOverlay
import com.syncjam.app.feature.voice.presentation.VoiceViewModel
import com.syncjam.app.feature.voice.presentation.components.MuteButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SessionScreen(
    sessionCode: String = "",
    isHost: Boolean = false,
    displayName: String = "",
    onLeave: () -> Unit,
    onOpenPlaylist: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
    voiceViewModel: VoiceViewModel = hiltViewModel(),
    libraryViewModel: com.syncjam.app.feature.library.presentation.LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceState by voiceViewModel.voiceState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddTrackSheet by remember { mutableStateOf(false) }

    // ── RECORD_AUDIO Permission + Push-to-Talk ────────────────────────────────
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var showMicRationale by remember { mutableStateOf(false) }

    /**
     * PTT-Taste gedrückt: Permission prüfen → Mic aktivieren + Music ducken.
     */
    val handlePttPress: () -> Unit = {
        when {
            micPermission.status.isGranted -> {
                // Nur aktivieren wenn noch nicht aktiv (verhindert doppeltes Toggle)
                if (uiState.isMicMuted) viewModel.onEvent(SessionEvent.ToggleMic)
                voiceViewModel.onPttPressed(
                    sessionId = uiState.sessionId ?: uiState.sessionCode,
                    userId = uiState.currentUserId,
                    displayName = displayName
                )
            }
            micPermission.status.shouldShowRationale -> showMicRationale = true
            else -> micPermission.launchPermissionRequest()
        }
    }

    /**
     * PTT-Taste losgelassen: Mic deaktivieren + Music-Ducking aufheben.
     */
    val handlePttRelease: () -> Unit = {
        if (!uiState.isMicMuted) viewModel.onEvent(SessionEvent.ToggleMic)
        voiceViewModel.onPttReleased()
    }

    LaunchedEffect(sessionCode) {
        if (sessionCode.isNotEmpty() && uiState.sessionCode.isEmpty()) {
            viewModel.onEvent(
                SessionEvent.ConnectToExistingSession(
                    sessionCode = sessionCode,
                    isHost = isHost,
                    displayName = displayName
                )
            )
        }
    }

    // ── Mikrofon-Berechtigung Rationale-Dialog ────────────────────────────────
    if (showMicRationale) {
        AlertDialog(
            onDismissRequest = { showMicRationale = false },
            title = { Text("Mikrofon-Zugriff benötigt") },
            text = { Text("SyncJam benötigt Zugriff auf dein Mikrofon, um Voice-Chat zu ermöglichen. Bitte erteile die Berechtigung in den Einstellungen.") },
            confirmButton = {
                TextButton(onClick = {
                    showMicRationale = false
                    micPermission.launchPermissionRequest()
                }) { Text("Erlauben") }
            },
            dismissButton = {
                TextButton(onClick = { showMicRationale = false }) { Text("Abbrechen") }
            }
        )
    }

    // Kicked dialog
    uiState.kickedReason?.let { reason ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(SessionEvent.DismissKicked); onLeave() },
            title = { Text("Aus Session entfernt") },
            text = { Text(reason) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(SessionEvent.DismissKicked); onLeave() }) {
                    Text("OK")
                }
            }
        )
    }

    // Track approval dialog (host only) — shown for each pending approval sequentially
    uiState.pendingApprovals.firstOrNull()?.let { pending ->
        AlertDialog(
            onDismissRequest = { /* force decision */ },
            title = { Text("Track-Anfrage") },
            text = {
                Column {
                    Text("${pending.requestedByName} möchte einen Track hinzufügen:")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${pending.title} — ${pending.artist}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(SessionEvent.ApproveTrack(pending.requestId)) }) {
                    Text("Hinzufügen")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(SessionEvent.RejectTrack(pending.requestId)) }) {
                    Text("Ablehnen")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Column {
                            Text(
                                if (uiState.sessionCode.isNotEmpty()) "Session ${uiState.sessionCode}"
                                else "Jam Session",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!uiState.isConnected) {
                                    Icon(Icons.Default.WifiOff, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                    Text("Verbinde…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                } else {
                                    ConnectionDot()
                                    Spacer(Modifier.width(4.dp))
                                    // +1 for self
                                    val total = uiState.participantCount.coerceAtLeast(uiState.participants.size + 1)
                                    Text(
                                        "$total ${if (total == 1) "Person" else "Personen"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(SessionEvent.LeaveSession); onLeave() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Verlassen")
                        }
                    },
                    actions = {
                        if (uiState.sessionCode.isNotEmpty()) {
                            IconButton(onClick = {
                                val shareText = "Komm in meine SyncJam Session! 🎵\nCode: ${uiState.sessionCode}\nsyncjam://join/${uiState.sessionCode}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    putExtra(Intent.EXTRA_SUBJECT, "SyncJam Session ${uiState.sessionCode}")
                                }
                                context.startActivity(Intent.createChooser(intent, "Session teilen"))
                            }) {
                                Icon(Icons.Default.Share, "Teilen")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                PlayerBottomBar(
                        uiState = uiState,
                        onTogglePlayPause = { viewModel.onEvent(SessionEvent.TogglePlayPause) },
                        onSkip = { uiState.currentTrack?.let { viewModel.onEvent(SessionEvent.SendTrackEnded(it.id)) } },
                        onOpenPlaylist = onOpenPlaylist,
                        onOpenLibrary = { showAddTrackSheet = true },
                        onReaction = { emoji -> viewModel.onEvent(SessionEvent.SendReaction(emoji)) },
                        onMicPress = handlePttPress,
                        onMicRelease = handlePttRelease,
                        onVolumeChange = { viewModel.onEvent(SessionEvent.SetVolume(it)) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))

                AlbumArtSection(
                    isPlaying = uiState.isPlaying,
                    currentTrack = uiState.currentTrack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Spacer(Modifier.height(16.dp))

                AnimatedContent(
                    targetState = uiState.currentTrack,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "track_info"
                ) { track ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (track != null) {
                            Text(
                                track.title,
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                track.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                            if (track.streamUrl != null) {
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "YouTube",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Kein Track aktiv",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Füge einen Track zur Warteschlange hinzu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                uiState.currentTrack?.let { track ->
                    ProgressSection(
                        positionMs = uiState.positionMs,
                        durationMs = track.durationMs,
                        onSeek = { posMs -> viewModel.onEvent(SessionEvent.Seek(posMs)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Waveform reacts to positionMs for music-like feel
                AnimatedVisibility(visible = uiState.currentTrack != null) {
                    Column {
                        AudioWaveformVisualizer(
                            isPlaying = uiState.isPlaying,
                            positionMs = uiState.positionMs,
                            trackId = uiState.currentTrack?.id ?: "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                if (uiState.participants.isNotEmpty()) {
                    // Merge speaking state from voice into participants
                    val speakingIds = voiceState.participants
                        .filter { it.isSpeaking }
                        .map { vp ->
                            // In stub mode sid = "local-$userId", in real LiveKit identity = userId
                            if (vp.sid.startsWith("local-")) vp.sid.removePrefix("local-") else vp.sid
                        }
                        .toSet()
                    val mergedParticipants = uiState.participants
                        .map { p ->
                            val speaking = speakingIds.contains(p.userId) ||
                                (voiceState.isSpeaking && voiceState.participants.any { it.isLocal && (it.sid == "local-${p.userId}" || it.sid == p.userId) })
                            if (speaking != p.isSpeaking) p.copy(isSpeaking = speaking) else p
                        }
                        .toImmutableList()
                    ParticipantsSection(
                        participants = mergedParticipants,
                        isCurrentUserAdmin = uiState.isCurrentUserAdmin,
                        onAdminEvent = { viewModel.onEvent(it) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                uiState.error?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Floating emoji reactions — rendered on top of everything
        FloatingReactionOverlay(reactions = uiState.floatingReactions)

        // Floating speaking-avatar — top-end corner, only when voice is active
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            VoiceOverlay(voiceState = voiceState)
        }

        // Add Track Sheet
        if (showAddTrackSheet) {
            SessionAddTrackSheet(
                libraryViewModel = libraryViewModel,
                onAddTrack = { track ->
                    viewModel.onEvent(
                        SessionEvent.AddLocalTrackToQueue(
                            trackId = track.id,
                            title = track.title,
                            artist = track.artist,
                            durationMs = track.durationMs,
                            contentUri = track.contentUri,
                            albumArtUri = track.albumArtUri
                        )
                    )
                },
                onDismiss = { showAddTrackSheet = false }
            )
        }

        // Direct message notification
        uiState.directMessage?.let { dm ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Mail, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dm.fromName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(dm.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { viewModel.onEvent(SessionEvent.DismissDirectMessage) }) {
                            Text("×", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    }
}

// ── Floating Reactions ────────────────────────────────────────────────────────

@Composable
private fun FloatingReactionOverlay(
    reactions: kotlinx.collections.immutable.ImmutableList<FloatingReactionUi>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        reactions.forEach { reaction ->
            key(reaction.id) {
                FloatingEmoji(reaction = reaction)
            }
        }
    }
}

@Composable
private fun FloatingEmoji(reaction: FloatingReactionUi) {
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val isEmojiOnly = reaction.emoji.length <= 4

    LaunchedEffect(reaction.id) {
        offsetY.animateTo(-600f, tween(reaction.durationMs.toInt(), easing = EaseOut))
    }
    LaunchedEffect(reaction.id) {
        delay((reaction.durationMs - 700L).coerceAtLeast(0L))
        alpha.animateTo(0f, tween(700))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
    ) {
        if (isEmojiOnly) {
            Text(
                text = reaction.emoji,
                fontSize = 36.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset { IntOffset((reaction.xFraction * 900).roundToInt(), offsetY.value.roundToInt()) }
            )
        } else {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset { IntOffset((reaction.xFraction * 500).roundToInt(), offsetY.value.roundToInt()) },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = reaction.emoji,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Bottom Bar ────────────────────────────────────────────────────────────────

@Composable
private fun PlayerBottomBar(
    uiState: SessionUiState,
    onTogglePlayPause: () -> Unit,
    onSkip: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onOpenLibrary: () -> Unit,
    onReaction: (String) -> Unit,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var textInput by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }
    val submitText = {
        val trimmed = textInput.trim()
        if (trimmed.isNotBlank()) {
            onReaction(trimmed)
            textInput = ""
            focusManager.clearFocus()
            showTextInput = false
        }
    }

    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Text reaction input — shown only when toggled
            AnimatedVisibility(visible = showTextInput) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { if (it.length <= 100) textInput = it },
                            placeholder = { Text("Text senden…", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitText() }),
                            shape = RoundedCornerShape(24.dp)
                        )
                        FilledTonalIconButton(
                            onClick = submitText,
                            modifier = Modifier.size(52.dp),
                            enabled = textInput.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Senden", Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("❤️", "🔥", "👏", "😂", "🎵", "🎸").forEach { emoji ->
                    Surface(
                        onClick = { onReaction(emoji) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(emoji, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                }
                // Toggle text input button
                Surface(
                    onClick = {
                        showTextInput = !showTextInput
                        if (!showTextInput) {
                            textInput = ""
                            focusManager.clearFocus()
                        }
                    },
                    shape = CircleShape,
                    color = if (showTextInput) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("Aa", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = uiState.musicVolume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    "${(uiState.musicVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MuteButton(
                    isMuted = uiState.isMicMuted,
                    onPress = onMicPress,
                    onRelease = onMicRelease,
                    size = 52.dp
                )

                FilledTonalIconButton(
                    onClick = onSkip,
                    modifier = Modifier.size(52.dp),
                    enabled = uiState.currentTrack != null
                ) {
                    Icon(Icons.Default.SkipNext, "Überspringen", Modifier.size(26.dp))
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(68.dp),
                    enabled = uiState.currentTrack != null
                ) {
                    Icon(
                        if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (uiState.isPlaying) "Pausieren" else "Abspielen",
                        Modifier.size(34.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onOpenLibrary,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.MusicNote, "Bibliothek", Modifier.size(22.dp))
                }

                FilledTonalButton(
                    onClick = onOpenPlaylist,
                    modifier = Modifier.height(52.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (uiState.playlist.isNotEmpty()) {
                                Badge { Text("${uiState.playlist.size}") }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Queue")
                }
            }
        }
    }
}

// ── Album Art ─────────────────────────────────────────────────────────────────

@Composable
private fun AlbumArtSection(
    isPlaying: Boolean,
    currentTrack: CurrentTrackUi?,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.94f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "scale"
    )
    val shadowElevation by animateFloatAsState(
        targetValue = if (isPlaying) 28f else 6f,
        animationSpec = tween(400),
        label = "shadow"
    )
    // Subtle breathing pulse when playing
    val breathTransition = rememberInfiniteTransition(label = "breathe")
    val breathScale by breathTransition.animateFloat(
        initialValue = 1f, targetValue = 1.015f, label = "breath",
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    val albumArtUri = remember(currentTrack?.id, currentTrack?.albumArtUri) {
        when {
            // Prefer the uploaded Supabase URL (works for both host and guest)
            currentTrack?.albumArtUri != null -> Uri.parse(currentTrack.albumArtUri)
            // Fallback: derive from local MediaStore (host-only)
            currentTrack?.id?.toLongOrNull() != null ->
                Uri.parse("content://media/external/audio/media/${currentTrack.id}/albumart")
            else -> null
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val cardScale = scale * if (isPlaying) breathScale else 1f
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
                .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
                .shadow(shadowElevation.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
        ) {
            when {
                albumArtUri != null -> {
                    AsyncImage(
                        model = albumArtUri,
                        contentDescription = currentTrack?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = null,
                        onError = null
                    )
                    // If art fails to load, Box below acts as fallback via background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.55f),
                                        tertiaryColor.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                    // Re-draw art on top of gradient so it shows when loaded
                    AsyncImage(
                        model = albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    // Gradient fallback
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.75f),
                                        tertiaryColor.copy(alpha = 0.85f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.38f),
                            tint = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Dark overlay when paused
            if (!isPlaying && currentTrack != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.30f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Idle state (no track)
            if (currentTrack == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(surfaceVariantColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.35f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ── Waveform ──────────────────────────────────────────────────────────────────

@Composable
private fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    positionMs: Long,
    trackId: String,
    modifier: Modifier = Modifier
) {
    val barCount = 36
    // animTime only ticks when playing — freezes completely when stopped
    var animTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(16L)
            animTime = (animTime + 0.02667f) % 1f
        }
    }
    val amplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.06f,
        animationSpec = tween(350),
        label = "amplitude"
    )

    // Deterministic "fingerprint" per track — each track has unique bar shape
    val trackSeed = remember(trackId) { trackId.hashCode().toLong() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 1.55f)
        val spacing = barWidth * 0.55f
        val totalBarWidth = barWidth + spacing
        val maxBarHeight = size.height * 0.95f
        val centerY = size.height / 2

        for (i in 0 until barCount) {
            val x = i * totalBarWidth + spacing / 2

            // 3-layer waveform: fast animation + slow position drift + track fingerprint
            val phase = animTime * 2f * PI.toFloat()
            // Position-driven slow drift — changes every 250ms for "beat" feel
            val posDrift = (positionMs / 250L).toFloat() * 0.28f
            // Track-unique static shape based on bar position
            val trackShape = sin((trackSeed % 100 + i * 1.3f).toFloat()) * 0.25f

            val w1 = sin(phase * 1.0f + i * 0.42f + posDrift)
            val w2 = sin(phase * 2.3f + i * 0.31f + posDrift * 1.5f) * 0.55f
            val w3 = sin(phase * 0.6f + i * 0.71f + posDrift * 0.7f) * 0.35f
            // Beat pulse: spikes every 500ms tied to positionMs
            val beat = if ((positionMs / 500L % 2L) == 0L)
                sin(i * 0.85f + posDrift) * 0.3f else 0f

            val raw = (w1 + w2 + w3 + beat + trackShape + 2.5f) / 5.0f
            val normalizedHeight = raw.coerceIn(0.04f, 1f)
            val barHeight = normalizedHeight * maxBarHeight * amplitude

            // Color gradient: low bars = surfaceVariant, high bars = primary/secondary
            val t = (normalizedHeight * amplitude).coerceIn(0f, 1f)
            val barColor = when {
                t < 0.5f -> androidx.compose.ui.graphics.lerp(surfaceVariantColor, secondaryColor, t * 2f)
                else -> androidx.compose.ui.graphics.lerp(secondaryColor, primaryColor, (t - 0.5f) * 2f)
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, centerY - barHeight / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}

// ── Connection dot ────────────────────────────────────────────────────────────

@Composable
private fun ConnectionDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f, label = "dot_alpha",
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
}

// ── Progress ──────────────────────────────────────────────────────────────────

@Composable
private fun ProgressSection(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val serverProgress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    var dragProgress by remember { mutableFloatStateOf(-1f) }
    val displayProgress = if (dragProgress >= 0f) dragProgress else serverProgress
    val displayMs = if (dragProgress >= 0f) (dragProgress * durationMs).toLong() else positionMs

    Column(modifier = modifier) {
        Slider(
            value = displayProgress,
            onValueChange = { dragProgress = it },
            onValueChangeFinished = {
                if (dragProgress >= 0f) {
                    onSeek((dragProgress * durationMs).toLong())
                    dragProgress = -1f
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatMs(displayMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMs(durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Participants ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParticipantsSection(
    participants: kotlinx.collections.immutable.ImmutableList<ParticipantUi>,
    isCurrentUserAdmin: Boolean,
    onAdminEvent: (SessionEvent) -> Unit
) {
    var selectedParticipant by remember { mutableStateOf<ParticipantUi?>(null) }
    var dmTarget by remember { mutableStateOf<ParticipantUi?>(null) }
    var dmText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Session timer ──────────────────────────────────────────────────────────
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSeconds++
        }
    }
    val timeString = remember(elapsedSeconds) {
        "%02d:%02d:%02d".format(
            elapsedSeconds / 3600,
            (elapsedSeconds % 3600) / 60,
            elapsedSeconds % 60
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Teilnehmer",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            // Session elapsed timer
            Text(
                timeString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            participants.take(5).forEach { participant ->
                ParticipantChip(
                    participant = participant,
                    onClick = if (isCurrentUserAdmin) ({ selectedParticipant = participant }) else null
                )
            }
            if (participants.size > 5) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "+${participants.size - 5}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Admin action sheet
    selectedParticipant?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { selectedParticipant = null },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    target.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                HorizontalDivider()
                AdminActionItem(
                    icon = Icons.Default.Mail,
                    label = "Privatnachricht senden",
                    onClick = { dmTarget = target; selectedParticipant = null }
                )
                AdminActionItem(
                    icon = if (target.mutedByAdmin) Icons.Default.Mic else Icons.Default.MicOff,
                    label = if (target.mutedByAdmin) "Stummschaltung aufheben" else "Stummschalten",
                    onClick = { onAdminEvent(SessionEvent.MuteUser(target.userId, !target.mutedByAdmin)); selectedParticipant = null }
                )
                AdminActionItem(
                    icon = Icons.Default.AdminPanelSettings,
                    label = "Admin-Rechte übertragen",
                    onClick = { onAdminEvent(SessionEvent.TransferAdmin(target.userId)); selectedParticipant = null }
                )
                HorizontalDivider()
                AdminActionItem(
                    icon = Icons.Default.PersonOff,
                    label = "Kicken",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { onAdminEvent(SessionEvent.KickUser(target.userId)); selectedParticipant = null }
                )
                AdminActionItem(
                    icon = Icons.Default.Block,
                    label = "Bannen",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { onAdminEvent(SessionEvent.BanUser(target.userId)); selectedParticipant = null }
                )
            }
        }
    }

    // DM dialog
    dmTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { dmTarget = null; dmText = "" },
            title = { Text("Nachricht an ${target.displayName}") },
            text = {
                OutlinedTextField(
                    value = dmText,
                    onValueChange = { if (it.length <= 200) dmText = it },
                    label = { Text("Nachricht") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onAdminEvent(SessionEvent.SendDirectMessage(target.userId, dmText)); dmTarget = null; dmText = "" },
                    enabled = dmText.isNotBlank()
                ) { Text("Senden") }
            },
            dismissButton = {
                TextButton(onClick = { dmTarget = null; dmText = "" }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun AdminActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
        }
    }
}

@Composable
private fun ParticipantChip(
    participant: ParticipantUi,
    onClick: (() -> Unit)? = null
) {
    val chipColor = when {
        participant.isAdmin -> MaterialTheme.colorScheme.tertiaryContainer
        participant.isHost -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        participant.isAdmin -> MaterialTheme.colorScheme.onTertiaryContainer
        participant.isHost -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val speakingBorderAlpha by animateFloatAsState(
        targetValue = if (participant.isSpeaking) 1f else 0f,
        animationSpec = tween(200),
        label = "speaking_border"
    )
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(16.dp),
        color = chipColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Outer green speaking ring
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = speakingBorderAlpha))
                )
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (participant.avatarUrl != null) {
                        AsyncImage(
                            model = participant.avatarUrl,
                            contentDescription = participant.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            participant.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
                // Host crown badge — top-centre above the avatar
                if (participant.isHost) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Host",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (-4).dp)
                    )
                }
            }
            Text(
                participant.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = if (participant.isHost || participant.isAdmin) FontWeight.SemiBold else FontWeight.Normal
            )
            if (participant.mutedByAdmin) {
                Icon(Icons.Default.MicOff, null, modifier = Modifier.size(12.dp), tint = contentColor.copy(alpha = 0.6f))
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

// ── Add Track Sheet ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionAddTrackSheet(
    libraryViewModel: com.syncjam.app.feature.library.presentation.LibraryViewModel,
    onAddTrack: (com.syncjam.app.feature.library.presentation.TrackUi) -> Unit,
    onDismiss: () -> Unit
) {
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var expandedPlaylistId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    // Playlist tracks — collected reactively when a playlist is expanded
    val expandedPlaylistTracks by produceState<List<com.syncjam.app.feature.library.presentation.TrackUi>>(
        initialValue = emptyList(),
        key1 = expandedPlaylistId
    ) {
        val id = expandedPlaylistId
        if (id != null) {
            libraryViewModel.getPlaylistTracksFlow(id).collect { value = it }
        } else {
            value = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        if (libraryState.tracks.isEmpty()) libraryViewModel.scanLibrary()
    }

    val filteredTracks = remember(libraryState.tracks, searchQuery) {
        if (searchQuery.isBlank()) libraryState.tracks
        else libraryState.tracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredFavorites = remember(libraryState.favorites, searchQuery) {
        if (searchQuery.isBlank()) libraryState.favorites
        else libraryState.favorites.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Track hinzufügen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { libraryViewModel.scanLibrary() }) {
                    if (libraryState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.Refresh, "Aktualisieren")
                    }
                }
            }

            // Search bar (Tracks + Favoriten)
            if (selectedTab < 2) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Suchen…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.MusicNote, null, Modifier.size(18.dp)) },
                    text = { Text("Alle") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Favorite, null, Modifier.size(18.dp)) },
                    text = { Text("Favoriten") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; searchQuery = "" },
                    icon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, Modifier.size(18.dp)) },
                    text = { Text("Playlists") }
                )
            }

            when (selectedTab) {
                0 -> AddTrackPickerList(
                    tracks = filteredTracks,
                    isLoading = libraryState.isLoading,
                    searchQuery = searchQuery,
                    onRefresh = { libraryViewModel.scanLibrary() },
                    onAddTrack = { onAddTrack(it); onDismiss() }
                )
                1 -> AddTrackPickerList(
                    tracks = filteredFavorites,
                    isLoading = libraryState.isLoading,
                    searchQuery = searchQuery,
                    onRefresh = { libraryViewModel.scanLibrary() },
                    onAddTrack = { onAddTrack(it); onDismiss() },
                    emptyMessage = "Keine Favoriten vorhanden"
                )
                2 -> AddPlaylistPickerList(
                    playlists = libraryState.playlists,
                    expandedPlaylistId = expandedPlaylistId,
                    expandedPlaylistTracks = expandedPlaylistTracks,
                    onTogglePlaylist = { id ->
                        expandedPlaylistId = if (expandedPlaylistId == id) null else id
                    },
                    onAddTrack = { onAddTrack(it); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun AddTrackPickerList(
    tracks: List<com.syncjam.app.feature.library.presentation.TrackUi>,
    isLoading: Boolean,
    searchQuery: String,
    onRefresh: () -> Unit,
    onAddTrack: (com.syncjam.app.feature.library.presentation.TrackUi) -> Unit,
    emptyMessage: String = "Keine Tracks gefunden"
) {
    if (isLoading && tracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (tracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (searchQuery.isNotEmpty()) "Kein Ergebnis für \"$searchQuery\""
                    else emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (searchQuery.isEmpty()) {
                    androidx.compose.material3.Button(onClick = onRefresh) { Text("Bibliothek scannen") }
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
    ) {
        val grouped = tracks.groupBy { it.artist }.toSortedMap()
        grouped.forEach { (artist, artistTracks) ->
            item(key = "artist_$artist") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        artist.ifBlank { "Unbekannter Künstler" },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${artistTracks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(artistTracks, key = { it.id }, contentType = { "track" }) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddTrack(track) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track.albumArtUri != null) {
                            AsyncImage(
                                model = track.albumArtUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            track.durationDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (track.isFavorite) {
                        Icon(Icons.Default.Favorite, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Icon(Icons.Default.Add, "Hinzufügen", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
            }
        }
    }
}

@Composable
private fun AddPlaylistPickerList(
    playlists: kotlinx.collections.immutable.ImmutableList<com.syncjam.app.feature.library.presentation.PlaylistUi>,
    expandedPlaylistId: String?,
    expandedPlaylistTracks: List<com.syncjam.app.feature.library.presentation.TrackUi>,
    onTogglePlaylist: (String) -> Unit,
    onAddTrack: (com.syncjam.app.feature.library.presentation.TrackUi) -> Unit
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Keine Playlists vorhanden", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
        items(playlists, key = { it.id }, contentType = { "playlist" }) { playlist ->
            val isExpanded = expandedPlaylistId == playlist.id
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTogglePlaylist(playlist.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${playlist.trackCount} Tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isExpanded) {
                    if (expandedPlaylistTracks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Keine Tracks in dieser Playlist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        expandedPlaylistTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAddTrack(track) }
                                    .padding(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (track.albumArtUri != null) {
                                        AsyncImage(
                                            model = track.albumArtUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.Default.Add, "Hinzufügen", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 74.dp))
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
