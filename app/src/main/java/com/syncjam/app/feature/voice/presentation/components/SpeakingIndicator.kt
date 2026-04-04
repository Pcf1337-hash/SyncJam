package com.syncjam.app.feature.voice.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wrapper der einen pulsierenden Rand anzeigt, wenn [isSpeaking] true ist.
 * Animiert via graphicsLayer für GPU-beschleunigten Scale-Effekt.
 */
@Composable
fun SpeakingIndicator(
    isSpeaking: Boolean,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speaking_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(
                if (isSpeaking) Modifier
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .clip(CircleShape)
    ) {
        content()
    }
}
