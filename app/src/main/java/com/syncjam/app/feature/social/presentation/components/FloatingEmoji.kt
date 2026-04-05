package com.syncjam.app.feature.social.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syncjam.app.feature.social.domain.model.Reaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A single floating emoji that animates upward and fades out over 2 seconds.
 *
 * @param reaction      The reaction data (emoji, sender, position).
 * @param screenWidth   Current screen width in dp — used to convert the relative [Reaction.x]
 *                      position (0.0–1.0) to an actual horizontal offset.
 * @param onComplete    Called with [Reaction.id] once the animation finishes so the caller
 *                      can remove the reaction from the active list.
 */
@Composable
fun FloatingEmoji(
    reaction: Reaction,
    screenWidth: Float,
    onComplete: (Long) -> Unit
) {
    val yAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(reaction.id) {
        launch {
            yAnim.animateTo(
                targetValue = -800f,
                animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 800,
                    delayMillis = 1200,
                    easing = LinearEasing
                )
            )
        }
        delay(2000L)
        onComplete(reaction.id)
    }

    Box(
        modifier = Modifier
            .offset(x = (reaction.x * screenWidth).dp, y = 0.dp)
            .graphicsLayer {
                translationY = yAnim.value
                alpha = alphaAnim.value
            }
            .pointerInput(reaction.id) {
                detectTapGestures(onLongPress = { showTooltip = true })
            }
    ) {
        Text(
            text = reaction.emoji,
            fontSize = 32.sp
        )

        if (showTooltip) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.offset(y = (-36).dp)
            ) {
                Text(
                    text = reaction.senderName,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
