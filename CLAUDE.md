# CLAUDE.md — SyncJam: Synchronized Music Listening App

> **Agent Directive v1.0** — Dieses Dokument ist die Single Source of Truth für Claude Code.
> Lies es KOMPLETT bevor du irgendetwas implementierst.

---

## 🎯 Projekt-Vision

**SyncJam** ist eine Android-App (Kotlin/Jetpack Compose), mit der zwei oder mehr Personen gemeinsam Musik aus ihren lokalen Bibliotheken hören können — synchronisiert über das Internet, mit Voice-Chat, Emoji-Reaktionen, GIF-Sharing, Track-Voting und einem modernen, album-art-getriebenen UI.

**Kernprinzip:** Command-Relay-Synchronisation — jedes Gerät spielt Audio lokal ab, ein zentraler Server koordiniert WAS und WANN gespielt wird. Kein Audio-Restreaming, kein DRM-Problem.

**Keine Streaming-Dienste.** Nur lokale Dateien (MP3, FLAC, WAV, OGG, AAC, M4A, OPUS).

---

## 📦 Tech Stack (VERBINDLICH)

### Android Client
| Komponente | Technologie | Version |
|---|---|---|
| **Sprache** | Kotlin | 2.1+ |
| **UI Framework** | Jetpack Compose + Material 3 | BOM 2025.05+ |
| **Build System** | Gradle KTS + Version Catalogs | 8.9+ |
| **Min SDK** | API 26 (Android 8.0) | - |
| **Target SDK** | API 35 | - |
| **Audio Playback** | AndroidX Media3 (ExoPlayer) | 1.9.0 |
| **DI** | Hilt | 2.51+ |
| **Navigation** | Navigation Compose (Type-Safe) | 2.8+ |
| **Local DB** | Room + KSP | 2.7+ |
| **Networking** | Ktor Client + kotlinx.serialization | 3.1+ |
| **WebSocket** | Ktor Client WebSocket | 3.1+ |
| **Voice Chat** | LiveKit Android SDK | 2.23+ |
| **Image Loading** | Coil 3 + Coil GIF | 3.1+ |
| **Animations** | Lottie Compose | 6.1+ |
| **GIF Picker** | Giphy SDK | 2.3+ |
| **Waveform** | compose-audiowaveform (lincollincol) | 1.1+ |
| **Dynamic Theming** | MaterialKolor (jordond) | 2.0+ |
| **Permissions** | Accompanist Permissions | 0.36+ |
| **Testing** | JUnit5 + Turbine + MockK + Compose Testing | latest |

### Backend
| Komponente | Technologie | Version |
|---|---|---|
| **Sync Server** | Ktor Server (WebSocket) | 3.1+ |
| **State/Auth/DB** | Supabase (Postgres + Realtime + Auth + Storage) | - |
| **Voice Infrastructure** | LiveKit Cloud (Free Tier) oder Self-Hosted | - |
| **File Transfer** | Supabase Storage + WebSocket Binary Frames | - |
| **Hosting** | Railway / Fly.io / Render (Ktor Server) | - |

### Gradle Version Catalog (libs.versions.toml) — IMMER verwenden
```toml
[versions]
kotlin = "2.1.0"
compose-bom = "2025.05.01"
media3 = "1.9.0"
hilt = "2.51.1"
room = "2.7.1"
ktor = "3.1.1"
livekit = "2.23.5"
coil = "3.1.0"
navigation = "2.8.9"
lottie = "6.1.0"
giphy = "2.3.15"
materialKolor = "2.0.0"
coroutines = "1.9.0"
kotlinxSerialization = "1.7.3"
kotlinxCollectionsImmutable = "0.3.8"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }
compose-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
media3-ui-compose = { group = "androidx.media3", name = "media3-ui-compose", version.ref = "media3" }
media3-datasource = { group = "androidx.media3", name = "media3-datasource", version.ref = "media3" }

ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-websockets = { group = "io.ktor", name = "ktor-client-websockets", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }

room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

livekit-android = { group = "io.livekit", name = "livekit-android", version.ref = "livekit" }

coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-gif = { group = "io.coil-kt.coil3", name = "coil-gif", version.ref = "coil" }

navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
lottie-compose = { group = "com.airbnb.android", name = "lottie-compose", version.ref = "lottie" }
materialkolor = { group = "com.materialkolor", name = "material-kolor", version.ref = "materialKolor" }

kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-collections-immutable = { group = "org.jetbrains.kotlinx", name = "kotlinx-collections-immutable", version.ref = "kotlinxCollectionsImmutable" }
kotlinx-coroutines = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
```

