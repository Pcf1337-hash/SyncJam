# SyncJam Modernisierung — TODO
> Quelle: `deep-research-report.md` | Stand: 2026-04-05
> Bereits erledigte Phasen (v2.0–v2.3) sind hier NICHT enthalten.
> Kein Spotify/Apple, kein Video, keine Lyrics.

---

## 1 — Home Screen / Session-Lobby

- [x] **QR-Code Scanner** für Session-Beitritt (ZXing) ✅ v2.6.0
- [x] **Session-Verlaufsliste** mit Album-Art Thumbnails (SessionHistoryCard, recentSessions) ✅
- [x] **Oeffentlicher Session-Feed** — PublicSessionsTab mit scrollbarer Liste ✅
- [x] **Warteraum-Animationen** — VinylIdleAnimation wenn keine Sessions ✅
- [x] **Shimmer Loading** — ShimmerSessionCard beim Laden ✅ v2.6.0

---

## 2 — Bibliothek

- [ ] **Paging fuer grosse Sammlungen** — Jetpack Paging Compose (`LazyPagingItems`) _(Nice-to-Have)_
- [x] **"Zuletzt gespielt" / "Am haeufigsten"** — horizontales Cover-Karussell ✅ v2.6.0
- [ ] **BlurHash-Placeholder** fuer Albumcover _(externe Lib benötigt, Nice-to-Have)_
- [x] **Leere Zustaende & Permissions** — freundliche Erklaertexte + Permission-Button ✅ v2.6.0

---

## 3 — Vollbild-Player

- [x] **Parallax-Effekt** fuer Cover (`Modifier.graphicsLayer`) ✅
- [ ] **Shared Element Transition** Mini-Player <-> Fullscreen _(Compose Navigation noch kein stabiles API)_
- [x] **Waveform-Visualizer** im Progress-Slider (WaveformProgressBar Canvas) ✅

---

## 4 — Mini-Player-Leiste

- [x] **Pulsierender Spiel-Indikator** — animierter Play-Button mit spring-Scale ✅
- [x] **Radial-Progress** — Canvas Arc um Play-Icon ✅

---

## 5 — Voice-Chat Overlay

- [x] **Netzwerkqualitaets-Anzeige** — NetworkQualityBars (3 Balken) in VoiceParticipantChip ✅
- [ ] **Auto-Ducking via AudioFocus** — `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` _(Nice-to-Have)_

---

## 6 — Emoji-Reaktionen

- [x] **Reaktions-Burst** — Doppel-Tap → 5 zufällige Emojis (BURST_COUNT=5) ✅
- [x] **"Wer hat reagiert"** — LongPress Tooltip mit senderName ✅

---

## 7 — GIF-Sharing

- [x] **Vorschau vor dem Senden** — GifPreviewDialog mit Bestätigung ✅
- [x] **Rate-Limit Countdown UI** — visueller Countdown auf Sende-Button ✅

---

## 8 — Chat

- [x] **Ungelesene-Badge** — Badge-Counter auf Chat-Handle, reset bei Öffnen ✅
- [x] **"Tippt..."-Anzeige** — Typing-Indicator AnimatedVisibility ✅
- [x] **Zeitstempel** — timeFormat HH:mm unten rechts in ChatBubble ✅

---

## 9 — Queue & Voting

- [x] **Track-Transfer-Indikator** — CloudUpload-Icon + CircularProgressIndicator ✅
- [x] **Host Drag-Reorder** — Up/Down-Buttons für Host (ReorderQueue Event) ✅ v2.6.0
- [x] **Vote-Animation** — spring-Scale 1.0→1.45→1.0 auf Upvote-Icon ✅

---

## 10 — Session Header / Teilnehmer

- [x] **Online-Indikator** — grüner Dot an Avataren ✅
- [x] **Session-Name editierbar** — nur Host, AlertDialog mit TextField ✅
- [x] **Einladung via Share-Intent** — Android Share Sheet mit Session-Code ✅

---

## 11 — Auth & Profil

- [x] **AuthRepositoryImpl** mit Supabase Auth ✅
- [x] **ProfileSheet** — Avatar-Upload, Anzeigename, Avatar-Anzeige in ProfileTab ✅
- [x] **Avatar in Teilnehmerliste** — Coil AsyncImage in ParticipantAvatarRow ✅

---

## 12 — Sync Engine

- [x] **Kronos NTP** — eigene NtpClockSync (5 Samples, Min-RTT, 30s Refresh) ✅
- [x] **State-Snapshot bei Rejoin** — StateSnapshot Handler + Reconnect-Logic ✅
- [x] **Host-Wechsel** — HostDisconnected → autoPromote + TransferHost Command ✅
- [x] **Latenz-Debug-Overlay** — Debug-Overlay in SessionScreen (toggle via Tap) ✅ v2.6.0

---

## 13 — Audio-Datei-Transfer

- [x] **Track-Transfer-Indikator** — Cloud-Icon + Progress in QueueTrackItem ✅
- [ ] **Custom DataSource fuer Media3** _(Sehr komplex, Nice-to-Have)_
- [ ] **Parallel-Upload** via Coroutines async _(In Progress)_

---

## 14 — Allgemeine UI/UX

- [x] **MediaSession + Notification** — PlaybackService mit MediaStyle ✅
- [x] **Adaptive Tablet-Layout** — WindowSizeClass zwei-Spalten Layout ✅ v2.6.0
- [x] **Onboarding** — 3 Intro-Screens, DataStore Flag ✅
- [x] **Shimmer/Skeleton Loading** — HomeScreen + LibraryScreen ✅ v2.6.0
- [x] **Haptik fuer Controls** — LocalHapticFeedback in Player ✅

---

## 15 — Feature-Ideen (Nice-to-Have)

- [ ] **DJ-Rotation** _(Future)_
- [ ] **Themen-Raeume** _(Future)_
- [ ] **Kollaborative Playlisten** _(Future)_
- [ ] **Community-Features** _(Future)_
- [ ] **Discord/Telegram Bot** _(Future)_

---

## 16 — Einstellungen (Erweiterung)

- [x] **Audio-Qualitaet** — Niedrig/Mittel/Hoch ✅
- [x] **Cache-Verwaltung** — Speicher anzeigen + leeren ✅
- [x] **Design-Override** — System/Hell/Dunkel ✅

---

## 17 — Server & Deployment

- [x] `/time` Endpoint aktiv auf 159.195.63.246:8080 ✅
- [ ] LiveKit env vars prüfen (LIVEKIT_API_KEY, LIVEKIT_API_SECRET, LIVEKIT_WS_URL)
- [x] Server läuft (`/health` → ok) ✅

---

## 18 — Testing & Release

- [x] Build erfolgreich ohne Compile-Errors ✅ v2.6.0
- [x] Emulator-Test Home Screen, Library, Login ✅
- [ ] README.md aktualisieren mit v2.6.0 Features
- [x] GitHub Release v2.6.0 erstellt mit APK-Asset ✅
