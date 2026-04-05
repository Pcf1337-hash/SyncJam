package com.syncjam.app.feature.library.presentation

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    onCreateSession: () -> Unit,
    onJoinSession: () -> Unit,
    onTrackSelected: (TrackUi) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    var showSortSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var trackForPlaylist by remember { mutableStateOf<TrackUi?>(null) }

    val audioPermission = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
    )

    LaunchedEffect(audioPermission.status.isGranted) {
        if (audioPermission.status.isGranted) viewModel.scanLibrary()
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = sheetState
        ) {
            SortBottomSheetContent(
                currentSort = uiState.sortOption,
                onSortSelected = { option ->
                    viewModel.setSortOption(option)
                    coroutineScope.launch { sheetState.hide(); showSortSheet = false }
                }
            )
        }
    }

    // Add-to-playlist sheet
    trackForPlaylist?.let { track ->
        AddToPlaylistSheet(
            playlists = uiState.playlists,
            onDismiss = { trackForPlaylist = null },
            onAddToPlaylist = { playlistId ->
                viewModel.addTrackToPlaylist(playlistId, track.id)
                trackForPlaylist = null
            },
            onCreateNew = { showCreatePlaylistDialog = true }
        )
    }

    // Create playlist dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, description ->
                viewModel.createPlaylist(name, description)
                showCreatePlaylistDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SyncJam",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (uiState.selectedTab == LibraryTab.TRACKS) {
                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sortieren")
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = if (uiState.isGridView) "Listenansicht" else "Gitteransicht"
                            )
                        }
                        IconButton(onClick = { if (audioPermission.status.isGranted) viewModel.scanLibrary() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                        }
                    }
                    IconButton(onClick = onJoinSession) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Session beitreten")
                    }
                }
            )
        },
        floatingActionButton = {
            when (uiState.selectedTab) {
                LibraryTab.PLAYLISTS -> FloatingActionButton(
                    onClick = { showCreatePlaylistDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Playlist erstellen")
                }
                else -> FloatingActionButton(
                    onClick = onCreateSession,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Session erstellen")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading || uiState.isDownloadingCovers) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = uiState.selectedTab == LibraryTab.TRACKS,
                    onClick = { viewModel.setTab(LibraryTab.TRACKS) },
                    text = { Text("Tracks") },
                    icon = { Icon(Icons.Default.LibraryMusic, null, Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == LibraryTab.FAVORITES,
                    onClick = { viewModel.setTab(LibraryTab.FAVORITES) },
                    text = { Text("Favoriten (${uiState.favorites.size})") },
                    icon = { Icon(Icons.Default.Favorite, null, Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == LibraryTab.PLAYLISTS,
                    onClick = { viewModel.setTab(LibraryTab.PLAYLISTS) },
                    text = { Text("Playlisten (${uiState.playlists.size})") },
                    icon = { Icon(Icons.Default.PlaylistPlay, null, Modifier.size(18.dp)) }
                )
            }

            when (uiState.selectedTab) {
                LibraryTab.TRACKS -> TracksTab(
                    uiState = uiState,
                    audioPermission = audioPermission,
                    viewModel = viewModel,
                    onTrackSelected = onTrackSelected,
                    onLongPressTrack = { trackForPlaylist = it }
                )
                LibraryTab.FAVORITES -> FavoritesTab(
                    tracks = uiState.favorites,
                    onTrackSelected = onTrackSelected,
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onLongPressTrack = { trackForPlaylist = it }
                )
                LibraryTab.PLAYLISTS -> PlaylistsTab(
                    playlists = uiState.playlists,
                    onDeletePlaylist = { viewModel.deletePlaylist(it) }
                )
            }
        }
    }
}

// ── Tracks Tab ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun TracksTab(
    uiState: LibraryUiState,
    audioPermission: com.google.accompanist.permissions.PermissionState,
    viewModel: LibraryViewModel,
    onTrackSelected: (TrackUi) -> Unit,
    onLongPressTrack: (TrackUi) -> Unit
) {
    when {
        !audioPermission.status.isGranted -> PermissionRequiredContent(
            showRationale = audioPermission.status.shouldShowRationale,
            onRequest = { audioPermission.launchPermissionRequest() }
        )
        uiState.isLoading && uiState.tracks.isEmpty() -> LazyColumn {
            items(8) { ShimmerItem() }
        }
        uiState.tracks.isEmpty() -> EmptyLibraryContent(onScan = { viewModel.scanLibrary() })
        else -> AnimatedContent(
            targetState = uiState.isGridView,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "viewToggle"
        ) { isGrid ->
            if (isGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tracks, key = { it.id }) { track ->
                        AlbumGridItem(track = track, onClick = { onTrackSelected(track) })
                    }
                }
            } else {
                TrackList(
                    tracks = uiState.tracks,
                    onTrackSelected = onTrackSelected,
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onLongPress = onLongPressTrack
                )
            }
        }
    }
}