---

## 🏗️ Architektur

### Clean Architecture — Strikt einhalten!

```
app/src/main/java/com/syncjam/
├── core/
│   ├── common/                    # Result<T>, Extensions, Constants
│   │   ├── Result.kt             # sealed class Result<out T>
│   │   ├── Extensions.kt
│   │   └── Constants.kt
│   ├── di/                        # Hilt AppModule, NetworkModule, DatabaseModule
│   │   ├── AppModule.kt
│   │   ├── NetworkModule.kt
│   │   └── DatabaseModule.kt
│   ├── network/                   # WebSocket client wrapper
│   │   ├── WebSocketClient.kt
│   │   └── WebSocketEvent.kt
│   └── ui/                        # Theme, shared components, design tokens
│       ├── theme/
│       │   ├── SyncJamTheme.kt
│       │   ├── Color.kt
│       │   ├── Type.kt
│       │   └── Shape.kt
│       └── components/
│           ├── SyncJamButton.kt
│           ├── SyncJamCard.kt
│           ├── LoadingIndicator.kt
│           └── ErrorDialog.kt
│
├── feature/
│   ├── library/                   # Lokale Musikbibliothek
│   │   ├── data/
│   │   │   ├── MediaStoreScanner.kt
│   │   │   ├── LocalTrackDao.kt
│   │   │   └── LibraryRepositoryImpl.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Track.kt
│   │   │   │   ├── Album.kt
│   │   │   │   └── Artist.kt
│   │   │   ├── repository/
│   │   │   │   └── LibraryRepository.kt
│   │   │   └── usecase/
│   │   │       ├── GetTracksUseCase.kt
│   │   │       ├── GetAlbumsUseCase.kt
│   │   │       ├── SearchLibraryUseCase.kt
│   │   │       └── ScanMediaStoreUseCase.kt
│   │   └── presentation/
│   │       ├── LibraryScreen.kt
│   │       ├── LibraryViewModel.kt
│   │       ├── LibraryUiState.kt
│   │       └── components/
│   │           ├── TrackListItem.kt
│   │           ├── AlbumGrid.kt
│   │           └── ArtistRow.kt
│   │
│   ├── session/                   # Jam Session (Kernfeature)
│   │   ├── data/
│   │   │   ├── SessionRepositoryImpl.kt
│   │   │   └── SupabaseSessionDataSource.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Session.kt
│   │   │   │   ├── Participant.kt
│   │   │   │   └── SessionCode.kt
│   │   │   ├── repository/
│   │   │   │   └── SessionRepository.kt
│   │   │   └── usecase/
│   │   │       ├── CreateSessionUseCase.kt
│   │   │       ├── JoinSessionUseCase.kt
│   │   │       ├── LeaveSessionUseCase.kt
│   │   │       └── ObserveSessionUseCase.kt
│   │   └── presentation/
│   │       ├── SessionScreen.kt
│   │       ├── SessionViewModel.kt
│   │       ├── SessionUiState.kt
│   │       ├── SessionEvent.kt
│   │       └── components/
│   │           ├── ParticipantAvatarRow.kt
│   │           ├── SessionCodeCard.kt
│   │           └── SessionHeader.kt
│   │
│   ├── player/                    # Audio Playback + Sync
│   │   ├── data/
│   │   │   ├── PlaybackService.kt          # Media3 MediaSessionService
│   │   │   ├── PlaybackRepositoryImpl.kt
│   │   │   └── SyncedPlayerController.kt   # Sync-aware Player wrapper
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── PlaybackState.kt
│   │   │   │   ├── PlayerCommand.kt
│   │   │   │   └── NowPlaying.kt
│   │   │   ├── repository/
│   │   │   │   └── PlaybackRepository.kt
│   │   │   └── usecase/
│   │   │       ├── PlayTrackUseCase.kt
│   │   │       ├── SyncPlaybackUseCase.kt
│   │   │       └── ControlPlaybackUseCase.kt
│   │   └── presentation/
│   │       ├── MiniPlayerBar.kt
│   │       ├── FullPlayerScreen.kt
│   │       ├── PlayerViewModel.kt
│   │       └── components/
│   │           ├── AlbumArtCover.kt
│   │           ├── PlaybackControls.kt
│   │           ├── ProgressSlider.kt
│   │           └── AudioVisualizer.kt
│   │
│   ├── voice/                     # Voice Chat (LiveKit)
│   │   ├── data/
│   │   │   ├── VoiceRepositoryImpl.kt
│   │   │   └── LiveKitTokenService.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── VoiceState.kt
│   │   │   │   └── VoiceParticipant.kt
│   │   │   └── repository/
│   │   │       └── VoiceRepository.kt
│   │   └── presentation/
│   │       ├── VoiceOverlay.kt
│   │       ├── VoiceViewModel.kt
│   │       └── components/
│   │           ├── MuteButton.kt
│   │           ├── SpeakingIndicator.kt
│   │           └── VoiceParticipantChip.kt
│   │
│   ├── social/                    # Reaktionen, GIFs, Chat
│   │   ├── data/
│   │   │   ├── ReactionRepositoryImpl.kt
│   │   │   ├── ChatRepositoryImpl.kt
│   │   │   └── GiphyDataSource.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Reaction.kt
│   │   │   │   ├── ChatMessage.kt
│   │   │   │   └── GifResult.kt
│   │   │   └── repository/
│   │   │       ├── ReactionRepository.kt
│   │   │       └── ChatRepository.kt
│   │   └── presentation/
│   │       ├── ReactionOverlay.kt
│   │       ├── ChatSheet.kt
│   │       ├── SocialViewModel.kt
│   │       └── components/
│   │           ├── FloatingEmoji.kt
│   │           ├── EmojiPicker.kt
│   │           ├── GifPicker.kt
│   │           ├── ChatBubble.kt
│   │           └── MessageInput.kt
│   │
│   ├── voting/                    # Track-Voting & Queue
│   │   ├── data/
│   │   │   └── VotingRepositoryImpl.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── TrackRequest.kt
│   │   │   │   └── Vote.kt
│   │   │   ├── repository/
│   │   │   │   └── VotingRepository.kt
│   │   │   └── usecase/
│   │   │       ├── RequestTrackUseCase.kt
│   │   │       ├── VoteTrackUseCase.kt
│   │   │       └── GetQueueUseCase.kt
│   │   └── presentation/
│   │       ├── QueueScreen.kt
│   │       ├── VotingViewModel.kt
│   │       └── components/
│   │           ├── QueueTrackItem.kt
│   │           ├── VoteButtons.kt
│   │           └── AddToQueueFAB.kt
│   │
│   └── auth/                      # Auth & Profil
│       ├── data/
│       │   └── AuthRepositoryImpl.kt
│       ├── domain/
│       │   ├── model/
│       │   │   └── User.kt
│       │   └── repository/
│       │       └── AuthRepository.kt
│       └── presentation/
│           ├── LoginScreen.kt
│           ├── AuthViewModel.kt
│           └── ProfileSheet.kt
│
├── sync/                          # Sync Engine (eigenes Package)
│   ├── NtpClockSync.kt           # NTP-style Uhrensynchronisation
│   ├── SyncProtocol.kt           # Command-Protokoll Sealed Classes
│   ├── SyncEngine.kt             # Orchestriert Sync-Logik
│   ├── SyncStateMachine.kt       # State Machine für Session-Lifecycle
│   └── DriftCorrector.kt         # Adaptive Playback-Rate Korrektur
│
├── db/
│   ├── SyncJamDatabase.kt        # Room Database
│   └── entity/
│       ├── LocalTrackEntity.kt
│       └── SessionHistoryEntity.kt
│
├── SyncJamApp.kt                  # @HiltAndroidApp Application class
├── MainActivity.kt                # Single Activity
└── SyncJamNavGraph.kt             # NavHost mit allen Routes
```

