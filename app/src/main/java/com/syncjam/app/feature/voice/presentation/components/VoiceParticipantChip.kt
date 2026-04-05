package com.syncjam.app.feature.voice.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.syncjam.app.feature.voice.domain.model.ConnectionQuality
import com.syncjam.app.feature.voice.domain.model.VoiceParticipant

/**
 * Kleiner Chip der einen Voice-Teilnehmer anzeigt:
 * Avatar + pulsierender Speaking-Indikator + Name + Stumm-Badge + Netzwerkqualitäts-Balken.
 */
@Composable
fun VoiceParticipantChip(
    participant: VoiceParticipant,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            SpeakingIndicator(
                isSpeaking = participant.isSpeaking,
                size = 28.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = if (participant.isLocal)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (participant.isLocal)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(7.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (participant.isLocal) "${participant.displayName} (Du)"
                               else participant.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(4.dp))
                    NetworkQualityBars(quality = participant.connectionQuality)
                }
                if (participant.isMuted) {
                    Spacer(Modifier.height(1.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(9.dp)
                        )
                        Text(
                            text = "stumm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3-Balken Netzwerkqualitäts-Indikator (wie WLAN-Symbol).
 *
 * Balken-Höhen: 4dp / 7dp / 10dp (klein → mittel → groß).
 * Farben:
 *   EXCELLENT → alle 3 Balken in [MaterialTheme.colorScheme.tertiary] (grün im Dark Theme)
 *   GOOD      → 2 Balken in [MaterialTheme.colorScheme.secondary] (gelb/amber)
 *   POOR      → 1 Balken in [MaterialTheme.colorScheme.error] (rot)
 *   LOST      → alle Balken in [MaterialTheme.colorScheme.error] (rot, ausgeblendet)
 *   UNKNOWN   → alle Balken in [MaterialTheme.colorScheme.outline] (grau)
 */
@Composable
private fun NetworkQualityBars(
    quality: ConnectionQuality,
    modifier: Modifier = Modifier
) {
    val activeColor = when (quality) {
        ConnectionQuality.EXCELLENT -> MaterialTheme.colorScheme.tertiary
        ConnectionQuality.GOOD      -> MaterialTheme.colorScheme.secondary
        ConnectionQuality.POOR,
        ConnectionQuality.LOST      -> MaterialTheme.colorScheme.error
        ConnectionQuality.UNKNOWN   -> MaterialTheme.colorScheme.outline
    }
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant

    // Anzahl aktiver (farbiger) Balken
    val activeBars = when (quality) {
        ConnectionQuality.EXCELLENT -> 3
        ConnectionQuality.GOOD      -> 2
        ConnectionQuality.POOR      -> 1
        ConnectionQuality.LOST      -> 0
        ConnectionQuality.UNKNOWN   -> 0
    }

    val barHeights = listOf(4.dp, 7.dp, 10.dp)
    val barWidth = 3.dp
    val barSpacing = 1.5.dp

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        modifier = modifier
    ) {
        barHeights.forEachIndexed { index, barHeight ->
            val isActive = index < activeBars
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .background(
                        color = if (isActive) activeColor else inactiveColor,
                        shape = RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp)
                    )
            )
        }
    }
}
