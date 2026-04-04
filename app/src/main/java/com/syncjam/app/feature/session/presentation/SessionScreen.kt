package com.syncjam.app.feature.session.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    sessionCode: String = "",
    isHost: Boolean = false,
    onLeave: () -> Unit,
    onOpenPlaylist: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionCode) {
        if (sessionCode.isNotEmpty() && uiState.sessionCode.isEmpty()) {
            viewModel.onEvent(
                SessionEvent.ConnectToExistingSession(sessionCode = sessionCode, isHost = isHost)
            )
        }
    }

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
                                Text(
                                    "${uiState.participantCount} ${if (uiState.participantCount == 1) "Person" else "Personen"}",
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

            // Album Art with glow + vinyl effect
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

            // Track info
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

            // Progress slider
            uiState.currentTrack?.let { track ->
                ProgressSection(
                    positionMs = uiState.positionMs,
                    durationMs = track.durationMs,
                    onSeek = { posMs -> viewModel.onEvent(SessionEvent.Seek(posMs)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            // Waveform visualizer
            if (uiState.isPlaying || uiState.currentTrack != null) {
                AudioWaveformVisualizer(
                    isPlaying = uiState.isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            // Participants
            if (uiState.participants.isNotEmpty()) {
                ParticipantsSection(participants = uiState.participants)
                Spacer(Modifier.height(8.dp))
            }

            // Error
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
}

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
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            // Emoji reactions
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
                    if (emoji != "🎸") Spacer(Modifier.width(4.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            // Volume slider
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
            // Playback controls + mic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic / PTT button
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
                        tint = if (uiState.isMicMuted)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.error
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
                    enabled = uiState.currentTrack != null || uiState.isConnected
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
        // Glow effect behind
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Vinyl / album art circle
        Box(
            modifier = Modifier
                .fillMaxSize(0.82f)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    if (isPlaying) this.rotationZ = rotation
                }
                .shadow(if (isPlaying) 24.dp else 8.dp, CircleShape)
                .clip(CircleShape)
                .background(surfaceVariantColor),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl grooves drawn with Canvas
            val grooveColor = primaryColor.copy(alpha = 0.08f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                val maxR = minOf(size.width, size.height) / 2
                for (i in 1..8) {
                    val r = maxR * (0.4f + i * 0.07f)
                    drawCircle(
                        color = grooveColor,
                        radius = r,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }
            }

            // Center label
            Box(
                modifier = Modifier
                    .fillMaxSize(0.4f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor,
                                primaryColor.copy(alpha = 0.7f)
                            )
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

        // Center spindle dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                }
        )
    }
}

@Composable
private fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 32
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f, label = "time",
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart)
    )
    val amplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.15f,
        animationSpec = tween(400),
        label = "amplitude"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 1.5f)
        val spacing = barWidth * 0.5f
        val totalBarWidth = barWidth + spacing
        val maxBarHeight = size.height * 0.9f
        val centerY = size.height / 2

        for (i in 0 until barCount) {
            val x = i * totalBarWidth + spacing / 2

            // Multiple sine waves for organic look
            val phase = time * 2f * Math.PI.toFloat()
            val wave1 = sin(phase + i * 0.4f)
            val wave2 = sin(phase * 1.7f + i * 0.3f) * 0.5f
            val wave3 = sin(phase * 0.5f + i * 0.6f) * 0.3f
            val normalizedHeight = ((wave1 + wave2 + wave3 + 1.8f) / 3.6f).coerceIn(0.05f, 1f)
            val barHeight = normalizedHeight * maxBarHeight * amplitude

            val barColor = androidx.compose.ui.graphics.lerp(
                surfaceVariantColor,
                primaryColor,
                normalizedHeight * amplitude
            )

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, centerY - barHeight / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}

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
