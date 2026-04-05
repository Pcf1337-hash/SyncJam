# SyncJam v2.0 — Modernisierung TODO
> Stand: 2026-04-05 | Vollständiger Plan: `C:\Users\Administrator\.claude\plans\functional-whistling-clarke.md`
> Research-Report: `C:\Users\Administrator\Downloads\deep-research-report.md`

## WICHTIG BEI CONTEXT-COMPACT
Falls der Kontext zurückgesetzt wird — diese Agents sind bereits FERTIG:
1. ✅ Social Features (16 Dateien in feature/social/)
2. ✅ Voice Chat (echte LiveKit Integration)
3. ✅ Player + Home + Library Modernisierung
4. ✅ Sync Engine + Session Header + Queue
5. 🔄 Theming + Settings → Agent läuft noch (a4a9cd22da1b51227) — falls nicht fertig, Phase 4+11 manuell
6. ⏳ Phase 10 (Auth/Profil), 13 (UI/UX), 14 (Server), 15 (Test), 16 (README), 17 (Release) → noch ausstehend

---

## Phase 1 — Dependency Updates ✅ ERLEDIGT
- [x] media3: 1.6.1 → 1.9.0 (`gradle/libs.versions.toml`)
- [x] livekit: 2.9.0 → 2.23.5
- [x] palette-ktx 1.0.0 hinzugefügt
- [x] paging-compose 3.3.6 hinzugefügt
- [x] kronos-android 0.0.1-alpha10 hinzugefügt
- [x] zxing-android 4.3.0 hinzugefügt
- [x] giphy-sdk Library-Eintrag hinzugefügt
- [x] app/build.gradle.kts — alle neuen deps + LIVEKIT_URL BuildConfig

---

## Phase 2 — Home Screen ✅ ERLEDIGT
- [x] Clipboard-Erkennung für Session-Code (LaunchedEffect + ClipboardManager + AlertDialog)
- [x] VinylIdleAnimation Composable (rotierende Vinyl-Scheibe in Compose)
- [x] HomeViewModel: detectedClipboardCode StateFlow + onClipboardCodeDetected + dismissClipboardDialog
- [ ] Session-Verlauf mit Album-Art Thumbnails ⏳ (ausstehend)
- [ ] Öffentlicher Session-Feed ⏳ (ausstehend)

---

## Phase 3 — Library Verbesserungen ✅ ERLEDIGT
- [x] Album-Grid / Listen-Toggle mit AnimatedContent (LazyVerticalGrid ↔ LazyColumn)
- [x] Sort/Filter BottomSheet (ModalBottomSheet + SortOption enum, 5 Optionen)
- [x] Shimmer-Placeholder ShimmerItem via graphicsLayer (nie .alpha Modifier)
- [x] SortOption in LibraryUiState + toggleViewMode() + applySorting()
- [ ] „Zuletzt gespielt" / „Am häufigsten" horizontales Karussell ⏳ (ausstehend)

---

## Phase 4 — Dynamic Album Art Theming ✅ ERLEDIGT
- [x] `core/ui/theme/AlbumArtColorExtractor.kt` — Coil 3 + Palette API, 100×100 IO-Thread
- [x] `core/ui/theme/SyncJamTheme.kt` — war bereits korrekt mit DynamicMaterialTheme
- [x] SessionViewModel — dominantColor StateFlow, updateDominantColor() bei Track-Wechsel

---

## Phase 5 — Vollbild-Player Modernisierung ✅ ERLEDIGT
- [x] Vinyl-Rotation (infiniteRepeatable tween 4000ms, lastRotation snap bei Pause)
- [x] Marquee: Modifier.basicMarquee(iterations = Int.MAX_VALUE) auf Titel
- [x] Haptic: LocalHapticFeedback auf Play/Pause/Skip Prev/Next
- [x] Glassmorphism: API31+ RenderEffect.createBlurEffect, Fallback semi-transparent
- [x] Swipe-Down Dismiss: Animatable + detectDragGestures, Schwelle 200dp, spring-back
- [x] MiniPlayer: LinearProgressIndicator 2dp + pulsierender Play-Indikator
- [ ] Shared Element Transition Mini↔Full ⏳ (komplex, nach NavGraph-Check)

---

## Phase 6 — Social Features ✅ ERLEDIGT
Alle neuen Dateien unter: `app/src/main/java/com/syncjam/app/feature/social/`

