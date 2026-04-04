package com.syncjam.app.feature.voice.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syncjam.app.feature.voice.domain.model.VoiceConnectionState
import com.syncjam.app.feature.voice.domain.model.VoiceState
import com.syncjam.app.feature.voice.presentation.components.MuteButton
import com.syncjam.app.feature.voice.presentation.components.VoiceParticipantChip

/**
 * Floating-Overlay das über dem PlayerBottomBar angezeigt wird,
 * wenn Voice aktiv ist (Stub-Modus oder echt).
 *
 * Enthält:
 * - Status-Label (Connecting / Voice aktiv / Voice Demo)
 * - Teilnehmer-Chips mit Speaking-Indikator
 * - Push-to-Talk Button (halten zum Sprechen)
 */
@Composable
fun VoiceOverlay(
    voiceState: VoiceState,
    onPttPress: () -> Unit,
    onPttRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = voiceState.isActive,
        enter = slideInVertically(tween(280)) { it } + fadeIn(tween(280)),
        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.97f),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val statusLabel = when (voiceState.connectionState) {
                        VoiceConnectionState.Connecting -> "Verbinde…"
                        VoiceConnectionState.Connected  -> "Voice aktiv"
                        VoiceConnectionState.StubMode   -> "Voice (Demo)"
                        is VoiceConnectionState.Error   -> "Fehler"
                        VoiceConnectionState.Disconnected -> ""
                    }
                    val statusColor = when (voiceState.connectionState) {
                        is VoiceConnectionState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    MuteButton(
                        isMuted = voiceState.isMicMuted,
                        onPress = onPttPress,
                        onRelease = onPttRelease,
                        size = 40.dp
                    )
                }

                if (voiceState.participants.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(
                            items = voiceState.participants,
                            key = { it.sid }
                        ) { participant ->
                            VoiceParticipantChip(participant = participant)
                        }
                    }
                }
            }
        }
    }
}