### Server (separates Modul: /server)
```
server/src/main/kotlin/com/syncjam/server/
├── Application.kt                 # Ktor main()
├── plugins/
│   ├── Routing.kt
│   ├── WebSockets.kt
│   ├── Serialization.kt
│   └── CORS.kt
├── session/
│   ├── SessionManager.kt         # Room-Management (ConcurrentHashMap)
│   ├── SessionState.kt           # Authoritative State pro Session
│   └── SyncBroadcaster.kt        # Timestamped Command Broadcasting
├── auth/
│   └── TokenValidator.kt         # Supabase JWT Validation
└── model/
    ├── SyncCommand.kt            # Shared Protocol (kotlinx.serialization)
    ├── SessionInfo.kt
    └── ParticipantInfo.kt
```

---

## 🔄 Sync-Protokoll (KERN DER APP)

### NTP-Style Clock Synchronisation
```
Client              Server
  |--- T1 (send) --->|
  |                   | T2 (receive)
  |                   | T3 (respond)
  |<-- T3,T2,T1 --- T4 (receive)

RTT = (T4 - T1) - (T3 - T2)
Offset = [(T2 - T1) + (T3 - T4)] / 2
```
- Bei Session-Join: 5 Samples, Minimum-RTT wählen
- Offset alle 30s refreshen
- Alle Timestamps in Server-Clock-Domain

