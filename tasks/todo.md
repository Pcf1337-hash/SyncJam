# tasks/todo.md — SyncJam Implementation Plan

> **10 Phasen, ~120 Tasks** — Von Projekt-Setup bis Polish & Release.
> Jede Phase baut auf der vorherigen auf. Nicht überspringen!

---

## Phase 1: Projekt-Grundgerüst & Build-System
> **Ziel:** Kompilierbares Projekt mit allen Dependencies, leere Feature-Module, Theme, Navigation.

- [ ] 1.1 Android-Projekt erstellen (com.syncjam.app, minSdk 26, targetSdk 35)
- [ ] 1.2 `gradle/libs.versions.toml` mit ALLEN Dependencies aus CLAUDE.md anlegen
- [ ] 1.3 `build.gradle.kts` (Project + App) konfigurieren — KSP, Hilt, Compose, kotlinx.serialization
- [ ] 1.4 Package-Struktur anlegen: core/, feature/, sync/, db/ (alle Packages leer mit .gitkeep)
- [ ] 1.5 `SyncJamApp.kt` (@HiltAndroidApp) + `MainActivity.kt` (Single Activity, enableEdgeToEdge)
- [ ] 1.6 Theme-System: `SyncJamTheme.kt` mit MaterialKolor DynamicMaterialTheme (Dark Default)
- [ ] 1.7 Typography definieren (Type.kt) — Inter/Outfit oder System Default
- [ ] 1.8 `core/common/Result.kt` — Sealed Result<T> (Success, Error, Loading)
- [ ] 1.9 `core/common/Extensions.kt` — Basis-Extensions (Context, Flow, etc.)
- [ ] 1.10 Navigation: `SyncJamNavGraph.kt` mit @Serializable Routes + NavHost (leere Screens)
- [ ] 1.11 AndroidManifest.xml mit allen Permissions aus CLAUDE.md
- [ ] 1.12 `.editorconfig` + Detekt/Ktlint Config
- [ ] 1.13 **Verify:** `./gradlew assembleDebug` kompiliert fehlerfrei

### Review Phase 1:
- [ ] Alle Dependencies resolven korrekt
- [ ] Navigation zwischen leeren Screens funktioniert
- [ ] Theme wird korrekt angewendet (Dark, Material 3)

---

## Phase 2: Lokale Musikbibliothek
> **Ziel:** MediaStore scannen, Tracks in Room speichern, Library-UI mit Suche.

- [ ] 2.1 Room Database Setup: `SyncJamDatabase.kt` mit KSP
- [ ] 2.2 `LocalTrackEntity.kt` — Entity wie in CLAUDE.md definiert
- [ ] 2.3 `LocalTrackDao.kt` — @Query für getAll, search(query), getByAlbum, getByArtist, @Upsert
- [ ] 2.4 `DatabaseModule.kt` — Hilt @Module mit @Provides für Database + DAOs
- [ ] 2.5 Domain Models: `Track.kt`, `Album.kt`, `Artist.kt` (mit Mapper-Extensions)
- [ ] 2.6 `MediaStoreScanner.kt` — ContentResolver Query auf MediaStore.Audio.Media
  - [ ] Filter: IS_MUSIC = 1, DURATION > 30000
  - [ ] Felder: TITLE, ARTIST, ALBUM, DURATION, SIZE, MIME_TYPE, DATA, ALBUM_ID
  - [ ] Album Art URI: `ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)`
