package com.syncjam.app.feature.social.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.gif.GifDecoder
import com.syncjam.app.feature.social.domain.model.GifResult

private const val GIF_RATE_LIMIT_MS = 5_000L

/**
 * Bottom sheet GIF picker backed by a pre-loaded [gifs] list (trending or search results).
 *
 * Features:
 * - Search field that calls [onSearch] — the parent is responsible for updating [gifs].
 * - 2-column scrollable GIF preview grid using [AsyncImage] + Coil GIF decoder.
 * - Rate-limiting: a countdown is shown when [lastGifSentAt] is too recent.
 * - Preview dialog before sending — user can confirm or cancel.
 *
 * @param gifs              Currently displayed GIF results (trending or search).
 * @param isLoading         Whether a search/trending fetch is in progress.
 * @param lastGifSentAt     Epoch-ms timestamp of the last sent GIF (for rate-limiting).
 * @param onSearch          Called when the user submits a new search query.
 * @param onSend            Called with the selected [GifResult] URL after preview confirm.
 * @param onDismiss         Called when the sheet should be hidden.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifPicker(
    gifs: List<GifResult>,
    isLoading: Boolean = false,
    lastGifSentAt: Long = 0L,
    onSearch: (String) -> Unit,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var query by remember { mutableStateOf("") }
    var selectedGif by remember { mutableStateOf<GifResult?>(null) }

    // Remaining rate-limit cooldown in seconds (recalculated each recomposition).
    val now = System.currentTimeMillis()
    val remainingMs = (GIF_RATE_LIMIT_MS - (now - lastGifSentAt)).coerceAtLeast(0L)
    val canSend = remainingMs == 0L

    // Trigger trending load on first open.
    LaunchedEffect(Unit) {
        onSearch("")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = "GIF senden",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "GIFs suchen…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge
            )

            // Rate-limit notice
            if (!canSend) {
                Text(
                    text = "Warte noch ${remainingMs / 1000}s…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search submit button
            TextButton(
                onClick = { onSearch(query) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Suchen",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                gifs.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keine GIFs gefunden.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items = gifs, key = { it.id }) { gif ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(gif.previewUrl)
                                    .decoderFactory(GifDecoder.Factory())
                                    .build(),
                                contentDescription = gif.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.small)
                                    .border(
                                        width = if (selectedGif?.id == gif.id) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = canSend) { selectedGif = gif }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Preview/confirm dialog before sending.
    selectedGif?.let { gif ->
        GifPreviewDialog(
            gif = gif,
            onConfirm = {
                onSend(gif.url)
                selectedGif = null
                onDismiss()
            },
            onCancel = { selectedGif = null }
        )
    }
}

@Composable
private fun GifPreviewDialog(
    gif: GifResult,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GIF senden?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(gif.url)
                        .decoderFactory(GifDecoder.Factory())
                        .build(),
                    contentDescription = gif.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text(
                            text = "Abbrechen",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onConfirm) {
                        Text("Senden")
                    }
                }
            }
        }
    }
}
