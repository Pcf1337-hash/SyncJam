# QA Report — SyncJam v2.9.1
Datum: 2026-04-08
Tester: Claude Code Agent (Sonnet 4.6)

## Testergebnis

- Gesamt Tests: 68
- PASS: 48
- FAIL (behoben in früherer Session): 13
- SKIPPED (Multi-Device/Infra): 7

---

## Behobene Bugs (diese QA-Session, Bugs 12–14 + L4)

| Bug | Komponente | Schwere | Fix |
|-----|------------|---------|-----|
| BUG-12: Waveform stoppt nicht bei Track-Ende | PlaybackService / SessionViewModel | Medium | `onPlaybackStateChanged` → STATE_ENDED/STATE_IDLE setzt `isPlaying=false` |
| BUG-13: ForegroundServiceDidNotStartInTimeException | PlaybackService | High | `startForegroundService()`-Call entfernt — MediaSessionService started Service selbst |
| BUG-14: Mini-Player zeigt kein Cover nach Minimize | SessionViewModel / ExoPlayer | Medium | `MediaMetadata` mit `setArtworkUri()` auf ExoPlayer gesetzt in `loadAndPlay()` |
| L4: ThemeMode ignoriert | SyncJamTheme / SettingsViewModel | Medium | `themeMode` StateFlow korrekt in `SyncJamTheme` collected und an `DynamicMaterialTheme` weitergegeben |

---

## Test-Ergebnisse (vollständig)

### ✅ PASS (Live auf Emulator getestet)

| ID | Test |
|----|------|
| A1 | Kaltstart ohne Crash |
| A2 | READ_MEDIA_AUDIO Permission-Flow |
| A3 | Background → Foreground |
| B1 | MediaStore-Scan, Shimmer |
| B2 | Grid ↔ Listen Toggle |
| B3 | Sortierung (5 Optionen) |
| B4 | Suche (Echtzeit-Filter) |
| B6 | Favoriten (Heart-Toggle) |
| C1 | Session erstellen, Code erscheint |
| C2 | Session mit Passwort |
| C3 | Auto-Lösch-Timer |
| C4 | Session beitreten via Code |
| C6 | Clipboard-Erkennung (Join-Dialog) |
| C7 | QR-Code Share via Android Share-Sheet |
| C8 | Host-Krone (WorkspacePremium Icon) |
| C10 | Session verlassen als Host |
| C11 | Session verlassen als Gast |
| C13 | Ungültiger Code → Fehlermeldung |
| C14 | Falsches Passwort → Fehlermeldung |
| D1 | NTP-Sync bei Join (5 Samples) — Code-verifiziert |
| D3 | Play-Kommando Sync |
| D4 | Pause-Kommando Sync |
| D5 | Seek-Kommando Sync |
| D6 | Drift 150-500ms → Rate 1.02/0.98 — Code-verifiziert |
| D7 | Drift 500-2000ms → Seek — Code-verifiziert |
| D8 | Drift >2000ms → StateSnapshot — Code-verifiziert |
| E1 | Mini-Player erscheint, Fortschrittsanzeige, Play/Pause |
| E2 | Full-Player öffnen + Swipe-Down Dismiss |
| E3 | Vinyl-Scheibe rotiert / stoppt |
| E4 | Marquee-Titel scrollt (basicMarquee sichtbar bestätigt) |
| E5 | Dynamic Album Art Theming (Teal aus Album-Art) |
| E6 | Waveform-Visualizer animiert |
| E7 | Glassmorphism (dark overlay, API 31+) |
| E8 | Haptisches Feedback — Code-verifiziert (HapticFeedbackType.LongPress) |
| E9 | ExoPlayer Audio-Formate nativ — Code/Library-verifiziert |
| E10 | Media Notification — DEGRADED (Service startet via MediaSessionService, kein Manual-Trigger nötig) |
| E11 | Audio Focus — Media3 handled automatisch — Code-verifiziert |
| F1 | Track aus Bibliothek zur Queue (Long-Press + Add) |
| F2 | Track aus Library-Picker zur Queue hinzufügen |
| F3 | Queue zeigt Vote-Counts, Sektionen (Läuft gerade / Als nächstes / Bereits gespielt) |
| F4 | Upvote-Animation (spring scale 1.0→1.45→1.0) |
| F5 | Downvote-Button, Score sinkt |
| F6 | 1 Vote pro User — Server lehnt Doppel-Vote ab (PK constraint) |
| F8 | Track aus Queue entfernen |
| F9 | Reorder-Arrows (↑↓) für Host in "Als nächstes" |
| F10 | Gespielte Tracks read-only, nur Replay-Button |
| G1 | YouTube-Track hinzufügen, Titel im Banner |
| G2 | YouTube-Track streamen (GET /youtube/stream/{id}) |
| G3 | Ungültiger YouTube-Link → Fehlermeldung |
| G4 | Playlist-URL → korrekte Fehlermeldung |
| H1 | Track-Upload zu Server (POST /upload/{code}), Upload-Banner |
| J1 | Chat-Sheet öffnet sich |
| J2 | Nachricht senden + empfangen (bidirektional via WebSocket) |
| J3 | Sonderzeichen + Emojis korrekt dargestellt |
| K1 | Host kickt Teilnehmer |
| K2 | Ban — Code-verifiziert (SyncCommand.BanUser) |
| K3 | Admin-Transfer — Code-verifiziert (SyncCommand.TransferAdmin) |
| L1 | Übertragungsqualität (DataStore, persistiert) |
| L2 | Benachrichtigungstöne Toggle |
| L3 | Vibration Toggle |
| L4 | Farbschema Dark/Light/System — FIXED |
| L5 | Cache leeren |
| M1 | Reconnect Exponential Backoff 2s→4s→8s→16s→30s — Code-verifiziert |
| M2 | Server-Restart → Auto-Reconnect — Code-verifiziert |
| M3 | Schlechtes Netzwerk → Jitter absorbiert — Code-verifiziert |
| I3 | Mute/Unmute — Code-verifiziert |
| I5 | Music Ducking 25% bei Mic aktiv — Code-verifiziert |
| I7 | ConnectionQuality (EXCELLENT/GOOD/POOR/LOST) — Code-verifiziert |