- [ ] 2.7 `LibraryRepository` Interface + `LibraryRepositoryImpl`
- [ ] 2.8 UseCases: `ScanMediaStoreUseCase`, `GetTracksUseCase`, `SearchLibraryUseCase`, `GetAlbumsUseCase`
- [ ] 2.9 `LibraryViewModel` — StateFlow<LibraryUiState>, Event-basiert
- [ ] 2.10 Permission-Handling: READ_MEDIA_AUDIO (API 33+) / READ_EXTERNAL_STORAGE (≤32)
- [ ] 2.11 UI: `LibraryScreen` mit Tabs (Tracks, Albums, Artists)
- [ ] 2.12 UI: `TrackListItem` — Album Art, Title, Artist, Duration (LazyColumn mit key+contentType)
- [ ] 2.13 UI: `AlbumGrid` — LazyVerticalGrid mit Album Cover + Name
- [ ] 2.14 UI: Search Bar mit debounced Input (300ms)
- [ ] 2.15 **Verify:** App zeigt lokale Musik an, Suche funktioniert, Album Art lädt

### Review Phase 2:
- [ ] MediaStore-Scan läuft performant (< 2s für 1000 Tracks)
- [ ] Room-Cache funktioniert (Zweiter Start lädt aus DB, nicht MediaStore)
- [ ] Suche liefert korrekte Ergebnisse über Title + Artist + Album

---

## Phase 3: Audio Playback (Lokal, ohne Sync)
> **Ziel:** Voll funktionaler lokaler Music Player mit Media3, Foreground Service, Mini-Player.

- [ ] 3.1 `PlaybackService.kt` — MediaSessionService mit Media3 ExoPlayer
  - [ ] AudioAttributes: USAGE_MEDIA, CONTENT_TYPE_MUSIC
  - [ ] Audio Focus: AUDIOFOCUS_GAIN
  - [ ] Foreground Service + MediaNotification
- [ ] 3.2 `PlaybackRepository` Interface + Impl — Abstraction über Media3 Player
- [ ] 3.3 `PlaybackState.kt` Domain Model — isPlaying, currentTrack, positionMs, durationMs, queue
- [ ] 3.4 Hilt Module: Player-Instanz providen via MediaController + SessionToken
- [ ] 3.5 `PlayerViewModel` — Observes PlaybackState, dispatches PlayerCommands
- [ ] 3.6 UI: `MiniPlayerBar` — Composable am unteren Rand (Track Info + Play/Pause + Progress)
- [ ] 3.7 UI: `FullPlayerScreen` — Großes Album Art, Controls, Progress Slider, Queue-Button
- [ ] 3.8 UI: `PlaybackControls` — Previous, Play/Pause, Next, Shuffle, Repeat
- [ ] 3.9 UI: `ProgressSlider` — Draggable, zeigt aktuelle Position + Gesamtdauer
- [ ] 3.10 UI: `AlbumArtCover` — Coil AsyncImage mit Crossfade + Placeholder
- [ ] 3.11 Shared Element Transition: MiniPlayer → FullPlayer (Album Art + Title)
- [ ] 3.12 Queue Management: AddToQueue, RemoveFromQueue, Reorder
- [ ] 3.13 Notification Controls funktionieren (Play/Pause/Skip via MediaSession)
- [ ] 3.14 **Verify:** Musik spielt, Notification funktioniert, Mini→Full Transition smooth

### Review Phase 3:
- [ ] Audio Focus wird korrekt gehandled (pausiert bei Anruf, etc.)
- [ ] Service überlebt App-Close (Foreground Notification)
- [ ] Seek funktioniert flüssig ohne Stutter

---

## Phase 4: Supabase Backend & Auth
> **Ziel:** User-Authentifizierung, Profil, Supabase-Tabellen aufgesetzt.

- [ ] 4.1 Supabase-Projekt erstellen (Free Tier)
- [ ] 4.2 SQL-Schema aus CLAUDE.md ausführen (profiles, sessions, participants, track_requests, votes)
- [ ] 4.3 RLS Policies aktivieren und testen
- [ ] 4.4 Vote-Score Trigger deployen
- [ ] 4.5 Supabase Kotlin SDK einbinden (`supabase-kt`)
- [ ] 4.6 `NetworkModule.kt` — Supabase Client mit Auth + Realtime + Storage + Postgrest
- [ ] 4.7 `AuthRepository` Interface + `AuthRepositoryImpl` — signUp, signIn, signOut, currentUser
- [ ] 4.8 `AuthViewModel` + `LoginScreen` — Email/Passwort Auth (simple)
- [ ] 4.9 `ProfileSheet` — Display Name + Avatar setzen
- [ ] 4.10 Deep Link Support: `syncjam://join/{code}` für Session-Invites
- [ ] 4.11 **Verify:** Login/Logout funktioniert, Profil wird in Supabase gespeichert

