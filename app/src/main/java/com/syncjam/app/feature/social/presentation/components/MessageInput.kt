package com.syncjam.app.feature.social.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

private const val MAX_MESSAGE_LENGTH = 500

/**
 * Chat message input field with an emoji picker trigger (leading icon) and a send button.
 *
 * - Hard-limited to [MAX_MESSAGE_LENGTH] (500) characters.
 * - Sends on IME action (Done/Send) or send-button tap.
 * - Calls [onTyping] on every keystroke so the caller can broadcast a typing indicator.
 *
 * @param onSend        Called with the trimmed message text when the user submits.
 * @param onEmojiClick  Called when the emoji icon is tapped — the caller decides how to handle
 *                      emoji insertion (e.g., open a system emoji keyboard or custom picker).
 * @param onTyping      Called on every text change — use to debounce and send typing indicators.
 */
@Composable
fun MessageInput(
    onSend: (String) -> Unit,
    onEmojiClick: () -> Unit = {},
    onTyping: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    fun submit() {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            onSend(trimmed)
            text = ""
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { newValue ->
                if (newValue.length <= MAX_MESSAGE_LENGTH) {
                    text = newValue
                    if (newValue.isNotEmpty()) onTyping()
                }
            },
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "Nachricht…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                IconButton(
                    onClick = onEmojiClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Emoji einfügen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = {
                if (text.isNotBlank()) {
                    IconButton(
                        onClick = ::submit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Senden",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            supportingText = if (text.length > MAX_MESSAGE_LENGTH - 50) {
                { Text("${text.length} / $MAX_MESSAGE_LENGTH") }
            } else null
        )
    }
}
