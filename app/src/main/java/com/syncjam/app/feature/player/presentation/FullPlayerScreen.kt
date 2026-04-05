package com.syncjam.app.feature.player.presentation

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.filled.KeyboardArrowDown

/**
 * Full-screen player with:
 * - Vinyl rotation animation (pauses when not playing, resumes from current angle)
 * - basicMarquee for long track titles
 * - Haptic feedback on play/pause/skip
 * - Glassmorphism background (API 31+, fallback semi-transparent)
 * - Swipe-down dismiss gesture
 */
@Composable
fun FullPlayerScreen(
    trackTitle: String,
    trackArtist: String,
    albumArtUri: String?,
    isPlaying: Boolean,
    playbackProgress: Float,        // 0f..1f
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // ── Swipe-down dismiss ────────────────────────────────────────────────────
    val swipeOffset = remember { Animatable(0f) }

    // ── Vinyl rotation ────────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "rotation"
    )
    var lastRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) lastRotation = rotation
    }

    // ── Glassmorphism / fallback background ───────────────────────────────────
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = android.graphics.RenderEffect
                .createBlurEffect(25f, 25f, android.graphics.Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
    } else {
        Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        if (dragAmount.y > 0) {
                            coroutineScope.launch {
                                swipeOffset.snapTo(swipeOffset.value + dragAmount.y)
                            }
                        }
                    },
                    onDragEnd = {
                        if (swipeOffset.value > 200f) {
                            onDismiss()
                        } else {
                            coroutineScope.launch {
                                swipeOffset.animateTo(0f, spring())
                            }
                        }
                    }
                )
            }
            .graphicsLayer {
                translationY = swipeOffset.value
                alpha = 1f - (swipeOffset.value / 400f).coerceIn(0f, 1f)
            }
    ) {
        // Background blur layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(blurModifier)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drag handle / close button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Schließen",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Vinyl Disc ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // Outer vinyl ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = if (isPlaying) rotation else lastRotation }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    // Album art fills disc
                    if (albumArtUri != null) {
                        AsyncImage(
                            model = albumArtUri,
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    // Inner circle overlay (label area)
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.2f)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Track info ────────────────────────────────────────────────────
            Text(
                text = trackTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE),
                maxLines = 1,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Clip
            )
            Text(
                text = trackArtist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Progress slider ───────────────────────────────────────────────
            Slider(
                value = playbackProgress,
                onValueChange = onSeek,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Playback controls ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSkipPrevious()
                    }
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Vorheriger Track",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Play/Pause button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayPause()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSkipNext()
                    }
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Nächster Track",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
