package com.syncjam.app.feature.voting.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.syncjam.app.feature.library.presentation.LibraryViewModel
import com.syncjam.app.feature.library.presentation.TrackUi
import com.syncjam.app.feature.session.presentation.QueueEntryUi
import com.syncjam.app.feature.session.presentation.SessionEvent
import com.syncjam.app.feature.session.presentation.SessionViewModel
import com.syncjam.app.feature.session.presentation.YtDownloadState
import com.syncjam.app.feature.voting.presentation.components.QueueTrackItem
import com.syncjam.app.feature.voting.presentation.components.YouTubeAddDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onBack: () -> Unit,
    sessionEntry: NavBackStackEntry? = null,
    viewModel: SessionViewModel = if (sessionEntry != null) hiltViewModel(sessionEntry) else hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()

    var showYouTubeDialog by remember { mutableStateOf(false) }
    var showLibrarySheet by remember { mutableStateOf(false) }
    var librarySearch by rememberSaveable { mutableStateOf("") }
    val librarySheetState = rememberModalBottomSheetState()

    LaunchedEffect(showLibrarySheet) {
        if (showLibrarySheet && libraryState.tracks.isEmpty()) {
            libraryViewModel.scanLibrary()
        }
    }

    val filteredTracks = remember(libraryState.tracks, librarySearch) {
        if (librarySearch.isBlank()) libraryState.tracks
        else libraryState.tracks.filter { track ->
            track.title.contains(librarySearch, ignoreCase = true) ||
            track.artist.contains(librarySearch, ignoreCase = true) ||
            track.album.contains(librarySearch, ignoreCase = true)
        }
    }

    if (showYouTubeDialog) {
        YouTubeAddDialog(
            onConfirm = { url ->
                showYouTubeDialog = false
                viewModel.onEvent(SessionEvent.AddYouTubeTrack(url))
            },
            onDismiss = { showYouTubeDialog = false }
        )
    }

    if (showLibrarySheet) {
        ModalBottomSheet(
            onDismissRequest = { showLibrarySheet = false; librarySearch = "" },
            sheetState = librarySheetState,
        ) {
            LibraryPickerContent(
                tracks = filteredTracks,
                searchQuery = librarySearch,
                isLoading = libraryState.isLoading,
                onSearchChange = { librarySearch = it },
                onRefresh = { libraryViewModel.scanLibrary() },
                onAddTrack = { track ->
                    viewModel.onEvent(
                        SessionEvent.AddLocalTrackToQueue(
                            trackId = track.id,
                            title = track.title,
                            artist = track.artist,
                            durationMs = track.durationMs,
                            contentUri = track.contentUri,
                            albumArtUri = track.albumArtUri
                        )
                    )
                    showLibrarySheet = false
                    librarySearch = ""
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Warteschlange", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.playlist.size} Tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier
                    .navigationBarsPadding()
                    .padding(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = uiState.isUploadingTrack,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        "Track wird hochgeladen…",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = uiState.ytDownloadState != null,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut()
                    ) {
                        when (val state = uiState.ytDownloadState) {
                            is YtDownloadState.Downloading -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.HourglassTop,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                "Lädt: ${state.title.ifEmpty { state.youtubeId }}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                            is YtDownloadState.Error -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Text(
                                        "Fehler: ${state.message}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            null -> {}
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { showLibrarySheet = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Aus Bibliothek")
                        }
                        Button(
                            onClick = { showYouTubeDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("YouTube")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.playlist.isEmpty()) {
            EmptyQueueState(
                onAddFromLibrary = { showLibrarySheet = true },
                onAddYoutube = { showYouTubeDialog = true },
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
        } else {
            val currentIdx = uiState.currentQueueIndex
            val nowPlaying = uiState.playlist.getOrNull(currentIdx)
            val upNext = if (uiState.playlist.size > currentIdx + 1)
                uiState.playlist.drop(currentIdx + 1) else emptyList()
            val played = if (currentIdx > 0) uiState.playlist.take(currentIdx) else emptyList()

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                nowPlaying?.let { track ->
                    item(key = "header_now_playing") {
                        QueueSectionHeader(
                            title = "Läuft gerade",
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    item(key = "now_playing_${track.requestId}") {
                        NowPlayingCard(
                            entry = track,
                            currentUserId = uiState.currentUserId,
                            onUpvote = { viewModel.onEvent(SessionEvent.Vote(track.requestId, 1)) },
                            onDownvote = { viewModel.onEvent(SessionEvent.Vote(track.requestId, -1)) },
                            onRemove = { viewModel.onEvent(SessionEvent.RemoveFromQueue(track.requestId)) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (upNext.isNotEmpty()) {
                    item(key = "header_up_next") {
                        QueueSectionHeader(
                            title = "Als nächstes",
                            count = upNext.size,
                            accentColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                    items(upNext, key = { it.requestId }, contentType = { "queue_track" }) { track ->
                        QueueTrackItem(
                            entry = track,
                            currentUserId = uiState.currentUserId,
                            onUpvote = { viewModel.onEvent(SessionEvent.Vote(track.requestId, 1)) },
                            onDownvote = { viewModel.onEvent(SessionEvent.Vote(track.requestId, -1)) },
                            onRemove = { viewModel.onEvent(SessionEvent.RemoveFromQueue(track.requestId)) }
                        )
                    }
                }

                if (played.isNotEmpty()) {
                    item(key = "header_played") {
                        Spacer(Modifier.height(8.dp))
                        QueueSectionHeader(
                            title = "Bereits gespielt",
                            count = played.size,
                            accentColor = MaterialTheme.colorScheme.outline
                        )
                    }
                    items(played, key = { it.requestId }, contentType = { "played_track" }) { track ->
                        QueueTrackItem(
                            entry = track,
                            currentUserId = uiState.currentUserId,
                            onUpvote = { viewModel.onEvent(SessionEvent.Vote(track.requestId, 1)) },
                            onDownvote = { viewModel.onEvent(SessionEvent.Vote(track.requestId, -1)) },
                            onRemove = { viewModel.onEvent(SessionEvent.RemoveFromQueue(track.requestId)) }
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun LibraryPickerContent(
    tracks: List<TrackUi>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onAddTrack: (TrackUi) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Aus Bibliothek wählen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Suchen…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Löschen")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // Stats row
        if (!isLoading && tracks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${tracks.size} ${if (tracks.size == 1) "Track" else "Tracks"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        if (tracks.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (searchQuery.isNotEmpty()) "Kein Ergebnis für \"$searchQuery\""
                        else "Keine Tracks gefunden",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isEmpty()) {
                        Button(onClick = onRefresh) { Text("Bibliothek scannen") }
                    }
                }
            }
        } else {
            // Group by artist
            val grouped = tracks.groupBy { it.artist }.toSortedMap()

            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                grouped.forEach { (artist, artistTracks) ->
                    item(key = "artist_$artist") {
                        LibraryArtistHeader(artist = artist, trackCount = artistTracks.size)
                    }
                    items(artistTracks, key = { it.id }, contentType = { "library_track" }) { track ->
                        LibraryTrackRow(track = track, onAdd = { onAddTrack(track) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryArtistHeader(artist: String, trackCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            artist.ifBlank { "Unbekannter Künstler" },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "$trackCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LibraryTrackRow(track: TrackUi, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAdd() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${track.album} · ${track.durationDisplay}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onAdd) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Hinzufügen",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

@Composable
private fun NowPlayingCard(
    entry: QueueEntryUi,
    currentUserId: String,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onRemove: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "now_playing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f, label = "pulse",
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated playing indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
                contentAlignment = Alignment.Center
            ) {
                PlayingBarsSmall()
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    entry.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "von ${entry.requestedByName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${entry.score}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Votes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun PlayingBarsSmall() {
    val infiniteTransition = rememberInfiniteTransition(label = "small_bars")
    val delays = listOf(0, 150, 300)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        delays.forEach { delay ->
            val height by infiniteTransition.animateFloat(
                initialValue = 3f, targetValue = 12f, label = "bar_$delay",
                animationSpec = infiniteRepeatable(tween(500 + delay), RepeatMode.Reverse)
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun QueueSectionHeader(title: String, accentColor: androidx.compose.ui.graphics.Color, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
            modifier = Modifier.weight(1f)
        )
        count?.let {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Text(
                    "$it",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyQueueState(
    onAddFromLibrary: () -> Unit,
    onAddYoutube: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Warteschlange ist leer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Füge Tracks aus deiner Bibliothek oder\nvon YouTube zur Warteschlange hinzu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onAddFromLibrary) {
                    Icon(Icons.Default.LibraryMusic, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bibliothek")
                }
                Button(
                    onClick = onAddYoutube,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.PlayCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("YouTube")
                }
            }
        }
    }
}
