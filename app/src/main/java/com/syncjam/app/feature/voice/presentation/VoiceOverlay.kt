package com.syncjam.app.feature.voice.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.syncjam.app.feature.voice.domain.model.VoiceState
import com.syncjam.app.feature.voice.presentation.components.SpeakingIndicator

/**
 * Floating Avatar-Overlay — taucht auf wenn jemand spricht.
 *
 * Zeigt den Avatar des gerade sprechenden Teilnehmers mit pulsierendem Ring +
 * Name-Label. Kein Bottom-Bar, kein Mute-Button hier — der Mute-Button liegt
 * im PlayerBottomBar.
 */
@Composable
fun VoiceOverlay(
    voiceState: VoiceState,
    modifier: Modifier = Modifier
) {
    // Prefer a speaking remote participant; fall back to local if local is speaking.
    val speakingParticipant = voiceState.participants.firstOrNull { it.isSpeaking && !it.isLocal }
        ?: voiceState.participants.firstOrNull { it.isSpeaking }

    val visible = voiceState.isActive && speakingParticipant != null

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f),
        exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f),
        modifier = modifier
    ) {
        speakingParticipant?.let { participant ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                SpeakingIndicator(isSpeaking = true, size = 48.dp) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (participant.isLocal)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.tertiary,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (participant.isLocal)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = if (participant.isLocal) "Du" else participant.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
