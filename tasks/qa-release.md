# SyncJam — QA & Release Agent Prompt
> Version: für Claude Code / Claude Opus  
> Ziel: Vollständiger End-to-End Test aller App-Funktionen + Release v2.9.0

---

## 🎯 DEINE AUFGABE

Du bist ein spezialisierter QA- und Release-Agent für die Android-App **SyncJam** (Kotlin / Jetpack Compose).  
Deine Mission: Die App **komplett und gnadenlos testen**, jeden Bug finden und fixen, und danach einen **sauberen Release v2.9.0** bauen und auf GitHub veröffentlichen — sodass das In-App-Update für bestehende Nutzer funktioniert.

**Am Ende muss gelten:** Die App ist release-ready. Kein bekannter Bug bleibt offen. Kein Crash. Kein Edge Case ungetestet.

---

## 🛠️ DEINE RESSOURCEN

### Emulator
- Du hast Zugriff auf einen Android-Emulator (AVD).
- **Starte ihn zuerst**, bevor du irgendetwas testest:
  ```bash
  # AVD auflisten
  $ANDROID_HOME/tools/emulator -list-avds
  # Starten (ersetze <AVD_NAME> mit dem tatsächlichen Namen)
  $ANDROID_HOME/tools/emulator -avd <AVD_NAME> -no-snapshot-load &
  # Warten bis Emulator bereit
  adb wait-for-device
  adb shell getprop sys.boot_completed
  # Solange wiederholen bis "1" zurückkommt
  ```
- Installiere die Debug-APK nach dem Starten:
  ```bash
  ./gradlew :app:assembleDebug
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

### SSH-Serverzugriff
- Du hast SSH-Daten für den Sync-Server (in deiner Umgebung hinterlegt / vom User bereitgestellt).
- Nutze SSH für:
  - **Live-Logs** während Tests: `journalctl -fu syncjam-server` oder `docker logs -f syncjam-server`
  - **Debugging** bei WebSocket-/Session-Problemen
  - **Server-Neustart** wenn nötig: `docker compose restart`
  - **Health-Check**: `curl http://<SERVER>:8080/health`

### GitHub CLI
- `gh` ist verfügbar für Release-Erstellung.
- Authentifizierung ist bereits gesetzt.

---

## 📋 PHASE 1: VORBEREITUNG

### 1.1 Codebase einlesen
Lies diese Dateien **komplett** bevor du anfängst:
- `CLAUDE.md` — Architektur, Coding-Regeln, Protokoll
- `AGENTS.md` — Agenten-Direktiven
- `tasks/todo.md` — Offene Aufgaben
- `tasks/lessons.md` — Bekannte Probleme / Learnings
- `modernise-todo.md` — Modernisierungsaufgaben
- `app/build.gradle.kts` — Aktuelle versionCode und versionName
- `server/` — Server-Implementierung

### 1.2 Aktuellen Stand ermitteln
```bash
# Aktuelle Version lesen
grep -E "versionCode|versionName" app/build.gradle.kts

# Letzten GitHub-Release prüfen
gh release list --limit 5

# Build-Status prüfen — App muss sauber kompilieren
./gradlew :app:assembleDebug 2>&1 | tail -20
./gradlew :server:shadowJar 2>&1 | tail -20
```

### 1.3 Server prüfen
```bash
# Ist der öffentliche Server erreichbar?
curl -s https://<SERVER_URL>/health
curl -s https://<SERVER_URL>/time

# SSH: Logs der letzten 100 Zeilen
ssh <SERVER_USER>@<SERVER_HOST> "docker logs syncjam-server --tail 100"
```

---

## 📋 PHASE 2: VOLLSTÄNDIGER FUNKTIONSTEST

Teste **jeden** der folgenden Bereiche. Für jeden Test:
1. Führe ihn auf dem Emulator durch (mit `adb shell` oder direkt per UI)
2. Beobachte Server-Logs via SSH
3. Notiere: ✅ PASS / ❌ FAIL / ⚠️ DEGRADED
4. Bei FAIL: **Sofort debuggen und fixen**, dann erneut testen

