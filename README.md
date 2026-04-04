# SyncJam 🎵

**Gemeinsam Musik hören — synchronisiert über das Internet.**

SyncJam ist eine Android-App, mit der zwei oder mehr Personen gleichzeitig Musik aus ihren lokalen Bibliotheken hören können — perfekt synchronisiert, mit Voice-Chat, Emoji-Reaktionen, Text-Reaktionen, Track-Voting und einem modernen Player-UI.

[![Release](https://img.shields.io/github/v/release/Pcf1337-hash/SyncJam?label=latest)](https://github.com/Pcf1337-hash/SyncJam/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green)](https://github.com/Pcf1337-hash/SyncJam/releases/latest)

---

## Features

- **Echtzeit-Sync** — NTP-basierte Uhrensynchronisation, max. 150 ms Drift
- **Lokale Bibliothek** — MP3, FLAC, WAV, OGG, AAC, M4A, OPUS
- **YouTube-Support** — Tracks direkt per URL zur Session hinzufügen (yt-dlp, Cookie-Support)
- **Auto-Play** — Erster Track startet automatisch sobald er ready ist
- **Kollaborative Warteschlange** — Tracks vorschlagen & abstimmen
- **Vinyl-Player UI** — Animierter Plattenspieler mit positionsbasiertem Waveform-Visualizer
- **Voice Chat** — Mikrofon-Toggle, Lautstärkeregler
- **Emoji-Reaktionen** — Floatende Emojis in Echtzeit
- **Text-Reaktionen** — Eigenen Text als flotierenden Bubble senden (Dauer proportional zur Textlänge)
- **Session-Codes** — Einfaches Beitreten per 6-stelligem Code
- **Dark Theme** — Album-Art-basiertes Dynamic Theming
- **Auto-Updater** — Prüft beim Start auf neue GitHub-Releases und zeigt scrollbaren Changelog

---

## Screenshots

> Coming soon

---

## Installation

### APK direkt installieren (Android 8.0+)

1. Lade `SyncJam-v1.1.0.apk` aus dem [Release](../../releases/latest) herunter
2. Aktiviere "Unbekannte Quellen" auf deinem Gerät
3. Installiere die APK
4. Server läuft bereits — einfach starten!

### Build from source

```bash
git clone https://github.com/Pcf1337-hash/SyncJam.git
cd SyncJam
./gradlew assembleDebug
```

---

## Tech Stack

| Komponente | Technologie |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Audio | AndroidX Media3 / ExoPlayer |
| Netzwerk | Ktor Client + WebSocket |
| DI | Hilt |
| Datenbank | Room |
| Auth | Supabase |
| Sync-Server | Ktor Server (Kotlin) |

---

## Server

Der Sync-Server ist ein Ktor-WebSocket-Server (Kotlin), der in Docker läuft.

```bash
cd server
docker build -t syncjam-server .
docker run -p 8080:8080 syncjam-server
```

Der öffentliche Server läuft auf `159.195.63.246:8080` — einfach die App öffnen und loslegen.

### YouTube Cookie Support

Für altersbeschränkte oder regiongesperrte Videos kann eine `cookies.txt` (Netscape-Format) hinterlegt werden:

```bash
# Im Server-Verzeichnis:
export YTDLP_COOKIES_PATH=/app/cookies.txt
# oder in docker-compose.yml: YTDLP_COOKIES_PATH=/app/cookies.txt
```

---

## Wie es funktioniert

**Command-Relay-Synchronisation:** Jedes Gerät spielt Audio lokal ab. Der Server koordiniert WAS und WANN gespielt wird — kein Audio-Restreaming, kein DRM-Problem.

```
Client A ──play(trackId, positionMs)──► Server
                                          │
Server ──play(trackId, positionMs, serverTs)──► Client B
                                          │
                                     Client C
```

NTP-Style Clock-Sync beim Session-Join kompensiert unterschiedliche Systemuhren.

---

## Lizenz

MIT License — feel free to use, modify and distribute.
