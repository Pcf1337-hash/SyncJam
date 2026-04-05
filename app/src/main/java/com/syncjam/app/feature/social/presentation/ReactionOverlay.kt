package com.syncjam.app.feature.social.presentation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.syncjam.app.feature.social.domain.model.Reaction
import com.syncjam.app.feature.social.presentation.components.EmojiPicker
import com.syncjam.app.feature.social.presentation.components.FloatingEmoji
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.random.Random

private const val MAX_ACTIVE_REACTIONS = 20
private const val BURST_COUNT = 5
private const val BURST_X_SPREAD = 0.05f

private val BURST_EMOJIS = listOf("🔥", "❤️", "🎵", "✨", "💫", "🎉", "⚡", "🌟")

/**
 * Full-screen overlay that:
 * - Renders up to [MAX_ACTIVE_REACTIONS] animated [FloatingEmoji] instances.
 * - Exposes an [EmojiPicker] FAB (bottom-right).
 * - Triggers a 5-emoji burst on double-tap anywhere on the screen.
 * - Displays externally received reactions from [incomingReactions].
 *
 * @param onSendReaction        Called when the local user picks or double-taps an emoji — the
 *                              caller should broadcast this to the session.
 * @param incomingReactions     Remote reactions received via the ViewModel; consumed once displayed.
 * @param onIncomingConsumed    Called with each reaction id after it has been added to the local
 *                              display list so the ViewModel can clear the queue.
 * @param modifier              Optional modifier forwarded to the root [Box].
 */
@Composable
fun ReactionOverlay(
    onSendReaction: (String) -> Unit,
    incomingReactions: ImmutableList<Reaction> = persistentListOf(),
    onIncomingConsumed: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeReactions = remember { mutableStateListOf<Reaction>() }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val screenWidth = LocalConfiguration.current.screenWidthDp.toFloat()

    // Absorb new incoming reactions from ViewModel into the local display list.
    LaunchedEffect(incomingReactions.size) {
        incomingReactions.forEach { reaction ->
            if (activeReactions.size < MAX_ACTIVE_REACTIONS) {
                activeReactions.add(reaction)
            }
            onIncomingConsumed(reaction.id)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val baseX = (offset.x / screenWidth).coerceIn(0.05f, 0.95f)
                        repeat(BURST_COUNT) {
                            if (activeReactions.size < MAX_ACTIVE_REACTIONS) {
                                val jitter = (Random.nextFloat() * 2f - 1f) * BURST_X_SPREAD
                                val reaction = Reaction(
                                    emoji = BURST_EMOJIS.random(),
                                    senderName = "Du",
                                    senderId = "self",
                                    x = (baseX + jitter).coerceIn(0.02f, 0.95f),
                                    sessionId = ""
                                )
                                activeReactions.add(reaction)
                                onSendReaction(reaction.emoji)
                            }
                        }
                    }
                )
            }
    ) {
        activeReactions.forEach { reaction ->
            FloatingEmoji(
                reaction = reaction,
                screenWidth = screenWidth,
                onComplete = { id ->
                    activeReactions.removeAll { it.id == id }
                }
            )
        }

        FloatingActionButton(
            onClick = { showEmojiPicker = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
                .size(48.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEmotions,
                contentDescription = "Reaktion senden",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    if (showEmojiPicker) {
        EmojiPicker(
            onEmojiSelected = { emoji ->
                onSendReaction(emoji)
                if (activeReactions.size < MAX_ACTIVE_REACTIONS) {
                    activeReactions.add(
                        Reaction(
                            emoji = emoji,
                            senderName = "Du",
                            senderId = "self",
                            x = Random.nextFloat() * 0.8f + 0.1f,
                            sessionId = ""
                        )
                    )
                }
            },
            onDismiss = { showEmojiPicker = false }
        )
    }
}