### Review Phase 4:
- [ ] RLS blockiert unauthorisierten Zugriff
- [ ] Token-Refresh funktioniert automatisch
- [ ] Deep Links öffnen die App korrekt

---

## Phase 5: Ktor WebSocket Sync Server
> **Ziel:** Funktionierender Sync-Server mit Room-Management, NTP-Clock-Sync, Command Broadcasting.

- [ ] 5.1 Ktor Server Modul aufsetzen (server/build.gradle.kts)
- [ ] 5.2 `Application.kt` — Ktor mit WebSocket + ContentNegotiation + CORS Plugins
- [ ] 5.3 `SyncCommand.kt` — Shared Protocol sealed interface (wie in CLAUDE.md)
- [ ] 5.4 `SessionState.kt` — Authoritative State Data Class (currentTrack, positionMs, isPlaying, queue, participants)
- [ ] 5.5 `SessionManager.kt` — ConcurrentHashMap<String, SessionRoom>
  - [ ] createSession(hostId) → SessionCode
  - [ ] joinSession(code, userId, wsSession)
  - [ ] leaveSession(code, userId)
  - [ ] getSession(code)
- [ ] 5.6 WebSocket Route: `/ws/session/{code}`
  - [ ] On Connect: Validate token, add to room, send StateSnapshot
  - [ ] On Message: Parse SyncCommand, update state, broadcast to room
  - [ ] On Close: Remove from room, broadcast participant leave
- [ ] 5.7 NTP Handshake: `/ws/sync/{code}` — T1→T2→T3→T4 Exchange
- [ ] 5.8 Heartbeat Handler: Empfange Client-Positionen, berechne Drift, sende Korrekturen
- [ ] 5.9 `SyncBroadcaster.kt` — Broadcast mit Server-Timestamp zu allen Teilnehmern
- [ ] 5.10 REST Endpoints: `POST /session` (create), `GET /session/{code}` (info), `DELETE /session/{code}` (end)
- [ ] 5.11 Dockerfile + docker-compose.yml für lokales Development
- [ ] 5.12 **Verify:** Server startet, WebSocket-Verbindung klappt, Kommandos werden gebroadcasted

### Review Phase 5:
- [ ] Latenz < 50ms für Command Broadcasting (lokal testen)
- [ ] Mehrere Clients gleichzeitig möglich
- [ ] Session wird aufgeräumt wenn alle disconnecten

---

## Phase 6: Client-Seitige Sync-Engine
> **Ziel:** Android-Client verbindet sich mit Sync-Server, synchronisiertes Playback zwischen 2+ Geräten.

- [ ] 6.1 `core/network/WebSocketClient.kt` — Ktor WebSocket Client Wrapper
  - [ ] Auto-Reconnect mit Exponential Backoff + Jitter
  - [ ] Ping/Pong: 15s Intervall, 10s Timeout
  - [ ] Flow<WebSocketEvent> für incoming messages
- [ ] 6.2 `sync/NtpClockSync.kt` — 5-Sample NTP Handshake, Offset berechnen
- [ ] 6.3 `sync/SyncEngine.kt` — Orchestriert:
  - [ ] Session Join/Leave
  - [ ] Clock Sync bei Connect
  - [ ] Command Dispatching (outgoing)
  - [ ] Command Processing (incoming)
  - [ ] StateFlow<SyncState> exposed
- [ ] 6.4 `sync/DriftCorrector.kt` — Drift Detection + Correction
  - [ ] < 150ms: nichts
  - [ ] 150-500ms: Playback-Rate 1.02×/0.98× via player.setPlaybackParameters()
  - [ ] 500-2000ms: player.seekTo(correctedPosition)
  - [ ] > 2000ms: Full re-sync (StateSnapshot anfordern)