---

### 🔵 TEST-BLOCK A: App-Start & Onboarding

**A1 — Kaltstart**
- App startet ohne Crash
- Splash Screen / Lottie-Animation läuft durch
- Home Screen erscheint korrekt

**A2 — Berechtigungsanfragen**
- `READ_MEDIA_AUDIO` wird beim ersten Start angefragt
- Ablehnung → App zeigt passende Erklärung, kein Crash
- Zulassen → Bibliothek-Scan startet

**A3 — Hintergrund → Vordergrund**
- App in Hintergrund schicken, zurückkehren
- Kein State-Verlust, kein Crash

**A4 — Screen Rotation**
- Home, Library, Session, Player — alle rotieren fehlerfrei
- State bleibt erhalten (ViewModel überlebt)

---

### 🔵 TEST-BLOCK B: Bibliothek (Library)

**B1 — MediaStore-Scan**
- Lokale MP3/FLAC/OGG-Dateien werden erkannt
- Shimmer-Ladeanimation erscheint während Scan
- Track-Liste erscheint vollständig nach Scan

**B2 — Grid / Listen-Ansicht**
- Umschaltung Grid ↔ Liste funktioniert mit AnimatedContent
- Keine Layout-Glitches

**B3 — Sortierung**
- Sortierung nach Titel, Interpret, Album, Dauer, Hinzugefügt testen
- Reihenfolge ist korrekt für alle Sortieroptionen

**B4 — Suche**
- Suchfeld öffnen
- Tippen → Ergebnisse filtern in Echtzeit
- Leere Suche → alle Tracks anzeigen
- Keine passenden Ergebnisse → leerer State korrekt angezeigt

**B5 — Playlists erstellen & bearbeiten**
- Neue Playlist erstellen (Name eingeben)
- Tracks zur Playlist hinzufügen
- Playlist umbenennen
- Tracks aus Playlist entfernen
- Playlist löschen

**B6 — Favoriten**
- Track zu Favoriten hinzufügen (Heart-Icon)
- Favoriten-Filter zeigt nur markierte Tracks
- Favorit entfernen

**B7 — Carousel (zuletzt gespielt / meistgespielt)**
- Erscheint nach erstem Track-Play
- Zeigt korrekte Tracks
- Tap → Track wird zur Queue hinzugefügt oder abgespielt

---

### 🔵 TEST-BLOCK C: Session-Management

**C1 — Session erstellen (ohne Passwort)**
- Session erstellen → Code erscheint
- Session ist auf Server aktiv: `curl .../session/{code}`
- QR-Code wird korrekt angezeigt

**C2 — Session erstellen (mit Passwort)**
- Passwortfeld eingeben
- Session erscheint im Server mit Passwort-Flag

**C3 — Session erstellen (mit Auto-Lösch-Timer)**
- Timer setzen (z.B. 30 Minuten)
- Server-seitig prüfen: Session wird nach Ablauf gelöscht

**C4 — Session beitreten via Code**
- Auf zweitem Emulator / Gerät: Code eingeben → Session beitreten
- Participant erscheint in der Teilnehmerliste beider Geräte

**C5 — Session beitreten via QR-Code**
- QR-Code scannen → Deep-Link öffnet App → Session wird beigetreten

**C6 — Clipboard-Erkennung**
- Session-Code in Clipboard kopieren → App fragt automatisch nach: "Beitreten?"
- Zustimmen → Join-Flow startet

**C7 — Session teilen**
- QR-Code als PNG über Android Share-Sheet teilen
- Bild ist korrekt und lesbar

**C8 — Host-Krone**
- Host hat visuell erkennbare Krone-Icon
- Andere Teilnehmer haben keine Krone

**C9 — Session-Timer**
- Timer läuft sichtbar und korrekt hoch

