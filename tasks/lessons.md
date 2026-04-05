# tasks/lessons.md — SyncJam: Learned Lessons & Patterns

> **Dieses File wird nach JEDER Korrektur durch den User aktualisiert.**
> Lies es bei JEDEM Session-Start. Mach keine Fehler zweimal.

---

## 🔴 Kritische Regeln (NIEMALS brechen)

### Compose
1. **NIEMALS `collectAsState()`** — Immer `collectAsStateWithLifecycle()` verwenden.
   - Warum: collectAsState() collected auch wenn die UI nicht sichtbar ist → Ressourcenverschwendung, potentielle Crashes.
   - Fix: `val state by viewModel.uiState.collectAsStateWithLifecycle()`

2. **NIEMALS `Color(0xFF...)` hardcoded** — Immer `MaterialTheme.colorScheme.*`
   - Warum: Bricht Dynamic Theming (MaterialKolor), Dark/Light Mode, Accessibility.
   - Fix: `MaterialTheme.colorScheme.primary`, `.surface`, `.onSurface`, etc.

3. **NIEMALS LazyColumn ohne `key`** — Immer `key = { it.id }` + `contentType`.
   - Warum: Ohne key re-composet Compose ALLE Items bei jeder Änderung.
   - Fix: `LazyColumn { items(tracks, key = { it.id }, contentType = { "track" }) { ... } }`

4. **NIEMALS KAPT für Room** — Immer KSP.
   - Warum: KAPT ist deprecated, langsamer, inkompatibel mit K2 Compiler.
   - Fix: `ksp(libs.room.compiler)` statt `kapt(libs.room.compiler)`

5. **NIEMALS Business-Logic in Composables** — Immer in ViewModel/UseCase.
   - Warum: Composables recompose jederzeit → Side-Effects, Race Conditions.

### Architektur
6. **NIEMALS Repository direkt in Composable injizieren** — Immer ViewModel dazwischen.
   - Warum: Clean Architecture, testbar, Lifecycle-Management.

7. **NIEMALS `Any` oder ungetypte String-IDs** — Typed IDs oder value classes.
   - Warum: Compile-Time Safety, keine verwechselten IDs.
   - Fix: `@JvmInline value class TrackId(val value: String)`

8. **NIEMALS `@Insert` + `@Update` getrennt** — Immer `@Upsert`.
   - Warum: Race Conditions bei parallelen Operationen, weniger Code.

### Netzwerk
9. **NIEMALS WebSocket ohne Reconnect-Logic** — Exponential Backoff + Jitter.
   - Warum: Netzwerke sind unzuverlässig, besonders Mobilfunk.
   - Pattern: `delay(min(baseDelay * 2^attempt + random(0..jitter), maxDelay))`

10. **NIEMALS harte Seeks unter 500ms Drift** — Adaptive Playback-Rate verwenden.
    - Warum: Hörbare Glitches/Knackser bei kleinen Seeks.
    - Fix: `player.setPlaybackParameters(PlaybackParameters(1.02f))` für langsames Aufholen.

---

## 🟡 Häufige Fallstricke

### Media3 / ExoPlayer
- **Audio Focus**: `AUDIOFOCUS_GAIN` einmal für die ganze App, nicht pro Track. Voice Chat und Musik sind interne Streams — kein Focus-Konflikt.
- **Foreground Service**: Ab Android 14 MUSS `FOREGROUND_SERVICE_MEDIA_PLAYBACK` im Manifest deklariert werden, sonst SecurityException.
- **MediaSession**: Immer `MediaSession.Builder(context, player).build()` in der Service onCreate(), nicht im ViewModel.
- **setPlaybackParameters()**: Betrifft ALLE Playback-Parameter (Speed + Pitch). Für Drift-Korrektur nur Speed ändern, Pitch auf 1.0 lassen → `PlaybackParameters(speed = 1.02f, pitch = 1.0f)`.

### WebSocket
- **Binary Frames vs Text Frames**: SyncCommands als Text (JSON), Audio-Chunks als Binary. Nicht mischen!
- **Ping/Pong**: Ktor Client macht kein Auto-Ping — manuell implementieren mit `send(Frame.Ping(byteArrayOf()))`.
- **Connection Timeout**: Default ist zu kurz für mobile Netzwerke. Mindestens 30s Connect-Timeout, 60s Request-Timeout.

### Supabase
- **Realtime Broadcast vs Postgres Changes**: Broadcast ist ephemer (kein DB-Write), Postgres Changes persistent. Reactions → Broadcast. Votes → Postgres Changes.
- **RLS mit Service Key**: Server-Side Operations brauchen den Service Key, nicht den Anon Key. Anon Key nur im Client.
- **Token Refresh**: Supabase Kotlin SDK handled Auto-Refresh, aber nur wenn der Client nicht manuell disposed wird.

