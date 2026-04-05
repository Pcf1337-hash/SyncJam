# SyncJam Modernisierung — TODO
> Quelle: `deep-research-report.md` | Stand: 2026-04-05
> Bereits erledigte Phasen (v2.0–v2.3) sind hier NICHT enthalten.
> Kein Spotify/Apple, kein Video, keine Lyrics.

---

## 1 — Home Screen / Session-Lobby

- [ ] **QR-Code Scanner** für Session-Beitritt (ZXing oder ML Kit)
- [ ] **Session-Verlaufsliste** mit Album-Art Thumbnails der zuletzt gehörten Musik (LazyColumn + animateItemPlacement)
- [ ] **Oeffentlicher Session-Feed** — scrollbare Liste aktiver Sessions mit Titel, Host, Teilnehmerzahl, Cover (Karten mit Shared-Element-Uebergang)
- [ ] **Warteraum-Animationen** — Lottie-Animationen fuer Idle-State (z.B. animierter Plattenspieler), solange keine Mitspieler da sind

---

## 2 — Bibliothek

- [ ] **Paging fuer grosse Sammlungen** — Jetpack Paging Compose (`LazyPagingItems`) statt alles auf einmal laden
- [ ] **"Zuletzt gespielt" / "Am haeufigsten"** — horizontales Cover-Karussell als dynamische Sektion oben in der Bibliothek
- [ ] **BlurHash-Placeholder** fuer Albumcover (`compose-image-blurhash`) statt einfachem Grau
- [ ] **Leere Zustaende & Permissions** — freundliche Erklaertexte + Buttons wenn keine Audiodateien oder Rechte fehlen

---

## 3 — Vollbild-Player

- [ ] **Parallax-Effekt** fuer Cover beim Scrollen (`Modifier.graphicsLayer` + scrollable)
- [ ] **Shared Element Transition** Mini-Player <-> Fullscreen (Compose Navigation Shared Elements)
- [ ] **Waveform-Visualizer** im Progress-Slider (compose-audiowaveform Library)

---

## 4 — Mini-Player-Leiste

- [ ] **Pulsierender Spiel-Indikator** — animiertes Lautsprecher-Icon oder 3-Bars-Animation wenn Audio laeuft
- [ ] **Radial-Progress** um das Play-Icon als alternative Fortschrittsanzeige

---

## 5 — Voice-Chat Overlay

- [ ] **Netzwerkqualitaets-Anzeige** im Voice UI — gruen/gelb/orange Balken neben Avatar (Latenz/Verbindungsqualitaet)
- [ ] **Auto-Ducking via AudioFocus** — `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` anfordern, damit System ExoPlayer automatisch duckt wenn Voice aktiv

---

## 6 — Emoji-Reaktionen

- [ ] **Reaktions-Burst** — bei Doppel-Tap oder langem Druecken Mini-Explosion aus mehreren zufaelligen Emojis
- [ ] **"Wer hat reagiert"** — bei gehaltenem Emoji Name des Senders als Tooltip/Overlay anzeigen

---

## 7 — GIF-Sharing

- [ ] **Vorschau vor dem Senden** — nach Auswahl Modal mit Senden/Abbrechen Buttons, erst bei Bestaetigung versenden
- [ ] **Rate-Limit Countdown UI** — visueller Kreis-Countdown Timer auf dem Sende-Button nach GIF-Versand

---

## 8 — Chat

- [ ] **Ungelesene-Badge** — wenn Chat-Sheet geschlossen und neue Nachrichten eingehen, Badge-Zaehler auf Handle/Tab anzeigen. Bei Oeffnen zuruecksetzen
- [ ] **"Tippt..."-Anzeige** — Typing-Event ueber WebSocket senden/empfangen, "Username tippt..." unterhalb des Chats anzeigen
- [ ] **Zeitstempel** klein unten rechts in Chat-Bubbles

---

## 9 — Queue & Voting

- [ ] **Track-Transfer-Indikator** — Cloud/Pfeil-Icon neben Track wenn Upload/Download laeuft + CircularProgressIndicator
- [ ] **Host Drag-Reorder** — nur Host kann per langem Tap oder Drag Tracks manuell umsortieren
- [ ] **Vote-Animation** — bei Up/Down-Tap kurze +1 Animation und Daumen-Vergroesserung als visuelles Feedback

---

## 10 — Session Header / Teilnehmer

- [ ] **Online-Indikator** — gruener Punkt an Teilnehmer-Avataren
- [ ] **Session-Name editierbar** — nur Host kann durch Tap auf Titel AlertDialog mit TextField oeffnen
- [ ] **Einladung via Share-Intent** — Android Share Sheet mit Session-Code/Deep-Link oeffnen

---

## 11 — Auth & Profil