**C10 — Session verlassen (Host)**
- Host verlässt → Admin-Übertragung an nächsten Teilnehmer
- Session bleibt aktiv wenn weitere Teilnehmer vorhanden
- Kein Crash für verbleibende Teilnehmer

**C11 — Session verlassen (Gast)**
- Gast verlässt → Host sieht Teilnehmer-Update
- Session läuft weiter

**C12 — Öffentliche Sessions**
- Öffentliche Session erscheint in Liste: `GET /sessions/public`
- Beitreten via Liste funktioniert

**C13 — Session mit falscher Code**
- Ungültigen Code eingeben → Fehlermeldung, kein Crash

**C14 — Session mit falschem Passwort**
- Richtiger Code, falsches Passwort → Fehlermeldung, kein Crash

---

### 🔵 TEST-BLOCK D: Synchronisation (KERNFUNKTION)

**D1 — NTP Clock-Sync beim Join**
- Beim Session-Beitritt werden 5 NTP-Samples gemessen
- Offset wird korrekt berechnet
- Debug-Overlay aktivieren: Latenz-Anzeige erscheint

**D2 — NTP Auto-Refresh**
- Alle 30 Sekunden wird NTP-Offset aktualisiert
- Keine Unterbrechung der Wiedergabe

**D3 — Play-Kommando Sync**
- Host drückt Play → Gast startet innerhalb < 500ms
- Server-Log zeigt Play-Broadcast mit Timestamp

**D4 — Pause-Kommando Sync**
- Host drückt Pause → Gast pausiert synchron

**D5 — Seek-Kommando Sync**
- Host seeked zu Position X → Gast springt zur gleichen Position

**D6 — Drift-Korrektur: Playback-Rate (150–500ms)**
- Künstlich 200ms Drift simulieren (via Debug-Overlay oder Code)
- Rate-Korrektur (1.02× / 0.98×) greift, kein hörbarer Glitch

**D7 — Drift-Korrektur: Seek (500–2000ms)**
- 800ms Drift simulieren → Seek-Korrektur greift

**D8 — Drift-Korrektur: Full Resync (> 2000ms)**
- > 2s Drift → StateSnapshot wird gesendet → Vollsync

**D9 — Track-Ende Sync**
- Host-Gerät sendet `TrackEnded` → Server broadcastet nächsten Track
- Beide Geräte starten neuen Track synchron

**D10 — Heartbeat**
- Alle 15s wird Heartbeat gesendet
- Server-Log zeigt Heartbeats von allen Clients

---

### 🔵 TEST-BLOCK E: Player

**E1 — Mini-Player-Bar**
- Erscheint sobald Track spielt
- Fortschrittsanzeige korrekt
- Play/Pause-Button funktioniert
- Tap → Vollbild-Player öffnet

**E2 — Vollbild-Player öffnen/schließen**
- Shared Element Transition Mini → Full funktioniert
- Swipe-Down schließt den Player
- Keine Jank / Frame-Drops

**E3 — Vinyl-Scheibe**
- Rotiert während Wiedergabe
- Stoppt bei Pause

**E4 — Marquee-Titel**
- Langer Track-Titel scrollt automatisch

**E5 — Dynamic Album Art Theming**
- Album Art wird geladen
- Dominante Farbe wird als Theme-Seed verwendet
- UI-Farben ändern sich entsprechend

**E6 — Waveform-Visualizer**
- Waveform wird während Wiedergabe animiert
- Amplitude-Anzeige reagiert auf Lautstärke

**E7 — Glassmorphism-Overlays**
- Auf API 31+ Emulator: echter Blur-Effekt sichtbar
- Auf API < 31: semi-transparenter Fallback

**E8 — Haptisches Feedback**
- Steuer-Buttons (Play/Pause/Skip) geben haptisches Feedback

**E9 — Audio-Formate**
- MP3 abspielbar ✓
- FLAC abspielbar ✓
- OGG abspielbar ✓
- WAV abspielbar ✓
- AAC/M4A abspielbar ✓
- OPUS abspielbar ✓