### Sync-Kommandos (kotlinx.serialization)
```kotlin
@Serializable
sealed interface SyncCommand {
    @Serializable @SerialName("play")
    data class Play(
        val trackId: String,
        val positionMs: Long,
        val serverTimestampMs: Long
    ) : SyncCommand

    @Serializable @SerialName("pause")
    data class Pause(val positionMs: Long, val serverTimestampMs: Long) : SyncCommand

    @Serializable @SerialName("seek")
    data class Seek(val positionMs: Long, val serverTimestampMs: Long) : SyncCommand

    @Serializable @SerialName("skip")
    data class Skip(val nextTrackId: String, val serverTimestampMs: Long) : SyncCommand

    @Serializable @SerialName("queue_update")
    data class QueueUpdate(val queue: List<QueueEntry>, val serverTimestampMs: Long) : SyncCommand

    @Serializable @SerialName("heartbeat")
    data class Heartbeat(val positionMs: Long, val clientTimestampMs: Long) : SyncCommand

    @Serializable @SerialName("state_snapshot")
    data class StateSnapshot(
        val sessionId: String,
        val hostId: String,
        val currentTrack: TrackInfo?,
        val positionMs: Long,
        val isPlaying: Boolean,
        val queue: List<QueueEntry>,
        val participants: List<ParticipantInfo>,
        val serverTimestampMs: Long
    ) : SyncCommand

    @Serializable @SerialName("track_transfer_offer")
    data class TrackTransferOffer(
        val trackId: String,
        val trackTitle: String,
        val trackArtist: String,
        val durationMs: Long,
        val fileSizeBytes: Long,
        val codec: String
    ) : SyncCommand

    @Serializable @SerialName("track_transfer_request")
    data class TrackTransferRequest(val trackId: String, val requesterId: String) : SyncCommand
}
```

### Drift-Korrektur Thresholds
| Drift | Aktion |
|---|---|
| < 150ms | Keine Korrektur nötig |
| 150–500ms | Adaptive Playback-Rate (1.02× oder 0.98×) |
| 500–2000ms | Seek-Korrektur |
| > 2000ms | Full Re-Sync via StateSnapshot |

