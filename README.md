# SyncJam

**Gemeinsam Musik hören — synchronisiert über das Internet.**

SyncJam ist eine Android-App, mit der zwei oder mehr Personen gleichzeitig Musik aus ihren lokalen Bibliotheken hören können — perfekt synchronisiert, mit Voice-Chat, kollaborativer Warteschlange, Emoji-Reaktionen und einem modernen Album-Art-getriebenen Player-UI.

[![Release](https://img.shields.io/github/v/release/Pcf1337-hash/SyncJam?label=latest)](https://github.com/Pcf1337-hash/SyncJam/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green)](https://github.com/Pcf1337-hash/SyncJam/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF)](https://kotlinlang.org)

---

## Funktionen

### Session-Management
- **Session erstellen** mit optionalem Namen, Passwort-Schutz und Auto-Lösch-Timer (1h / 6h / 24h / nie)
- **Öffentliche oder private Sessions** — öffentliche erscheinen im Feed, private nur per Code beitreten
- **6-stelliger Session-Code** — einfaches Teilen per Copy-Paste oder QR
- **Clipboard-Erkennung** — liegt ein Session-Code in der Zwischenablage, fragt die App beim Öffnen automatisch nach
- **Session-Verlauf** — zuletzt besuchte Sessions mit Umbenennen- und Löschen-Option
- **Passwort-Schutz** — nur Personen mit Code + Passwort kommen rein

### Wiedergabe & Synchronisation
- **NTP-basierte Uhrsynchronisation** beim Session-Beitritt (5 Samples, Minimum-RTT)
- **Drift-Korrektur** in drei Stufen: Playback-Rate-Anpassung (< 500 ms), Seek-Korrektur (500–2000 ms), Full-Resync (> 2 s)
- **Nur der Host löst Track-Wechsel aus** — Nicht-Hosts warten auf den Server-Command, kein Doppelspringen in der Queue
- **Admin übernimmt automatisch** wenn der Host die Session verlässt
- **Exponentieller Reconnect-Backoff** (2s → 4s → 8s → 16s → max. 30s)
- **Track-Upload auf Server** — Audio-Dateien werden per Multipart-Upload hochgeladen, damit alle Teilnehmer dieselbe Stream-URL nutzen (kein P2P-Transfer nötig)
- **Unterstützte Formate:** MP3, FLAC, WAV, OGG, AAC, M4A, OPUS

### Player
- **Vollbild-Player** mit rotierender Vinyl-Scheibe, Marquee-Titel, Seek-Slider und Lautstärkeregler
- **Mini-Player-Bar** mit LinearProgressIndicator und pulsierendem Play-Indikator
- **Swipe-Down** zum Schließen des Vollbild-Players
- **Haptisches Feedback** bei Play / Pause / Skip
- **Glassmorphism-Overlays** (API 31+: echter RenderEffect-Blur, Fallback semi-transparent)
- **Dynamic Album Art Theming** — dominante Farbe des Cover-Arts wird als MaterialKolor-Seed verwendet
- **Album-Art Auto-Download** aus MusicBrainz / Cover Art Archive

### Bibliothek
- **MediaStore-Scanner** liest alle lokalen Audio-Tracks automatisch ein
- **Album-Grid / Listen-Ansicht** — umschaltbar mit AnimatedContent
- **Sortierung** nach Titel, Interpret, Album, Dauer, Zuletzt hinzugefügt
- **Suche** in Echtzeit
- **Playlists** — erstellen, bearbeiten, Tracks hinzufügen; direkt aus der Session heraus einfügbar
- **Favoriten** — Tracks markieren und schnell wiederfinden
- **Shimmer-Ladeanimation** während MediaStore gescannt wird

### Kollaborative Warteschlange
- **Tracks hinzufügen** aus der lokalen Bibliothek oder eigenen Playlists
- **YouTube-Integration** — Track per URL zur Session hinzufügen (yt-dlp, Cookie-Support für geo-gesperrte Videos)
- **Track-Bestätigung** — wenn ein Nicht-Host einen Track hinzufügen will, bekommt der Host einen Bestätigungs-Dialog (Akzeptieren / Ablehnen)
- **Voting** (+/−) — Score-basierte Sortierung; nur der noch-nicht-gespielte Teil der Queue wird umsortiert, gespielte Tracks bleiben unveränderlich
- **Track entfernen** aus der Queue (Host / Admin)

### Voice Chat
- **LiveKit WebRTC Voice Chat** — echte Ende-zu-Ende-verschlüsselte Audioübertragung
- **Mikrofon-Toggle** (stumm / aktiv)
- **Push-to-Talk-Modus** (in den Einstellungen aktivierbar)
- **Music-Ducking** — Musik automatisch auf 25 %, wenn das Mikrofon aktiv ist
- **Speaking-Indikator** — pulsierender Ring um den Avatar beim Sprechen
- **Netzwerkqualitäts-Anzeige** (Excellent / Good / Poor / Lost)
- **Floating Avatar Overlay** — zeigt aktive Sprecher kompakt im Player

### Soziale Features
- **Floating Emoji-Reaktionen** — animiert mit Y-Translation + Alpha-Fade, bis zu 20 gleichzeitig auf dem Bildschirm
- **Text-Reaktionen** — eigenen Text als Bubble senden (Anzeigedauer proportional zur Textlänge)
- **Double-Tap Burst** — 5 Emojis auf einmal senden

### Admin-Funktionen (Host / Admin)
- **Kick** — Teilnehmer aus der Session entfernen
- **Ban** — Teilnehmer dauerhaft für diese Session sperren
- **Server-Mute** — Mikrofon eines Teilnehmers deaktivieren
- **Admin übertragen** — Admin-Rechte an einen anderen Teilnehmer weitergeben
- **Direktnachricht** — private Nachricht an einen einzelnen Teilnehmer senden
- **Host-Krone** — visuell erkennbar, wer Host / Admin ist

### Einstellungen
- **Übertragungsqualität** — Niedrig / Mittel / Hoch
- **Push-to-Talk** — Mikrofon nur bei gedrückter Taste aktiv
- **Benachrichtigungstöne** — Töne bei Session-Ereignissen
- **Vibration** — bei Reaktionen und Benachrichtigungen
- **Farbschema** — System / Hell / Dunkel
- **Cache leeren** — temporäre Dateien entfernen

### Sonstiges
- **Profilbild & Anzeigename** — individuell einstellbar, wird in der Session angezeigt
- **Auto-Updater** — prüft beim Start auf neue GitHub-Releases, zeigt scrollbaren Changelog-Dialog
- **Session-Timer** — zeigt, wie lange die aktuelle Session schon läuft

---

## Screenshots

> Coming soon

---

## Installation

### APK direkt installieren (empfohlen)

1. Lade `app-release.apk` aus dem [neuesten Release](../../releases/latest) herunter
2. Aktiviere "Unbekannte Quellen" auf deinem Gerät
3. Installiere die APK
4. Der öffentliche Server läuft bereits — einfach Session erstellen und loslegen

### Build from source

```bash
git clone https://github.com/Pcf1337-hash/SyncJam.git
cd SyncJam
./gradlew :app:assembleDebug
# APK liegt in app/build/outputs/apk/debug/
```

---

## Wie die Synchronisation funktioniert

**Command-Relay-Prinzip** — kein Audio-Restreaming, kein DRM-Problem:

- Jedes Gerät streamt den Track selbst über die Upload-URL vom Server
- Der Server koordiniert nur WAS und WANN gespielt wird
- Tracks werden beim Hinzufügen einmalig auf den Server hochgeladen — alle Teilnehmer nutzen dieselbe HTTP-URL

```
Host                    Server                  Gast
  │── AddToQueue ──────►│                         │
  │                     │── PlaylistUpdate ───────►│
  │◄── PlaylistUpdate ──│                         │
  │                     │                         │
  │── Play(trackId) ───►│── Play(trackId) ────────►│
  │                     │                         │
  │── TrackEnded ──────►│── PlaylistUpdate ───────►│
  │                     │── Play(nextTrack) ──────►│
```

### NTP Clock-Sync

```
Client          Server
  │─── T1 ─────►│ T2
  │             │ T3
  │◄─ T2,T3 ───│ T4

RTT    = (T4 − T1) − (T3 − T2)
Offset = [(T2 − T1) + (T3 − T4)] / 2
```

5 Samples beim Join, Minimum-RTT gewinnt. Alle 30 Sekunden automatischer Refresh.

### Drift-Korrektur

| Drift | Aktion |
|---|---|
| < 150 ms | Keine Korrektur |
| 150–500 ms | Playback-Rate ±2 % |
| 500–2000 ms | Seek-Korrektur |
| > 2000 ms | Full Resync via StateSnapshot |

---

## Tech Stack

| Komponente | Technologie | Version |
|---|---|---|
| Sprache | Kotlin | 2.2.0 |
| UI | Jetpack Compose + Material 3 | BOM 2025.05.01 |
| Audio | AndroidX Media3 / ExoPlayer | 1.9.0 |
| DI | Hilt | 2.58 |
| Navigation | Navigation Compose (Type-Safe) | 2.8.9 |
| Datenbank | Room + KSP | 2.7.1 |
| Netzwerk | Ktor Client | 3.1.3 |
| Voice Chat | LiveKit Android SDK | 2.23.5 |
| Bilder | Coil 3 | 3.1.0 |
| Animationen | Lottie Compose | 6.6.6 |
| Dynamic Theme | MaterialKolor | 2.0.0 |
| Persistenz | DataStore Preferences | 1.1.4 |
| Server | Ktor Server (WebSocket) | 3.1.3 |
| YouTube | yt-dlp | latest |
| Min SDK | Android 8.0 (API 26) | — |
| Target SDK | Android 15 (API 35) | — |

---

## Server

Der Sync-Server ist ein Ktor-WebSocket-Server, der in Docker läuft. Der öffentliche Server läuft bereits — einfach die App öffnen und loslegen.

### Selbst hosten

```bash
./gradlew :server:shadowJar
# JAR liegt in server/build/libs/server-1.0.0.jar

cd server
docker compose up -d
```

### YouTube Cookie Support

Für geo-gesperrte oder altersbeschränkte Videos eine `cookies.txt` im Netscape-Format hinterlegen:

```yaml
# docker-compose.yml
environment:
  YTDLP_COOKIES_PATH: /app/cookies.txt
volumes:
  - ./cookies.txt:/app/cookies.txt
```

### API-Übersicht

| Methode | Pfad | Beschreibung |
|---|---|---|
| `GET` | `/health` | Server-Status |
| `GET` | `/time` | Server-Zeit (NTP-Sync) |
| `POST` | `/session` | Session erstellen |
| `GET` | `/session/{code}` | Session-Info |
| `DELETE` | `/session/{code}` | Session löschen (nur Host) |
| `PATCH` | `/session/{code}` | Session umbenennen |
| `GET` | `/sessions/public` | Öffentliche Sessions |
| `POST` | `/upload/{code}` | Audio-/Cover-Datei hochladen |
| `GET` | `/uploads/{code}/{file}` | Hochgeladene Datei streamen |
| `POST` | `/youtube/add` | YouTube-Track zur Session hinzufügen |
| `WS` | `/ws/session/{code}` | Haupt-Sync WebSocket |
| `WS` | `/ws/ntp` | NTP Clock-Sync WebSocket |

---

## Lizenz

MIT License
