package com.syncjam.app.feature.session.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    sessionCode: String = "",
    isHost: Boolean = false,
    displayName: String = "",
    onLeave: () -> Unit,
    onOpenPlaylist: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                            IconButton(onClick = {}) {
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
                    onReaction = { emoji -> viewModel.onEvent(SessionEvent.SendReaction(emoji)) },
                    onToggleMic = { viewModel.onEvent(SessionEvent.ToggleMic) },
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
                    isYoutube = uiState.currentTrack?.streamUrl != null,
                    hasTrack = uiState.currentTrack != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(16.dp)
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
                    ParticipantsSection(participants = uiState.participants)
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
    onReaction: (String) -> Unit,
    onToggleMic: () -> Unit,
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
                FilledTonalIconButton(
                    onClick = onToggleMic,
                    modifier = Modifier.size(52.dp),
                    colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (uiState.isMicMuted)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        if (uiState.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        if (uiState.isMicMuted) "Mikrofon ein" else "Stummschalten",
                        Modifier.size(24.dp),
                        tint = if (uiState.isMicMuted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.error
                    )
                }

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
    isYoutube: Boolean,
    hasTrack: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f, label = "rotate",
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart)
    )
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.88f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "scale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.5f else 0.15f,
        animationSpec = tween(500),
        label = "glow"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(
                    Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = glowAlpha), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize(0.82f)
                .graphicsLayer {
                    scaleX = scale; scaleY = scale
                    if (isPlaying) rotationZ = rotation
                }
                .shadow(if (isPlaying) 24.dp else 8.dp, CircleShape)
                .clip(CircleShape)
                .background(surfaceVariantColor),
            contentAlignment = Alignment.Center
        ) {
            val grooveColor = primaryColor.copy(alpha = 0.08f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                val maxR = minOf(size.width, size.height) / 2
                for (i in 1..8) {
                    val r = maxR * (0.4f + i * 0.07f)
                    drawCircle(
                        color = grooveColor, radius = r, center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(0.4f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isYoutube) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.5f),
                    tint = Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .graphicsLayer { scaleX = scale; scaleY = scale }
        )
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
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    // Fast animation cycle for live-feel
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f, label = "time",
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Restart)
    )
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

@Composable
private fun ParticipantsSection(participants: kotlinx.collections.immutable.ImmutableList<ParticipantUi>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Teilnehmer",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            participants.take(5).forEach { participant ->
                ParticipantChip(participant = participant)
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
}

@Composable
private fun ParticipantChip(participant: ParticipantUi) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (participant.isHost) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (participant.isHost) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    participant.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (participant.isHost) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                participant.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = if (participant.isHost) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (participant.isHost) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
