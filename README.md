# SyncJam 🎵

**Gemeinsam Musik hören — synchronisiert über das Internet.**

SyncJam ist eine Android-App, mit der zwei oder mehr Personen gleichzeitig Musik aus ihren lokalen Bibliotheken hören können — perfekt synchronisiert, mit Voice-Chat, Emoji-Reaktionen, GIF-Sharing, Track-Voting und einem modernen Player-UI.

---

## Features

- **Echtzeit-Sync** — NTP-basierte Uhrensynchronisation, max. 150 ms Drift
- **Lokale Bibliothek** — MP3, FLAC, WAV, OGG, AAC, M4A, OPUS
- **YouTube-Support** — Tracks direkt per URL zur Session hinzufügen
- **Kollaborative Warteschlange** — Tracks vorschlagen & abstimmen
- **Vinyl-Player UI** — Animierter Plattenspieler mit Waveform-Visualizer
- **Voice Chat** — Mikrofon-Toggle (Push-to-Talk), Lautstärkeregler
- **Emoji-Reaktionen** — Floatende Reaktionen in Echtzeit
- **Session-Codes** — Einfaches Beitreten per 6-stelligem Code
- **Dark Theme** — Album-Art-basiertes Dynamic Theming

---

## Screenshots

> Coming soon

---

## Installation

### APK direkt installieren (Android 8.0+)

1. Lade `SyncJam-v1.0.apk` aus dem [Release](../../releases/latest) herunter
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

Der öffentliche Server läuft auf `84.252.121.74` — einfach die App öffnen und loslegen.

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