- [x] `domain/model/Reaction.kt` (@Immutable)
- [x] `domain/model/ChatMessage.kt` (@Immutable, isOwn flag)
- [x] `domain/model/GifResult.kt`
- [x] `domain/repository/ReactionRepository.kt`
- [x] `domain/repository/ChatRepository.kt`
- [x] `data/ReactionRepositoryImpl.kt` (Stub, Supabase TODOs markiert)
- [x] `data/ChatRepositoryImpl.kt` (Stub, Supabase TODOs markiert)
- [x] `data/GiphyDataSource.kt` — suspendCancellableCoroutine um GPHCore
- [x] `presentation/components/FloatingEmoji.kt` — graphicsLayer translationY+alpha, LongPress Tooltip
- [x] `presentation/components/EmojiPicker.kt` — ModalBottomSheet, 8 Quick + LazyVerticalGrid 32 Emojis
- [x] `presentation/ReactionOverlay.kt` — max 20, Double-Tap Burst 5 Emojis, FAB
- [x] `presentation/components/ChatBubble.kt` — isOwn rechts/links, asymm. RoundedCornerShape
- [x] `presentation/components/MessageInput.kt` — 500-Zeichen-Limit, IME Send Action
- [x] `presentation/ChatSheet.kt` — auto-scroll, Typing-Indicator AnimatedVisibility, Unread-Badge
- [x] `presentation/components/GifPicker.kt` — 2-Spalten Grid, Rate-Limit Countdown, Preview-Dialog
- [x] `presentation/SocialViewModel.kt` — @HiltViewModel, ImmutableList, supervisorScope
- [ ] Social Features in SessionScreen einbinden ⏳ (nach Theming-Agent)

---

## Phase 7 — Voice Chat Real Integration ✅ ERLEDIGT
- [x] ConnectionQuality Enum (EXCELLENT/GOOD/POOR/LOST/UNKNOWN) in VoiceParticipant.kt
- [x] VoiceState: anyoneSpeaking + isPttRecommended computed properties
- [x] SpeakingIndicator — pulsierender Ring (graphicsLayer scale 1.0→1.15, alpha 0.4→1.0)
- [x] VoiceParticipantChip — NetworkQualityBars 3 Balken (tertiary/secondary/error)
- [x] VoiceRepositoryImpl — echte Room.connect(), ActiveSpeakersChanged Events, ConnectionQualityChanged
- [x] VoiceViewModel — isPttMode, anyoneSpeaking StateFlow, onPttPressed/Released
- [x] VoiceOverlay — PTT-Hinweis, SpeakingIndicator integriert
- [x] VoiceModule — Room als Hilt Singleton via LiveKit.create()
- [x] SyncJamApp — LiveKit.init() in onCreate()
- [x] buildConfigField LIVEKIT_URL bereits gesetzt

---

## Phase 8 — Queue & Voting Verbesserungen ✅ ERLEDIGT
- [x] `feature/voting/presentation/QueueScreen.kt` — Modifier.animateItem() auf Items
- [x] Vote-Animation: spring-based Scale-Up (1.0→1.45→1.0) auf ThumbUp-Icon
- [ ] Track-Transfer-Indikator: Cloud-Icon + CircularProgressIndicator
- [ ] Host Drag-Reorder Controls (nur Host)

---

## Phase 9 — Session Header ✅ ERLEDIGT
- [x] Host-Krone Icons.Default.WorkspacePremium über Avatar (tertiary Farbe)
- [x] Session-Timer HH:MM:SS mit LaunchedEffect
- [x] Invite-Button war bereits vorhanden
- [ ] Session-Name editierbar (nur Host): tap → AlertDialog mit TextField

---

## Phase 10 — Auth & Profil ⏳ AUSSTEHEND
- [ ] `feature/auth/data/AuthRepositoryImpl.kt` erstellen
- [ ] `feature/profile/presentation/ProfileSheet.kt` erstellen
  - ImagePicker-Intent für Avatar
  - Supabase Storage upload: `storage.from("avatars").upload(userId, bytes)`
  - Coil AsyncImage für Avatar-Anzeige
- [ ] Avatar-URL in ParticipantAvatarRow anzeigen

---

## Phase 11 — Einstellungen-Screen ✅ ERLEDIGT
- [x] `feature/settings/presentation/SettingsScreen.kt` — 4 Sektionen: Audio, Benachrichtigungen, Erscheinungsbild, Speicher
- [x] `feature/settings/presentation/SettingsViewModel.kt` — DataStore Preferences (syncjam_settings)
- [x] Route Settings in SyncJamNavGraph.kt + HomeScreen Settings-Button im Profil-Tab
- [x] datastore = "1.1.4" in libs.versions.toml + build.gradle.kts

---

## Phase 12 — Sync Engine ✅ ERLEDIGT
- [x] `sync/NtpClockSync.kt` — 5 Samples bei Join, Min-RTT, 30s Auto-Refresh
- [x] `sync/DriftCorrector.kt` — Thresholds: <150 nix, 150-500 Rate, 500-2000 Seek, >2000 Resync, pitch=1.0f
- [x] `sync/SyncStateMachine.kt` erstellt — SessionSyncState enum, vollständige Transition-Tabelle
- [x] Server `/time` Endpoint in `Routing.kt` hinzugefügt

---