- [ ] 6.5 `SyncedPlayerController.kt` — Wraps Media3 Player + SyncEngine
  - [ ] play() → sendet SyncCommand.Play an Server
  - [ ] onServerPlay() → berechnet korrigierte Position, seekTo, play
  - [ ] Heartbeat: alle 2s currentPosition an Server senden
- [ ] 6.6 `SessionRepositoryImpl.kt` — Create/Join/Leave Session via REST + WebSocket
- [ ] 6.7 `SessionViewModel` — UI State für Session (Participants, CurrentTrack, Queue, IsPlaying)
- [ ] 6.8 UI: `SessionScreen` — Übersicht: Now Playing, Participants, Session Code teilen
- [ ] 6.9 UI: `CreateSessionScreen` — Session Name, Session erstellen → Code anzeigen
- [ ] 6.10 UI: `JoinSessionScreen` — Code eingeben oder Deep Link
- [ ] 6.11 **Verify:** Zwei Geräte spielen synchronisiert den gleichen Track, Drift < 200ms

### Review Phase 6:
- [ ] Sync funktioniert bei gutem Netz (< 100ms Drift)
- [ ] Reconnect nach Netzwerkverlust funktioniert
- [ ] Seek/Skip wird korrekt synchronisiert
- [ ] Session Join/Leave aktualisiert alle Clients

---

## Phase 7: Audio File Transfer
> **Ziel:** Tracks teilen zwischen Geräten, die den Track nicht lokal haben.

- [ ] 7.1 Track-Matching Logik: Titel + Artist + Duration (±2s) → Beide haben den Track?
- [ ] 7.2 `TrackTransferOffer` bei Play: Server broadcastet Track-Info, Clients prüfen lokal
- [ ] 7.3 WebSocket Binary Frame Streaming: 4KB Chunks, Opus 128kbps Transcoding
  - [ ] Sender: MediaCodec oder FFmpeg-lite für Opus-Encoding
  - [ ] Empfänger: Custom `DataSource` für Media3 das aus Buffer liest
- [ ] 7.4 Jitter Buffer: 500ms initial, adaptive (200ms bei stabiler Verbindung)
- [ ] 7.5 Supabase Storage Upload: Parallel im Hintergrund für späteren Zugriff
- [ ] 7.6 Fallback: Wenn Transfer zu langsam → Skip zum nächsten gemeinsamen Track
- [ ] 7.7 UI: Transfer-Indikator (Buffering-Animation, Download-Progress)
- [ ] 7.8 **Verify:** User A spielt Track, User B (ohne Track) hört synchronisiert mit

### Review Phase 7:
- [ ] Transfer startet innerhalb 1s nach Play-Command
- [ ] Buffering-Zeit < 3s bei normalem LTE
- [ ] Audio-Qualität bleibt bei Opus 128kbps akzeptabel
- [ ] Kein Memory Leak bei langem Streaming

---

## Phase 8: Voice Chat (LiveKit)
> **Ziel:** Voice Chat in der Session, Mute/Unmute, Music Ducking.

- [ ] 8.1 LiveKit Cloud Account + API Keys
- [ ] 8.2 Token-Generierung: Ktor Server Endpoint `/voice/token` mit LiveKit Server SDK
- [ ] 8.3 `VoiceRepositoryImpl.kt` — Connect, Disconnect, SetMicEnabled, ObserveParticipants
- [ ] 8.4 `VoiceViewModel` — VoiceState (isConnected, isMuted, participants, activeSpeaker)
- [ ] 8.5 Music Ducking: player.volume = 0.25f wenn Voice aktiv, 1.0f wenn Voice inaktiv
- [ ] 8.6 UI: `VoiceOverlay` — Floating Composable mit Mute-Button + Active Speaker Indicator
- [ ] 8.7 UI: `VoiceParticipantChip` — Avatar + Name + Speaking Indicator (animierter Ring)
- [ ] 8.8 UI: `MuteButton` — Toggle mit Animation (Mic ↔ MicOff Icon)
- [ ] 8.9 Permission: RECORD_AUDIO mit Rationale Dialog
- [ ] 8.10 Push-to-Talk Modus (optional, aktivierbar in Settings)
- [ ] 8.11 **Verify:** Voice Chat funktioniert parallel zu Musik, kein Echo, Ducking smooth