NIEMALS harte Seeks unter 500ms — hörbare Glitches!

---

## 🎵 Audio File Transfer

### Wenn User A einen Track hat den User B nicht hat:
1. Server sendet `TrackTransferOffer` an alle Teilnehmer
2. Teilnehmer ohne Track senden `TrackTransferRequest`
3. Track-Owner streamt via WebSocket Binary Frames (4KB Chunks, Opus 128kbps)
4. Empfänger: 500ms Jitter Buffer → Custom Media3 DataSource → Playback
5. Parallel: Upload zu Supabase Storage für späteren Zugriff

### Wenn beide den Track lokal haben:
- Match via Metadaten: `title + artist + duration (±2s)`
- Jeder spielt seine lokale Datei → nur Sync-Kommandos
- Schnellster und bandbreitenschonendster Pfad

---

## 🎙️ Voice Chat

- **LiveKit** für WebRTC Voice
- Music ducking: `player.volume = 0.25f` wenn Voice aktiv
- Mute: `room.localParticipant.setMicrophoneEnabled(false)`
- Push-to-Talk Option für Sessions > 4 Personen
- Token-Generierung via Ktor Server → LiveKit Server SDK

---

## ✨ Interaktive Features

### Floating Emoji Reactions
- Broadcast via Supabase Realtime Broadcast Channel
- Animation: Y -800dp/2s, Alpha 1→0, `graphicsLayer` für Performance
- Max 20 gleichzeitige Reactions auf Screen

### GIF Sharing
- Giphy SDK für Picker
- Nur URL via Broadcast senden (nicht das GIF!)
- Anzeige via Coil GIF Decoder
- Rate Limit: 1 GIF / User / 5s

### Track-Voting
- Supabase Postgres + Realtime
- Score-basierte Queue-Ordnung
- 1 Vote pro User pro Track Request
- Trigger-based Score-Update

### Chat
- Supabase Broadcast (ephemer, verschwindet nach Session)
- Max 500 Zeichen, Markdown-Light

---

## 🎨 UI/UX Design

### Dynamic Album Art Theming
```kotlin
DynamicMaterialTheme(
    seedColor = albumArtDominantColor,
    useDarkTheme = true,
    animate = true,
    style = PaletteStyle.Expressive
)
```

### Navigation Routes
```kotlin
@Serializable sealed interface Route {
    @Serializable data object Home : Route
    @Serializable data object Library : Route
    @Serializable data class Session(val sessionId: String) : Route
    @Serializable data object CreateSession : Route
    @Serializable data class JoinSession(val code: String? = null) : Route
    @Serializable data object Profile : Route
}
```

### Design Rules
- Dark Theme First
- Glassmorphism für Overlays (API 31+ blur, Fallback semi-transparent)
- Shared Element Transitions (Mini-Player ↔ Full-Player)
- Marquee für lange Track-Titel
- Lottie für Reaction-Animationen
- Audio Waveform Visualizer
- NIEMALS `Color(0xFF...)` — immer `MaterialTheme.colorScheme.*`

---

## 🗄️ Room Database

```kotlin
@Entity(tableName = "local_tracks")
data class LocalTrackEntity(
    @PrimaryKey val id: String,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtUri: String?,
    val durationMs: Long,
    val fileSize: Long,
    val mimeType: String,
    val bitrate: Int?,
    val sampleRate: Int?,
    val lastModified: Long,
    val lastPlayed: Long? = null,
    val playCount: Int = 0
)

@Entity(tableName = "sessions_history")
data class SessionHistoryEntity(
    @PrimaryKey val id: String,
    val sessionCode: String,
    val hostName: String,
    val participantCount: Int,
    val tracksPlayed: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val lastTrackTitle: String?,
    val lastTrackArtist: String?
)
```

---

## 🌐 Supabase Schema

