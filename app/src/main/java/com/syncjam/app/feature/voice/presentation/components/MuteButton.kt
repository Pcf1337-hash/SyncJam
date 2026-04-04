package com.syncjam.app.feature.voice.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Push-to-Talk Mic-Button.
 *
 * - Gedrückt halten → [onPress] → Mikrofon aktiv (Rot/groß)
 * - Loslassen        → [onRelease] → Mikrofon stumm (Neutral/klein)
 *
 * Kein Toggle: Sprache nur solange aktiv, wie der Button gehalten wird.
 */
@Composable
fun MuteButton(
    isMuted: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    val containerColor by animateColorAsState(
        targetValue = if (isMuted)
            MaterialTheme.colorScheme.surfaceVariant
        else
            MaterialTheme.colorScheme.errorContainer,
        animationSpec = tween(durationMillis = 120),
        label = "ptt_container_color"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isMuted)
            MaterialTheme.colorScheme.onSurfaceVariant
        else
            MaterialTheme.colorScheme.error,
        animationSpec = tween(durationMillis = 120),
        label = "ptt_icon_color"
    )
    val scale by animateFloatAsState(
        targetValue = if (isMuted) 1f else 1.15f,
        animationSpec = tween(durationMillis = 100),
        label = "ptt_scale"
    )

    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isMuted) "Halten zum Sprechen" else "Spricht…",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
