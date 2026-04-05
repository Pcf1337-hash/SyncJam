package com.syncjam.app.feature.social.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val QUICK_EMOJIS = listOf("❤️", "😄", "🔥", "👏", "😭", "🤩", "💀", "🎵")
private val ALL_EMOJIS = listOf(
    "❤️", "😄", "🔥", "👏", "😭", "🤩", "💀", "🎵",
    "😍", "🥳", "😂", "🙌", "💯", "⚡", "🎉", "😎",
    "🤯", "💃", "🕺", "🎸", "🥁", "🎤", "🎧", "🌟",
    "✨", "💫", "🌈", "🦋", "🐝", "🎵", "🎶", "🎼"
)

/**
 * Bottom sheet emoji picker.
 *
 * Shows 8 quick-access emojis by default. A "Mehr Emojis" button expands to a 8-column
 * scrollable grid containing all available emojis.
 *
 * @param onEmojiSelected Called with the selected emoji string; the sheet is dismissed immediately.
 * @param onDismiss       Called when the sheet should be hidden (swipe-down or emoji selected).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showAll by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Reaktion senden",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (!showAll) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QUICK_EMOJIS.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            modifier = Modifier
                                .clickable {
                                    onEmojiSelected(emoji)
                                    onDismiss()
                                }
                                .padding(8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showAll = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Mehr Emojis",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(items = ALL_EMOJIS, key = { it + ALL_EMOJIS.indexOf(it) }) { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clickable {
                                    onEmojiSelected(emoji)
                                    onDismiss()
                                }
                                .padding(6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