## Phase 13 — Allgemeine UI/UX ⏳ AUSSTEHEND
- [ ] `feature/player/data/PlaybackService.kt` — MediaSession mit NotificationCompat.MediaStyle
- [ ] Lockscreen Controls (Play/Pause/Skip)
- [ ] Adaptive Tablet-Layout: WindowSizeClass in MainActivity oder NavGraph
- [ ] Onboarding: `feature/onboarding/` — 3 Screens, DataStore `onboarding_completed` Flag
- [ ] Shimmer-Loader in HomeScreen

---

## Phase 14 — Server-Deployment ⏳ AUSSTEHEND
```bash
ssh root@159.195.63.246     # PW: 1djvVTWgZ1ozUxW
cd /opt/syncjam/
cat docker-compose.yml      # LiveKit + Supabase env vars prüfen
./gradlew :server:shadowJar
scp server/build/libs/server-all.jar root@159.195.63.246:/opt/syncjam/server.jar
docker-compose down && docker-compose up -d
curl http://159.195.63.246:8080/health
```
- [ ] /time Endpoint auf Server aktiv
- [ ] LiveKit env vars gesetzt (LIVEKIT_API_KEY, LIVEKIT_API_SECRET, LIVEKIT_WS_URL)
- [ ] Server läuft nach Deployment

---

## Phase 15 — Build & Emulator Testing ⏳ AUSSTEHEND
```bash
./gradlew :app:assembleDebug   # Build testen
./gradlew :app:installDebug    # Auf Emulator installieren
```
MCP Tools: `mcp__android-toolkit__take-screenshot`, `mcp__android-toolkit__inject-input`, `mcp__android-toolkit__manage-logcat`

- [ ] Build erfolgreich ohne Compile-Errors
- [ ] Screenshot Home Screen
- [ ] Screenshot Library (Grid + Listen Toggle)
- [ ] Screenshot Full Player (Vinyl-Animation, Marquee)
- [ ] Emoji Reaktion tippen → FloatingEmoji erscheint
- [ ] Chat Sheet öffnen → Bubbles korrekt
- [ ] Voice Chat Mic tippen → LiveKit Logcat
- [ ] Logcat — keine Crashes/Exceptions
- [ ] Sync Test (Session erstellen + beitreten)

---

## Phase 16 — README Update ⏳ AUSSTEHEND
- [ ] README.md komplett neu schreiben
  - App-Beschreibung + Screenshots
  - Feature-Liste v2.0
  - Tech-Stack-Tabelle (aktuelle Versionen)
  - Setup-Anleitung
  - Server-Deployment (Docker, SSH)

---

## Phase 17 — GitHub Release v2.0.0 ⏳ AUSSTEHEND
> In-App Updater liest: api.github.com/repos/Pcf1337-hash/SyncJam/releases/latest
> Vergleicht tag_name vs VERSION_NAME — tag muss > "1.9.0" sein → v2.0.0 löst Update-Dialog aus
> APK muss als Asset angehängt sein (erkennt erste .apk Datei via browser_download_url)

- [ ] `app/build.gradle.kts`: versionCode = 11, versionName = "2.0.0"
- [ ] `./gradlew :app:assembleRelease`
- [ ] APK liegt unter: `app/build/outputs/apk/release/app-release.apk`
- [ ] GitHub Release erstellen:
  ```bash
  gh release create v2.0.0 \
    --title "SyncJam v2.0.0 — Social Features, Voice Chat & UI Modernization" \
    --notes "## Was ist neu
  - Floating Emoji Reaktionen mit Animation & Burst-Effekt
  - Ephemerer Chat (Supabase Realtime)
  - GIF-Sharing via Giphy SDK
  - Echter LiveKit Voice Chat mit Speaking-Indikatoren
  - Push-to-Talk Modus ab 4 Teilnehmern
  - Netzwerkqualitäts-Anzeige
  - Dynamic Album Art Theming (MaterialKolor)
  - Glassmorphism UI (API 31+)
  - Vinyl-Rotation & Marquee im Player
  - Swipe-Down Dismiss
  - Einstellungen-Screen
  - NTP Sync Engine mit Drift-Korrektur
  - Session-Timer & Host-Krone" \
    app/build/outputs/apk/release/app-release.apk
  ```
- [ ] Verify: Release-Tag ist `v2.0.0`, APK-Asset sichtbar → In-App-Update wird getriggert

---

## Kritische Regeln (aus CLAUDE.md + lessons.md)
- **NIEMALS** `collectAsState()` → immer `collectAsStateWithLifecycle()`
- **NIEMALS** `Color(0xFF...)` → immer `MaterialTheme.colorScheme.*`
- **IMMER** `graphicsLayer { }` für animierte Properties (GPU-beschleunigt)
- **IMMER** `LazyColumn` mit `key = { it.id }` + `contentType`
- **IMMER** `supervisorScope` für parallele Coroutines
- **Drift < 500ms** → Playback-Rate Anpassung (0.98/1.02), KEIN Seek
- Nach jeder Korrektur: `tasks/lessons.md` updaten