### Review Phase 8:
- [ ] Latenz Voice < 300ms
- [ ] Echo Cancellation funktioniert
- [ ] Music ducking smooth (kein abrupter Volume-Sprung)
- [ ] Mute/Unmute reagiert sofort

---

## Phase 9: Social Features (Reactions, GIFs, Chat, Voting)
> **Ziel:** Alle interaktiven Features implementiert und funktional.

### Emoji Reactions
- [ ] 9.1 `Reaction.kt` Domain Model + `FloatingReaction` UI Model
- [ ] 9.2 `ReactionRepository` — Send via Supabase Broadcast, Observe incoming
- [ ] 9.3 `ReactionOverlay.kt` — Floating Emojis mit graphicsLayer Animation
  - [ ] Y: -800dp über 2s
  - [ ] Alpha: 1→0
  - [ ] Random X offset (20-80% screen width)
  - [ ] Random Rotation (±15°)
  - [ ] Max 20 gleichzeitig
- [ ] 9.4 `EmojiPicker.kt` — Row mit 8-10 Emoji Buttons (❤️🔥😍🎵🤯💀👏🎉)
- [ ] 9.5 Haptic Feedback beim Senden

### GIF Sharing
- [ ] 9.6 Giphy SDK einbinden + API Key
- [ ] 9.7 `GifPicker.kt` — Giphy Dialog Integration in Compose
- [ ] 9.8 GIF-URL via Supabase Broadcast senden
- [ ] 9.9 GIF-Anzeige via Coil GIF Decoder im Chat
- [ ] 9.10 Rate Limit: Max 1 GIF pro 5 Sekunden pro User

### Chat
- [ ] 9.11 `ChatMessage.kt` Domain Model
- [ ] 9.12 `ChatRepository` — Supabase Broadcast für ephemere Messages
- [ ] 9.13 `ChatSheet.kt` — Bottom Sheet mit MessageList + Input
- [ ] 9.14 `ChatBubble.kt` — Sender Name, Message, Timestamp, optional GIF
- [ ] 9.15 `MessageInput.kt` — TextField + Send Button + GIF Button

### Track Voting
- [ ] 9.16 `VotingRepository` — Supabase Postgres CRUD für track_requests + votes
- [ ] 9.17 Realtime-Subscription auf track_requests (Postgres Changes)
- [ ] 9.18 `VotingViewModel` — Queue State, Vote Actions, Request Track
- [ ] 9.19 `QueueScreen.kt` — LazyColumn mit QueueTrackItems, sortiert nach Score
- [ ] 9.20 `QueueTrackItem.kt` — Track Info + Upvote/Downvote Buttons + Score Badge
- [ ] 9.21 `AddToQueueFAB.kt` — FAB → Sheet zum Track aus Library wählen
- [ ] 9.22 Auto-Play: Wenn aktueller Track endet → nächster Track mit höchstem Score
- [ ] 9.23 **Verify:** Reactions floaten, GIFs laden, Chat funktioniert, Voting aktualisiert Queue

### Review Phase 9:
- [ ] Reactions sind flüssig (60fps) auch bei 20 gleichzeitigen
- [ ] GIFs laden schnell (< 1s)
- [ ] Chat-Nachrichten erscheinen sofort bei allen Teilnehmern
- [ ] Voting-Score aktualisiert sich in Echtzeit

---

