package com.syncjam.app.feature.session.presentation

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Immutable
data class SessionUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val sessionId: String? = null,
    val sessionCode: String = "",
    val hostId: String = "",
    val currentUserId: String = "",
    val pendingDisplayName: String = "",  // stored before WebSocket connects
    val currentTrack: CurrentTrackUi? = null,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val playlist: ImmutableList<QueueEntryUi> = persistentListOf(),
    val currentQueueIndex: Int = 0,
    val participants: ImmutableList<ParticipantUi> = persistentListOf(),
    val participantCount: Int = 0,
    val ytDownloadState: YtDownloadState? = null,
    val error: String? = null,
    val isMicMuted: Boolean = true,
    val musicVolume: Float = 1f,
    val floatingReactions: ImmutableList<FloatingReactionUi> = persistentListOf()
)

@Immutable
data class CurrentTrackUi(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val albumArtUri: String?,
    /** null = local, non-null = YouTube stream URL */
    val streamUrl: String? = null
)

@Immutable
data class QueueEntryUi(
    val requestId: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val score: Int,
    val requestedBy: String,
    val requestedByName: String,
    val source: TrackSourceUi,
    val youtubeId: String?,
    val thumbnailUrl: String?,
    val isCurrent: Boolean = false
)

enum class TrackSourceUi { LOCAL, YOUTUBE }

@Immutable
data class ParticipantUi(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val isHost: Boolean
)

@Immutable
data class FloatingReactionUi(
    val id: String = UUID.randomUUID().toString(),
    val emoji: String,
    val xFraction: Float,  // 0f..1f horizontal position
    val durationMs: Long = 2500L
)

sealed interface YtDownloadState {
    data class Downloading(val youtubeId: String, val title: String) : YtDownloadState
    data class Error(val message: String) : YtDownloadState
}

sealed interface SessionEvent {
    data class CreateSession(val name: String, val userId: String, val displayName: String, val autoDeleteAfterHours: Int = 0) : SessionEvent
    data class JoinSession(val code: String, val userId: String, val displayName: String) : SessionEvent
    data class ConnectToExistingSession(val sessionCode: String, val isHost: Boolean, val displayName: String = "") : SessionEvent
    data object LeaveSession : SessionEvent
    data object TogglePlayPause : SessionEvent
    data class SendReaction(val emoji: String) : SessionEvent
    data class AddLocalTrackToQueue(
        val trackId: String,
        val title: String,
        val artist: String,
        val durationMs: Long
    ) : SessionEvent
    data class AddYouTubeTrack(val url: String) : SessionEvent
    data class Vote(val requestId: String, val voteType: Int) : SessionEvent
    data class RemoveFromQueue(val requestId: String) : SessionEvent
    data class SendTrackEnded(val trackId: String) : SessionEvent
    data class Seek(val positionMs: Long) : SessionEvent
    data object ToggleMic : SessionEvent
    data class SetVolume(val volume: Float) : SessionEvent
    data object DismissError : SessionEvent
}