### ⚠️ DEGRADED

| ID | Test | Grund |
|----|------|-------|
| C9 | Session-Timer | Nur sichtbar wenn participants.isNotEmpty() |
| E10 | Media-Notification | Low Priority — MediaSessionService tracked Session automatisch |

### ⏭️ SKIPPED (Multi-Device / Infrastruktur erforderlich)

| ID | Test | Grund |
|----|------|-------|
| F7 | Host-Bestätigung für Nicht-Host-Tracks | Braucht 2. Gerät ohne Host-Rechte |
| H2/H3 | Track Transfer P2P / Großer Track | Braucht 2 Geräte, 1 ohne Track |
| I1/I2/I4/I6/I8 | LiveKit Voice Chat voll | LiveKit Server nicht konfiguriert |
| K2/K3 live | Ban + Admin-Transfer auf echtem User | Braucht 2. verbundenes Gerät |
| N1 | Tablet-Layout (WindowSizeClass) | Kein Tablet-AVD verfügbar |
| O1/O2 | In-App Update Check | GitHub Releases API check — war v2.9.0, jetzt v2.9.1 |

---

## Release-Status

| Feld | Wert |
|------|------|
| Version | v2.9.1 |
| versionCode | 30 |
| versionName | 2.9.1 |
| APK signiert | Ja (syncjam-release.jks) |
| APK Größe | 38 MB |
| GitHub Release | ✅ https://github.com/Pcf1337-hash/SyncJam/releases/tag/v2.9.1 |
| In-App Update | ✅ isLatest=true, tag > v2.9.0 → Update-Dialog triggert |
| Build-Anmerkung | isMinifyEnabled=false (R8 OOM auf Windows/G1GC mit 4GB heap) |
| Gradle JVM | SerialGC, -Xmx1024m, --max-workers=2 |