**E10 — Background-Playback**
- App in Hintergrund → Musik läuft weiter
- Notification mit Steuerung erscheint (Media3 MediaSession)
- Steuern über Notification funktioniert

**E11 — Audio Focus**
- Anderer Player startet → SyncJam pausiert (Audio Focus Lost)
- Anderer Player stoppt → SyncJam resumt (Audio Focus Gain)

---

### 🔵 TEST-BLOCK F: Queue & Voting

**F1 — Track zur Queue hinzufügen (aus Bibliothek)**
- Track long-press → "Zur Queue hinzufügen"
- Track erscheint in Queue aller Teilnehmer

**F2 — Track zur Queue hinzufügen (aus Playlist)**
- Playlist öffnen → Track zu Queue hinzufügen

**F3 — Queue-Anzeige**
- Queue zeigt alle Tracks in korrekter Reihenfolge
- Score-Anzeige pro Track

**F4 — Voting (upvote)**
- Gast gibt +Vote → Score erhöht sich
- Queue sortiert sich neu (Score-basiert)
- Supabase Trigger prüfen: Score korrekt?

**F5 — Voting (downvote)**
- -Vote → Score sinkt
- Queue-Sortierung aktualisiert

**F6 — 1 Vote pro User pro Track**
- Zweites Vote auf selben Track ändert nichts (oder togglet)

**F7 — Track-Bestätigung durch Host**
- Nicht-Host fügt Track hinzu → Host bekommt Confirmation-Dialog
- Host bestätigt → Track erscheint in Queue
- Host lehnt ab → Track wird entfernt

**F8 — Track entfernen (Host)**
- Host entfernt Track aus Queue
- Track verschwindet bei allen Teilnehmern

**F9 — Queue Drag-Reorder (Host)**
- Host kann Track-Reihenfolge per Drag ändern
- Neue Reihenfolge wird an alle gesendet

**F10 — Gespielte Tracks**
- Gespielte Tracks bleiben in Queue (unveränderlich, kein Re-Ordering)

---

### 🔵 TEST-BLOCK G: YouTube-Integration

**G1 — YouTube-Link einfügen**
- Gültigen YouTube-Link einfügen
- Server startet yt-dlp Download
- Download-Banner zeigt korrekten Titel (NICHT die rohe Video-ID!)
- Track erscheint in Queue nach Download

**G2 — YouTube-Stream**
- Track wird von allen Clients gestreamt: `GET /youtube/stream/{id}`
- Playback startet ohne langen Buffer

**G3 — Ungültiger YouTube-Link**
- Ungültigen Link eingeben → Fehlermeldung, kein Crash

**G4 — Geo-gesperrter Content (wenn cookies.txt vorhanden)**
- Prüfen ob Cookie-Pfad korrekt konfiguriert

**G5 — Server-Log bei YouTube-Download**
- SSH: yt-dlp Output im Log prüfen
- Keine Fehler bei normalem Video

---

### 🔵 TEST-BLOCK H: Track Upload

**H1 — Track hochladen (Upload)**
- Lokalen Track über `POST /upload/{code}` hochladen
- Server empfängt und speichert Track
- Track erscheint in Queue

**H2 — Track Transfer zwischen Nutzern**
- User A hat Track, User B nicht
- Server sendet `TrackTransferOffer`
- User B sendet `TrackTransferRequest`
- Transfer startet (WebSocket Binary Frames)
- User B spielt Track lokal ab

**H3 — Großer Track (> 50MB)**
- Chunked Transfer (4KB Chunks) funktioniert
- Kein Timeout, kein Memory Overflow

---

### 🔵 TEST-BLOCK I: Voice Chat (LiveKit)

**I1 — Voice Chat beitreten**
- Mikrofon-Button aktivieren → Voice-Raum wird beigetreten
- Token wird von Server generiert

**I2 — Audio-Übertragung**
- Sprechen → Gesprochenes kommt auf Gegenseite an