```sql
-- Profiles
CREATE TABLE profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Sessions
CREATE TABLE sessions (
    id TEXT PRIMARY KEY,
    host_id UUID NOT NULL REFERENCES auth.users(id),
    name TEXT NOT NULL DEFAULT 'Jam Session',
    is_active BOOLEAN DEFAULT true,
    max_participants INT DEFAULT 8,
    created_at TIMESTAMPTZ DEFAULT now(),
    ended_at TIMESTAMPTZ
);

-- Participants
CREATE TABLE session_participants (
    session_id TEXT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id),
    joined_at TIMESTAMPTZ DEFAULT now(),
    is_active BOOLEAN DEFAULT true,
    PRIMARY KEY (session_id, user_id)
);

-- Track Requests
CREATE TABLE track_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id TEXT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    track_title TEXT NOT NULL,
    track_artist TEXT NOT NULL,
    track_duration_ms BIGINT NOT NULL,
    track_file_url TEXT,
    requested_by UUID NOT NULL REFERENCES auth.users(id),
    score INT DEFAULT 0,
    is_played BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Votes
CREATE TABLE votes (
    user_id UUID NOT NULL REFERENCES auth.users(id),
    request_id UUID NOT NULL REFERENCES track_requests(id) ON DELETE CASCADE,
    vote_type SMALLINT NOT NULL CHECK (vote_type IN (-1, 1)),
    created_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (user_id, request_id)
);

-- RLS aktivieren
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE session_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE track_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE votes ENABLE ROW LEVEL SECURITY;

-- Score Auto-Update Trigger
CREATE OR REPLACE FUNCTION update_track_score()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE track_requests
    SET score = (SELECT COALESCE(SUM(vote_type), 0) FROM votes WHERE request_id = COALESCE(NEW.request_id, OLD.request_id))
    WHERE id = COALESCE(NEW.request_id, OLD.request_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_vote_change
AFTER INSERT OR UPDATE OR DELETE ON votes
FOR EACH ROW EXECUTE FUNCTION update_track_score();
```

---

## 📱 Permissions

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

---

## ⚙️ Coding-Regeln (VERBINDLICH)

### Kotlin / Compose
- **StateFlow + collectAsStateWithLifecycle()** — NIEMALS collectAsState()
- **LazyColumn** immer mit `key = { it.id }` und `contentType`
- **derivedStateOf** für abgeleiteten State
- **graphicsLayer { }** für animierte Properties
- **Modifier.offset { }** (Lambda) für animierte Offsets
- **@Immutable** + ImmutableList für alle UI State Data Classes
- **Sealed Result<T>** für Error Handling — kein raw try-catch in ViewModels
- **Mapper-Funktionen** Entity ↔ Domain ↔ Ui
- **Kein `Any`**, keine ungetypten String-IDs
- **Room mit KSP**, @Upsert statt @Insert+@Update
- **Type-Safe Navigation** mit @Serializable Routes
- **Hilt** für DI

### Netzwerk
- **Exponential Backoff + Jitter** für WebSocket Reconnect
- **supervisorScope** für parallel laufende Coroutines
- **Ktor Client** + kotlinx.serialization
- **Ping/Pong**: 15s Intervall, 10s Timeout

### Testing
- **MockK** für Mocking
- **Turbine** für Flow-Testing
- Unit Tests für alle UseCases + ViewModels

---

## ⚠️ Workflow-Regeln

1. **IMMER `tasks/todo.md` lesen** bevor du anfängst
2. **IMMER `tasks/lessons.md` lesen** bei Session-Start
3. **Plan Mode** für JEDE nicht-triviale Aufgabe (3+ Schritte)
4. **Subagents** für Research und parallele Analyse
5. **Verifizieren** bevor Task als erledigt markiert — Build muss laufen
6. **Eleganz** über Quick-Fixes
7. **Lessons updaten** nach JEDER Korrektur
8. **Minimal Impact** — Nur anfassen was nötig ist
9. **Root Cause** finden, keine temporären Fixes
10. **Fortschritt tracken** in `tasks/todo.md`
