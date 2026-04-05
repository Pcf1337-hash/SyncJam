package com.syncjam.app.feature.social.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.syncjam.app.feature.social.presentation.components.ChatBubble
import com.syncjam.app.feature.social.presentation.components.MessageInput
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.syncjam.app.feature.social.domain.model.ChatMessage

/**
 * Chat bottom sheet showing session messages with auto-scroll and a typing indicator.
 *
 * Opening the sheet automatically resets the unread badge (caller's responsibility via [onOpened]).
 *
 * @param messages      Immutable list of [ChatMessage] to display.
 * @param typingUser    Display name of the user currently typing, or null.
 * @param onSend        Called with the trimmed message text when the user submits.
 * @param onTyping      Called on every keystroke for broadcasting the typing indicator.
 * @param onOpened      Called once when the sheet becomes visible — use to clear unread count.
 * @param onDismiss     Called when the sheet should be hidden.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSheet(
    messages: ImmutableList<ChatMessage> = persistentListOf(),
    typingUser: String? = null,
    onSend: (String) -> Unit,
    onTyping: () -> Unit = {},
    onOpened: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val listState = rememberLazyListState()

    // Notify caller that the sheet is open so unread count can be reset.
    LaunchedEffect(Unit) {
        onOpened()
    }

    // Auto-scroll to the latest message whenever the list grows.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Text(
                text = "Chat",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                itemsIndexed(
                    items = messages,
                    key = { _, message -> message.id },
                    contentType = { _, message -> if (message.isOwn) "own" else "foreign" }
                ) { _, message ->
                    ChatBubble(message = message)
                }

                if (messages.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Noch keine Nachrichten. Sag Hallo! 👋",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Typing indicator displayed above the input field.
            AnimatedVisibility(
                visible = typingUser != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "$typingUser tippt…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            MessageInput(
                onSend = onSend,
                onTyping = onTyping,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
