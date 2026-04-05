package com.syncjam.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val streamUrl: String? = null,
    val albumArtUrl: String? = null
)

@Serializable
data class QueueEntry(
    val requestId: String,
    val trackInfo: TrackInfo,
    val score: Int,
    val requestedBy: String,
    val requestedByName: String = "",
    /** "LOCAL" or "YOUTUBE" */
    val source: String = "LOCAL",
    val youtubeId: String? = null,
    val thumbnailUrl: String? = null
)

@Serializable
data class ParticipantInfo(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val isHost: Boolean,
    val isAdmin: Boolean = false
)

@Serializable
sealed interface SyncCommand {
    @Serializable @SerialName("play")
    data class Play(val trackId: String, val positionMs: Long, val serverTimestampMs: Long) : SyncCommand

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

    @Serializable @SerialName("ntp_request")
    data class NtpRequest(val t1: Long) : SyncCommand

    @Serializable @SerialName("ntp_response")
    data class NtpResponse(val t1: Long, val t2: Long, val t3: Long) : SyncCommand

    @Serializable @SerialName("reaction")
    data class Reaction(val emoji: String, val senderId: String, val senderName: String) : SyncCommand

    @Serializable @SerialName("chat_message")
    data class ChatMessage(val senderId: String, val senderName: String, val message: String, val timestampMs: Long) : SyncCommand

    @Serializable @SerialName("vote")
    data class Vote(val requestId: String, val userId: String, val voteType: Int) : SyncCommand

    @Serializable @SerialName("participant_joined")
    data class ParticipantJoined(val participant: ParticipantInfo, val serverTimestampMs: Long) : SyncCommand

    @Serializable @SerialName("participant_left")
    data class ParticipantLeft(val userId: String, val serverTimestampMs: Long) : SyncCommand

    @Serializable @SerialName("error")
    data class Error(val code: String, val message: String) : SyncCommand

    // ── Collaborative Playlist / Queue Commands ──────────────────────────────

    @Serializable @SerialName("add_to_queue")
    data class AddToQueue(
        val requestId: String,
        val trackInfo: TrackInfo,
        val requestedBy: String,
        val requestedByName: String,
        /** "LOCAL" or "YOUTUBE" */
        val source: String = "LOCAL",
        val youtubeId: String? = null,
        val thumbnailUrl: String? = null,
        val serverTimestampMs: Long
    ) : SyncCommand

    @Serializable @SerialName("remove_from_queue")
    data class RemoveFromQueue(
        val requestId: String,
        val serverTimestampMs: Long
    ) : SyncCommand

    @Serializable @SerialName("playlist_update")
    data class PlaylistUpdate(
        val tracks: List<QueueEntry>,
        val currentIndex: Int,
        val serverTimestampMs: Long
    ) : SyncCommand

    // ── YouTube-specific Commands ─────────────────────────────────────────────

    @Serializable @SerialName("youtube_download_started")
    data class YouTubeDownloadStarted(
        val youtubeId: String,
        val title: String,
        val requestedBy: String,
        val serverTimestampMs: Long
    ) : SyncCommand

    @Serializable @SerialName("youtube_download_ready")
    data class YouTubeDownloadReady(
        val youtubeId: String,
        val trackId: String,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val serverTimestampMs: Long
    ) : SyncCommand

    @Serializable @SerialName("track_ended")
    data class TrackEnded(
        val trackId: String,
        val serverTimestampMs: Long
    ) : SyncCommand

    // ── Admin Commands ────────────────────────────────────────────────────────

    @Serializable @SerialName("kick_user")
    data class KickUser(val targetUserId: String, val reason: String = "", val serverTimestampMs: Long = 0L) : SyncCommand

    @Serializable @SerialName("ban_user")
    data class BanUser(val targetUserId: String, val serverTimestampMs: Long = 0L) : SyncCommand

    @Serializable @SerialName("mute_participant")
    data class MuteParticipant(val targetUserId: String, val muted: Boolean, val issuedBy: String, val serverTimestampMs: Long = 0L) : SyncCommand

    @Serializable @SerialName("transfer_host")
    data class TransferHost(val newHostId: String, val serverTimestampMs: Long = 0L) : SyncCommand

    @Serializable @SerialName("transfer_admin")
    data class TransferAdmin(val newAdminId: String, val serverTimestampMs: Long = 0L) : SyncCommand

    @Serializable @SerialName("admin_update")
    data class AdminUpdate(val adminId: String, val serverTimestampMs: Long = 0L) : SyncCommand

    @Serializable @SerialName("you_were_kicked")
    data class YouWereKicked(val reason: String = "", val serverTimestampMs: Long = 0L) : SyncCommand

    @Serializable @SerialName("direct_message")
    data class DirectMessage(val fromUserId: String, val fromName: String, val message: String, val serverTimestampMs: Long = 0L) : SyncCommand

    // ── Track Approval (Non-Host → Host) ─────────────────────────────────────
    /** Server → Host: a non-host wants to add this track; requires approval. */
    @Serializable @SerialName("track_pending_approval")
    data class TrackPendingApproval(
        val requestId: String,
        val trackInfo: TrackInfo,
        val requestedBy: String,
        val requestedByName: String,
        val source: String = "LOCAL",
        val youtubeId: String? = null,
        val thumbnailUrl: String? = null,
        val serverTimestampMs: Long = 0L
    ) : SyncCommand

    /** Host → Server: approve a pending track request. */
    @Serializable @SerialName("track_approved")
    data class TrackApproved(
        val requestId: String,
        val serverTimestampMs: Long = 0L
    ) : SyncCommand

    /** Host → Server: reject a pending track request. */
    @Serializable @SerialName("track_rejected")
    data class TrackRejected(
        val requestId: String,
        val reason: String = "",
        val serverTimestampMs: Long = 0L
    ) : SyncCommand
}