- [ ] **AuthRepositoryImpl** mit Supabase Auth (Email/Passwort)
- [ ] **ProfileSheet** — Avatar-Upload via ImagePicker + Supabase Storage, Anzeigename aendern
- [ ] **Avatar in Teilnehmerliste** — Coil AsyncImage fuer Profilbilder in Session-Header und Chat

---

## 12 — Sync Engine

- [ ] **Kronos NTP Library** integrieren — `AndroidClockFactory.createKronosClock(context).syncInBackground()` fuer robustere Zeitsynchronisation
- [ ] **State-Snapshot bei Rejoin** — nach Reconnect letzten lokalen State senden, Server antwortet mit aktuellem StateSnapshot
- [ ] **Host-Wechsel** — wenn Host disconnected, automatisch naechsten Teilnehmer zum Host promoten (Supabase Presence oder Server-seitig) bei rejoin soll host status aber zurück gegeben werden und mann soll host modus auch fest übertragen können für die session 
- [ ] **Latenz-Debug-Overlay** — optionales Dev-Overlay das Netzwerk-Latenz (Ping) in ms anzeigt

---

## 13 — Audio-Datei-Transfer

- [ ] **Custom DataSource fuer Media3** — DataSource.Factory die aus WebSocket-Buffer liest und parallel in Storage schreibt, damit Playback beginnt waehrend noch Daten kommen
- [ ] **Fortschrittsanzeige** beim Transfer — LinearProgressIndicator oder CircularProgressIndicator als Overlay auf Titelbild
- [ ] **Parallel-Upload** — mehrere Dateien gleichzeitig via Coroutines `async` in Supabase Storage hochladen

---

## 14 — Allgemeine UI/UX

- [ ] **MediaSession + Notification** — Media3 MediaSessionService mit MediaStyle Notification + Lockscreen Controls (Play/Pause/Skip)
- [ ] **Adaptive Tablet-Layout** — `WindowSizeClass` nutzen: Bibliothek links, Player rechts auf Tablets
- [ ] **Onboarding** — 3 kurze Intro-Screens fuer Erstnutzer (ViewPager-Style), DataStore `onboarding_completed` Flag
- [ ] **Shimmer/Skeleton Loading** — Shimmer-Platzhalter beim Laden von Listen (Home, Library, Chat)
- [ ] **Haptik fuer Controls** — `LocalHapticFeedback.performHapticFeedback()` bei allen wichtigen Aktionen (teilweise schon im Player)

---

## 15 — Feature-Ideen (Nice-to-Have)

- [ ] **DJ-Rotation** — Nutzer melden sich als DJ, reihum darf nur der aktive DJ den naechsten Song vorschlagen
- [ ] **Themen-Raeume** — oeffentliche Genre-gefilterte Raeume (Chill, Party, Metal) mit Cover und Hoererzahl
- [ ] **Kollaborative Playlisten** — Teilnehmer fuegen Vorschlaege direkt in gemeinsame Playlist ein
- [ ] **Community-Features** — Follower/Abos fuer Hosts/Raeume, Push-Benachrichtigung wenn Freund Session startet
- [ ] **Discord/Telegram Bot** — automatisches Einladen in Sessions ueber externe Messenger

---

## 16 — Einstellungen (Erweiterung)

- [ ] **Audio-Qualitaet** — Optionen Niedrig/Mittel/Hoch (beeinflusst Transfer-Qualitaet)
- [ ] **Cache-Verwaltung** — Speicherbelegung anzeigen (Waveforms, Cover, Downloads) + Cache-Leeren Button
- [ ] **Design-Override** — Theme-Wahl System/Tag/Nacht explizit

---

## 17 — Server & Deployment

- [ ] `/time` Endpoint auf Produktions-Server aktiv
- [ ] LiveKit env vars gesetzt (LIVEKIT_API_KEY, LIVEKIT_API_SECRET, LIVEKIT_WS_URL)
- [ ] Server nach Deployment verifizieren (`/health` + WebSocket-Verbindung)

---

## 18 — Testing & Release

- [ ] Build erfolgreich ohne Compile-Errors
- [ ] Emulator-Test aller neuen Features (Screenshots + Logcat)
- [ ] README.md komplett neu mit aktuellen Features + Screenshots
- [ ] GitHub Release erstellen (APK als Asset, In-App-Update triggert)

---

## Kritische Regeln (aus CLAUDE.md)
- **NIEMALS** `collectAsState()` -> immer `collectAsStateWithLifecycle()`
- **NIEMALS** `Color(0xFF...)` -> immer `MaterialTheme.colorScheme.*`
- **IMMER** `graphicsLayer { }` fuer animierte Properties (GPU-beschleunigt)
- **IMMER** `LazyColumn` mit `key = { it.id }` + `contentType`
- **IMMER** `supervisorScope` fuer parallele Coroutines
- **Drift < 500ms** -> Playback-Rate Anpassung, KEIN Seek
- Nach jeder Korrektur: `tasks/lessons.md` updaten
