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
    val adminId: String = "",
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
    val isUploadingTrack: Boolean = false,
    val error: String? = null,
    val isMicMuted: Boolean = true,
    val musicVolume: Float = 1f,
    val floatingReactions: ImmutableList<FloatingReactionUi> = persistentListOf(),
    val kickedReason: String? = null,
    val directMessage: DirectMessageNotification? = null
) {
    val isCurrentUserAdmin: Boolean get() = adminId.isNotEmpty() && adminId == currentUserId
}

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
    val isHost: Boolean,
    val isAdmin: Boolean = false,
    val mutedByAdmin: Boolean = false,
    val isSpeaking: Boolean = false
)

@Immutable
data class FloatingReactionUi(
    val id: String = UUID.randomUUID().toString(),
    val emoji: String,
    val xFraction: Float,  // 0f..1f horizontal position
    val durationMs: Long = 2500L
)

@Immutable
data class DirectMessageNotification(
    val fromName: String,
    val message: String
)

sealed interface YtDownloadState {
    data class Downloading(val youtubeId: String, val title: String) : YtDownloadState
    data class Error(val message: String) : YtDownloadState
}

sealed interface SessionEvent {
    data class CreateSession(
        val name: String,
        val userId: String,
        val displayName: String,
        val autoDeleteAfterHours: Int = 0,
        val isPublic: Boolean = false,
        val password: String = ""
    ) : SessionEvent
    data class JoinSession(val code: String, val userId: String, val displayName: String, val password: String = "") : SessionEvent
    data class ConnectToExistingSession(val sessionCode: String, val isHost: Boolean, val displayName: String = "") : SessionEvent
    data object LeaveSession : SessionEvent
    data object TogglePlayPause : SessionEvent
    data class SendReaction(val emoji: String) : SessionEvent
    data class AddLocalTrackToQueue(
        val trackId: String,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val contentUri: String,
        val albumArtUri: String?
    ) : SessionEvent
    data class AddYouTubeTrack(val url: String) : SessionEvent
    data class Vote(val requestId: String, val voteType: Int) : SessionEvent
    data class RemoveFromQueue(val requestId: String) : SessionEvent
    data class SendTrackEnded(val trackId: String) : SessionEvent
    data class Seek(val positionMs: Long) : SessionEvent
    data object ToggleMic : SessionEvent
    data class SetVolume(val volume: Float) : SessionEvent
    data object DismissError : SessionEvent
    // Admin actions
    data class KickUser(val targetUserId: String, val reason: String = "") : SessionEvent
    data class BanUser(val targetUserId: String) : SessionEvent
    data class MuteUser(val targetUserId: String, val muted: Boolean) : SessionEvent
    data class TransferAdmin(val newAdminId: String) : SessionEvent
    data class SendDirectMessage(val targetUserId: String, val message: String) : SessionEvent
    data object DismissKicked : SessionEvent
    data object DismissDirectMessage : SessionEvent
}