**I3 — Mute/Unmute**
- Mikro stummschalten → kein Audio mehr übertragen
- Unmute → Audio wieder aktiv

**I4 — Push-to-Talk**
- PTT in Einstellungen aktivieren
- Taste gedrückt halten → Sprechen möglich
- Taste loslassen → Stummschalten

**I5 — Music Ducking**
- Während Voice aktiv: Musik-Lautstärke auf 25%
- Voice deaktivieren → Musik zurück auf 100%

**I6 — Speaking-Indicator**
- Pulsierender Avatar-Ring während Sprechen

**I7 — Netzwerkqualitäts-Anzeige**
- Excellent / Good / Poor / Lost korrekt angezeigt

**I8 — Server-Mute (Host)**
- Host muted Teilnehmer-Mikro remote
- Teilnehmer kann eigenes Mikro nicht reaktivieren solange Mute aktiv

---

### 🔵 TEST-BLOCK J: Chat & Soziale Features

**J1 — Chat öffnen**
- Chat-Sheet öffnet sich korrekt (Bottom Sheet)

**J2 — Nachricht senden**
- Text eingeben und senden
- Nachricht erscheint bei allen Teilnehmern

**J3 — Nachricht mit Sonderzeichen / Emojis**
- Unicode-Zeichen korrekt dargestellt

**J4 — Zeichenlimit (500 Zeichen)**
- Mehr als 500 Zeichen → Eingabe blockiert oder Fehlermeldung

**J5 — Chat nach Session-Ende ephemer**
- Session beenden → Nachrichten sind weg (kein persistenter Speicher)

**J6 — Emoji-Reaktion senden**
- Emoji-Button drücken → floating Emoji-Animation startet
- Animation: Y-Translation + Alpha-Fade, korrekt

**J7 — Double-Tap Burst (5 Emojis)**
- Doppelt tippen → 5 Emojis gleichzeitig

**J8 — Max 20 gleichzeitige Reaktionen**
- 25 Reaktionen schnell hintereinander → max. 20 gleichzeitig sichtbar

**J9 — Direktnachricht (Host → Teilnehmer)**
- Host sendet private Nachricht
- Nur Empfänger sieht sie

---

### 🔵 TEST-BLOCK K: Admin-Tools (Host)

**K1 — Kick**
- Host kickt Teilnehmer → Teilnehmer wird aus Session entfernt
- Teilnehmer sieht "Du wurdest aus der Session entfernt"-Meldung

**K2 — Ban**
- Host bannt Teilnehmer → Teilnehmer kann Session nicht wieder beitreten

**K3 — Admin-Übertragung**
- Host überträgt Admin-Rechte an Teilnehmer
- Neuer Host bekommt Krone und Admin-Funktionen

---

### 🔵 TEST-BLOCK L: Einstellungen

**L1 — Übertragungsqualität ändern**
- Niedrig / Mittel / Hoch auswählen
- Einstellung wird gespeichert (DataStore)
- Einstellung bleibt nach App-Neustart

**L2 — Benachrichtigungstöne**
- An/Aus togglen
- Gespeichert nach Neustart

**L3 — Vibration**
- An/Aus togglen

**L4 — Farbschema**
- System / Hell / Dunkel auswählen
- UI wechselt sofort

**L5 — Cache leeren**
- Button tippen → Cache wird geleert
- App läuft weiter ohne Crash

---

### 🔵 TEST-BLOCK M: Reconnect & Netzwerk-Edge-Cases

**M1 — WLAN trennen und wieder verbinden**
- Während Session aktiv: WLAN trennen (10s)
- Reconnect mit Exponential Backoff: 2s → 4s → 8s → max 30s
- Nach Reconnect: StateSnapshot wird empfangen, Sync wiederhergestellt

**M2 — Server kurz nicht erreichbar**
- Server per SSH kurz neustarten
- App reconnectet automatisch
- Session-State nach Reconnect korrekt