## Phase 10: Polish, Testing & Release-Prep
> **Ziel:** Produktionsreife, Performance, Edge Cases, Build-Optimierung.

### UI Polish
- [ ] 10.1 Audio Waveform Visualizer auf FullPlayerScreen (compose-audiowaveform)
- [ ] 10.2 Lottie-Animationen für Reaction-Effekte (Herzen, Feuer, Konfetti)
- [ ] 10.3 Loading States für alle Screens (Shimmer/Skeleton)
- [ ] 10.4 Error States mit Retry-Buttons
- [ ] 10.5 Empty States (keine Tracks, keine Session, leere Queue)
- [ ] 10.6 Splash Screen (Core Splash Screen API)
- [ ] 10.7 App Icon + Adaptive Icon
- [ ] 10.8 Smooth Color-Transitions bei Track-Wechsel (MaterialKolor animate)

### Performance
- [ ] 10.9 ProGuard/R8 Rules für alle Libraries
- [ ] 10.10 Baseline Profiles generieren
- [ ] 10.11 Memory-Leak Check (LeakCanary) besonders bei WebSocket + LiveKit
- [ ] 10.12 Battery-Impact prüfen (Foreground Service + WebSocket + Voice)

### Edge Cases
- [ ] 10.13 Offline-Handling: Graceful Degradation wenn Server nicht erreichbar
- [ ] 10.14 Session-Cleanup: Auto-End nach 30min Inaktivität
- [ ] 10.15 Max Participants Limit (8) enforced
- [ ] 10.16 Audio Focus: Pausiert bei Anruf, resumed danach
- [ ] 10.17 Backgrounding: Service läuft weiter, WebSocket bleibt verbunden
- [ ] 10.18 Orientation Change: State bleibt erhalten
- [ ] 10.19 Large Library Handling: 10.000+ Tracks Performance-Test

### Testing
- [ ] 10.20 Unit Tests: Alle UseCases (min 80% Coverage)
- [ ] 10.21 Unit Tests: SyncEngine + DriftCorrector
- [ ] 10.22 Unit Tests: ViewModels mit Turbine
- [ ] 10.23 Integration Tests: WebSocket Sync zwischen 2 Clients
- [ ] 10.24 UI Tests: Session Join Flow, Voting Flow

### Release
- [ ] 10.25 Signing Config (Release Keystore)
- [ ] 10.26 `./gradlew assembleRelease` — APK < 30MB
- [ ] 10.27 Server Deployment (Railway/Fly.io) + Environment Variables
- [ ] 10.28 README.md mit Setup-Anleitung
- [ ] 10.29 **Final Verify:** 2-Device Test, kompletter Flow von Login → Session → Sync → Voice → Reactions → End

### Review Phase 10:
- [ ] Kein ANR, kein Crash bei normalem Usage
- [ ] Cold Start < 2s
- [ ] Sync-Drift < 200ms bei normalem LTE
- [ ] Voice + Music + Reactions gleichzeitig ohne Stutter

---

## 📊 Fortschritts-Tracking

| Phase | Status | Tasks | Done | % |
|---|---|---|---|---|
| 1 - Grundgerüst | ⬜ Not Started | 13 | 0 | 0% |
| 2 - Library | ⬜ Not Started | 15 | 0 | 0% |
| 3 - Playback | ⬜ Not Started | 14 | 0 | 0% |
| 4 - Backend & Auth | ⬜ Not Started | 11 | 0 | 0% |
| 5 - Sync Server | ⬜ Not Started | 12 | 0 | 0% |
| 6 - Client Sync | ⬜ Not Started | 11 | 0 | 0% |
| 7 - File Transfer | ⬜ Not Started | 8 | 0 | 0% |
| 8 - Voice Chat | ⬜ Not Started | 11 | 0 | 0% |
| 9 - Social | ⬜ Not Started | 23 | 0 | 0% |
| 10 - Polish | ⬜ Not Started | 29 | 0 | 0% |
| **TOTAL** | | **147** | **0** | **0%** |