// ── Favorites Tab ─────────────────────────────────────────────────────────────

@Composable
private fun FavoritesTab(
    tracks: List<TrackUi>,
    onTrackSelected: (TrackUi) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onLongPressTrack: (TrackUi) -> Unit
) {
    if (tracks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Noch keine Favoriten",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tippe auf das Herz-Symbol bei einem Track, um ihn als Favoriten zu markieren.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        TrackList(
            tracks = tracks,
            onTrackSelected = onTrackSelected,
            onToggleFavorite = onToggleFavorite,
            onLongPress = onLongPressTrack
        )
    }
}

// ── Playlists Tab ─────────────────────────────────────────────────────────────

@Composable
private fun PlaylistsTab(
    playlists: List<PlaylistUi>,
    onDeletePlaylist: (String) -> Unit
) {
    if (playlists.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.PlaylistPlay,
                    null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Noch keine Playlisten",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tippe auf das + Symbol, um eine neue Playlist zu erstellen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(playlists, key = { it.id }, contentType = { "playlist" }) { playlist ->
                PlaylistItem(playlist = playlist, onDelete = { onDeletePlaylist(playlist.id) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun PlaylistItem(playlist: PlaylistUi, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Playlist löschen") },
            text = { Text("\"${playlist.name}\" wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") }
            }
        )
    }

    ListItem(
        headlineContent = {
            Text(playlist.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                if (playlist.description.isNotBlank()) playlist.description else "${playlist.trackCount} Tracks",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlaylistPlay,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        trailingContent = {
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

// ── Track List ────────────────────────────────────────────────────────────────

@Composable
private fun TrackList(
    tracks: List<TrackUi>,
    onTrackSelected: (TrackUi) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onLongPress: (TrackUi) -> Unit
) {
    LazyColumn {
        items(tracks, key = { it.id }, contentType = { "track" }) { track ->
            ListItem(
                headlineContent = {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text(
                        "${track.artist} · ${track.album}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            track.durationDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { onToggleFavorite(track.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (track.isFavorite) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen",
                                tint = if (track.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.combinedClickable(
                    onClick = { onTrackSelected(track) },
                    onLongClick = { onLongPress(track) }
                )
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}

// ── Album Grid Item ───────────────────────────────────────────────────────────

@Composable
private fun AlbumGridItem(track: TrackUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            if (track.isFavorite) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        .size(20.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(track.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Add to Playlist Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlaylistSheet(
    playlists: List<PlaylistUi>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Zur Playlist hinzufügen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            ListItem(
                headlineContent = { Text("Neue Playlist erstellen") },
                leadingContent = {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                },
                modifier = Modifier.clickable { onCreateNew() }
            )
            HorizontalDivider()
            if (playlists.isEmpty()) {
                Text(
                    "Noch keine Playlisten vorhanden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                playlists.forEach { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        leadingContent = {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.PlaylistPlay, null, tint = MaterialTheme.colorScheme.primary) }
                        },
                        modifier = Modifier.clickable { onAddToPlaylist(playlist.id) }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Create Playlist Dialog ────────────────────────────────────────────────────

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) { Text("Erstellen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

// ── Shimmer Placeholder ───────────────────────────────────────────────────────

@Composable
fun ShimmerItem(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    Row(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(Modifier.size(56.dp).graphicsLayer { alpha = shimmerAlpha }.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column {
            Box(Modifier.fillMaxWidth(0.7f).height(14.dp).graphicsLayer { alpha = shimmerAlpha }.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.5f).height(12.dp).graphicsLayer { alpha = shimmerAlpha }.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)))
        }
    }
}

// ── Sort Bottom Sheet ─────────────────────────────────────────────────────────

@Composable
private fun SortBottomSheetContent(currentSort: SortOption, onSortSelected: (SortOption) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Sortieren nach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        mapOf(
            SortOption.TITLE to "Titel",
            SortOption.ARTIST to "Interpret",
            SortOption.ALBUM to "Album",
            SortOption.DURATION to "Länge",
            SortOption.RECENTLY_PLAYED to "Zuletzt gespielt"
        ).forEach { (option, label) ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSortSelected(option) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = currentSort == option, onClick = { onSortSelected(option) })
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ── Permission Required ───────────────────────────────────────────────────────

@Composable
private fun PermissionRequiredContent(showRationale: Boolean, onRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Musikbibliothek", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (showRationale) "SyncJam benötigt Zugriff auf deine Musik, um gemeinsam mit anderen hören zu können."
                else "Erlaube SyncJam den Zugriff auf deine Musikdateien.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRequest) { Text("Zugriff erlauben") }
        }
    }
}

// ── Empty Library ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyLibraryContent(onScan: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Keine Musik gefunden", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Es wurden keine Audiodateien auf deinem Gerät gefunden.\nUnterstützte Formate: MP3, FLAC, WAV, OGG, AAC, M4A",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onScan) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp).padding(end = 4.dp))
                Text("Erneut scannen")
            }
        }
    }
}