**M3 — Schlechtes Netzwerk (Jitter)**
- Network-Emulation auf Emulator aktivieren (Poor Network)
- App funktioniert stabil, Sync hält

**M4 — Nur Host verliert Verbindung**
- Host reconnectet → Admin-Übertragung rückgängig oder korrekt gehandhabt?

---

### 🔵 TEST-BLOCK N: Tablet-Layout

**N1 — WindowSizeClass**
- Auf Tablet-Emulator (7"/10" AVD): Tablet-Layout erscheint
- Kein falsch-großes Phone-Layout

---

### 🔵 TEST-BLOCK O: In-App Update

**O1 — Update-Check**
- App prüft auf neue Version: `GET /releases/latest` oder GitHub Releases API
- Wenn neuere Version vorhanden → Update-Banner erscheint

**O2 — APK-Download und Install**
- Update-Banner → Download der neuen APK
- Android-Installer öffnet sich
- Installation erfolgreich

---

## 📋 PHASE 3: BUG-FIXING

Nach dem Testdurchlauf:

1. **Alle FAIL-Items auflisten** mit Priorität (Critical / High / Medium / Low)
2. **Critical und High zuerst fixen** — kein Release mit Criticals
3. **Nach jedem Fix**: erneut testen, Emulator-Build neu installieren
4. **Lessons updaten**: `tasks/lessons.md` mit jedem gefixten Bug
5. **Build-Verifikation** nach allen Fixes:
   ```bash
   ./gradlew :app:assembleRelease
   # Muss fehlerfrei kompilieren
   ```

---

## 📋 PHASE 4: VERSION BUMP

**Aktuelle Version:** v2.8.0 → **Neue Version: v2.9.0**

### 4.1 Gradle Version Bump
Datei: `app/build.gradle.kts`

```kotlin
// VORHER (Beispiel):
versionCode = 28
versionName = "2.8.0"

// NACHHER:
versionCode = 29
versionName = "2.9.0"
```

**Wichtig:**
- `versionCode` muss ganzzahlig um mindestens 1 erhöht werden (Android-Requirement)
- `versionName` auf `"2.9.0"` setzen
- Beide Werte ändern — nicht nur einen!

### 4.2 Changelog aktualisieren
Datei: `README.md` — Changelog-Sektion:

```markdown
### v2.9.0

* QA: Vollständiger Test aller Features — Release Ready
* Fix: [alle gefixten Bugs hier auflisten]
* Improvement: [alle Verbesserungen hier]
```

### 4.3 Commit pushen
```bash
git add app/build.gradle.kts README.md tasks/lessons.md
git commit -m "chore: bump version to v2.9.0 — QA Release"
git push origin master
```

---

## 📋 PHASE 5: RELEASE-APK BAUEN

### 5.1 Release Build
```bash
# Signed Release APK bauen
./gradlew :app:assembleRelease

# APK-Pfad:
# app/build/outputs/apk/release/app-release.apk

# Größe und Integrität prüfen
ls -lh app/build/outputs/apk/release/app-release.apk
# Muss > 1MB sein, kein leere Datei

# APK-Signatur verifizieren
$ANDROID_HOME/build-tools/<VERSION>/apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

### 5.2 Keystore-Konfiguration prüfen
Das Keystore `syncjam-release.jks` liegt im Root. Stelle sicher dass in `app/build.gradle.kts` korrekt konfiguriert:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../syncjam-release.jks")
        // storePassword, keyAlias, keyPassword aus env oder local.properties
    }
}
```

Falls Signing-Variablen fehlen → aus `local.properties` oder Umgebungsvariablen lesen.

---

## 📋 PHASE 6: GITHUB RELEASE ERSTELLEN

### 6.1 Git Tag erstellen
```bash
git tag v2.9.0
git push origin v2.9.0
```