### LiveKit
- **Room.connect() ist async** — Immer in CoroutineScope mit Timeout (10s).
- **Mic-Permission vor connect()** — Permission muss VORHER granted sein, sonst Silent Audio.
- **Disconnect in onCleared()** — Room MUSS disconnected werden wenn ViewModel destroyed wird.

### Compose UI
- **graphicsLayer für Animations** — `Modifier.graphicsLayer { alpha = animatedAlpha }` ist performanter als `Modifier.alpha(animatedAlpha)` weil es auf der RenderNode-Ebene arbeitet.
- **Modifier.offset { IntOffset(...) }** (Lambda-Version) — Vermeidet Recomposition, nur Relayout.
- **rememberSaveable für User-Input** — `remember` überlebt Configuration Change NICHT.
- **snapshotStateListOf() für Reactions** — Performanter als `mutableStateListOf()` für häufige Adds/Removes.

### Kotlin Coroutines
- **supervisorScope für parallele Jobs** — Wenn ein Job failt, sollen die anderen weiterlaufen (z.B. Sync + Voice + Reactions).
- **flowOn(Dispatchers.IO) für Room-Queries** — Room Flows sind per Default Main-Thread, I/O-intensive Operationen MÜSSEN auf IO dispatched werden.
- **conflate() für Heartbeats** — Wenn der Consumer langsamer ist als der Producer, nur den neuesten Wert verarbeiten.

---

## 🟢 Bewährte Patterns

### Sealed Result<T>
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// Usage in ViewModel:
fun loadTracks() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        when (val result = getTracksUseCase()) {
            is Result.Success -> _uiState.update { it.copy(tracks = result.data.toImmutableList(), isLoading = false) }
            is Result.Error -> _uiState.update { it.copy(error = UiError(result.message), isLoading = false) }
            is Result.Loading -> { /* handled above */ }
        }
    }
}
```

### Event-Driven ViewModel
```kotlin
// IMMER dieses Pattern:
sealed interface SessionEvent {
    data class JoinSession(val code: String) : SessionEvent
    data object LeaveSession : SessionEvent
    data class PlayTrack(val trackId: String) : SessionEvent
    data object TogglePlayPause : SessionEvent
    data class SendReaction(val emoji: String) : SessionEvent
    data class VoteTrack(val requestId: String, val voteType: Int) : SessionEvent
}

// In ViewModel:
fun onEvent(event: SessionEvent) { ... }

// In Composable:
viewModel.onEvent(SessionEvent.JoinSession("ABC123"))
```

### WebSocket Reconnect Pattern
```kotlin
private suspend fun connectWithRetry(url: String, maxAttempts: Int = 10) {
    var attempt = 0
    while (attempt < maxAttempts) {
        try {
            connect(url)
            attempt = 0 // Reset on successful connection
        } catch (e: Exception) {
            attempt++
            val delay = min(1000L * (1 shl attempt) + Random.nextLong(0, 1000), 30_000L)
            delay(delay)
        }
    }
}
```

### Mapper Pattern (Entity ↔ Domain ↔ Ui)
```kotlin
// IMMER separate Mapper pro Layer:
fun LocalTrackEntity.toDomain(): Track = Track(
    id = TrackId(id),
    title = title,
    artist = artist,
    // ...
)

fun Track.toUi(): TrackUi = TrackUi(
    id = id.value,
    title = title,
    artistDisplay = artist,
    durationDisplay = durationMs.formatDuration(),
    // ...
)
```

---

## 📝 Projekt-spezifische Erkenntnisse

> Dieser Abschnitt wird während der Entwicklung gefüllt.
> Nach JEDER Korrektur durch den User hier einen neuen Eintrag hinzufügen.

### Template:
```
### [DATUM] — [KURZBESCHREIBUNG]
**Problem:** Was war falsch?
**Root Cause:** Warum war es falsch?
**Fix:** Was wurde geändert?
**Regel:** Welche Regel verhindert den Fehler in Zukunft?
```

---

*Letzte Aktualisierung: Initialer Stand — noch keine Korrekturen.*

### 2026-04-05 — v2.6.0 Modernisierung via Subagenten
**Problem:** Viele parallele Features zu implementieren ohne Konflikte.
**Root Cause:** Subagenten können keine Edit-Befehle ausführen wenn Permissions fehlen.
**Fix:** Subagenten für Research/Read nutzen, kritische Edit-Befehle im Hauptagenten ausführen.
**Regel:** Subagenten immer mit `run_in_background: true` für parallele unabhängige Tasks starten. Für dateiübergreifende Änderungen den Hauptagenten nutzen.

### 2026-04-05 — WindowSizeClass Dependency
**Problem:** `material3-window-size-class` braucht eigene Version in `libs.versions.toml`.
**Fix:** `material3-windowsizeclass = "1.3.2"` in versions, `material3-window-size-class = { group = "androidx.compose.material3", name = "material3-window-size-class", version.ref = "material3-windowsizeclass" }` in libraries.
**Regel:** Immer Version Catalog Alias verwenden, niemals direkte Strings in build.gradle.kts.