### 6.2 GitHub Release mit APK
```bash
# Release erstellen mit APK-Anhang
gh release create v2.9.0 \
  app/build/outputs/apk/release/app-release.apk \
  --title "SyncJam v2.9.0 — QA Release Ready" \
  --notes "## SyncJam v2.9.0

### 🎯 Diese Version wurde vollständig QA-getestet

**Alle Features getestet und release-ready:**
- ✅ NTP-Synchronisation & Drift-Korrektur
- ✅ Session-Management (Erstellen, Beitreten, QR-Code, Clipboard)
- ✅ Vollbild-Player mit Dynamic Theming & Waveform
- ✅ YouTube-Integration (yt-dlp)
- ✅ Track Upload & P2P Transfer
- ✅ Voice Chat (LiveKit)
- ✅ Kollaborative Queue mit Voting
- ✅ Chat & Emoji-Reaktionen
- ✅ Admin-Tools (Kick, Ban, Server-Mute)
- ✅ Reconnect-Logik & Netzwerk-Edge-Cases
- ✅ In-App Update-Flow

### Installation
1. \`app-release.apk\` herunterladen
2. 'Unbekannte Quellen' aktivieren
3. APK installieren

### Fixes in dieser Version
$(cat /tmp/release_fixes.txt 2>/dev/null || echo '- Diverse Bugfixes und Stabilitätsverbesserungen')
" \
  --latest
```

### 6.3 Release verifizieren
```bash
# Prüfen ob Release korrekt erscheint
gh release view v2.9.0

# APK-Download testen
gh release download v2.9.0 --pattern "*.apk" --dir /tmp/verify
ls -lh /tmp/verify/
```

### 6.4 In-App Update verifizieren
Das In-App Update funktioniert über die GitHub Releases API. Prüfe:
- Release ist als "Latest" markiert: `gh release view v2.9.0 --json isLatest`
- APK-Dateiname ist `app-release.apk`
- `versionCode` in der APK ist höher als in der vorherigen Version

---

## 📋 PHASE 7: ABSCHLUSS-PROTOKOLL

Erstelle am Ende eine Datei `tasks/qa-report-v2.9.0.md`:

```markdown
# QA Report — SyncJam v2.9.0
Datum: [Datum]
Tester: Claude Code Agent

## Testergebnis
- Gesamt Tests: [N]
- PASS: [N]
- FAIL (behoben): [N]  
- SKIPPED: [N]

## Behobene Bugs
| Bug | Komponente | Schwere | Fix |
|-----|------------|---------|-----|
| ... | ... | Critical/High/Medium | ... |

## Offene Issues (Low Priority — Kein Release-Blocker)
- ...

## Release-Status
✅ Version: v2.9.0
✅ versionCode: [N]
✅ APK signed: Ja
✅ GitHub Release: erstellt
✅ In-App Update: funktionsfähig
```

---

## ⚠️ WICHTIGE REGELN

1. **Niemals** einen Release erstellen wenn noch ein Critical- oder High-Bug offen ist
2. **Immer** nach einem Fix neu bauen und testen — kein "das wird schon passen"
3. **Server-Logs beobachten** während allen Netzwerk-Tests — viele Bugs sind nur server-seitig sichtbar
4. **Emulator-Logs mitlaufen lassen:** `adb logcat -s SyncJam:V` in separatem Terminal
5. **Kein Quick-Fix** — Root Cause finden und sauber lösen (aus `CLAUDE.md`)
6. **Nach jedem Fix:** `tasks/lessons.md` aktualisieren

---

## 🚀 STARTREIHENFOLGE

```
1. Emulator starten
2. CLAUDE.md + AGENTS.md lesen
3. Server-Health prüfen (SSH)
4. Debug-Build installieren
5. Tests A → N durchführen (Logcat + SSH-Logs parallel)
6. Alle Bugs fixen
7. Release-Build erstellen
8. Version bump (Gradle + README)
9. Commit + Push
10. GitHub Release mit APK erstellen
11. QA-Report schreiben
```

**Du bist fertig wenn:** GitHub Release v2.9.0 existiert, APK ist angehängt, In-App Update funktioniert, QA-Report ist geschrieben. 🎉
