package com.syncjam.server.queue

import com.syncjam.server.model.QueueEntry
import com.syncjam.server.model.TrackInfo
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Serializable
data class PlaylistTrack(
    val requestId: String,
    val trackInfo: TrackInfo,
    val score: Int = 0,
    val requestedBy: String,
    val requestedByName: String,
    val source: TrackSource = TrackSource.LOCAL,
    val youtubeId: String? = null,
    val thumbnailUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Serializable
enum class TrackSource { LOCAL, YOUTUBE }

class PlaylistManager {
    // sessionCode -> mutable ordered list of tracks
    private val playlists = ConcurrentHashMap<String, MutableList<PlaylistTrack>>()
    // sessionCode -> current playback index
    private val currentIndex = ConcurrentHashMap<String, AtomicInteger>()
    // sessionCode -> trackId of the most recently advanced track (prevents double-advance on stale TrackEnded)
    private val lastAdvancedFromId = ConcurrentHashMap<String, String>()
    // sessionCode -> "$requestId:$userId" -> voteType (-1 or +1)
    private val votes = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

    fun getPlaylist(sessionCode: String): List<PlaylistTrack> =
        playlists.getOrPut(sessionCode) { mutableListOf() }.toList()

    fun addTrack(sessionCode: String, track: PlaylistTrack): List<PlaylistTrack> {
        val list = playlists.getOrPut(sessionCode) { mutableListOf() }
        synchronized(list) {
            list.add(track)
            // Read curIdx inside synchronized to avoid race with advanceToNext()
            val curIdx = currentIndex.getOrPut(sessionCode) { AtomicInteger(0) }.get()
            sortUnplayed(list, curIdx)
        }
        return list.toList()
    }

    /** Stable sort only the items at [fromIndex..end] by descending score. */
    private fun sortUnplayed(list: MutableList<PlaylistTrack>, fromIndex: Int) {
        if (fromIndex >= list.size) return
        val unplayed = list.subList(fromIndex, list.size).sortedByDescending { it.score }
        for (i in unplayed.indices) list[fromIndex + i] = unplayed[i]
    }

    fun removeTrack(sessionCode: String, requestId: String): List<PlaylistTrack> {
        val list = playlists.getOrPut(sessionCode) { mutableListOf() }
        synchronized(list) {
            list.removeIf { it.requestId == requestId }
        }
        // Also remove associated votes
        votes[sessionCode]?.keys?.removeIf { it.startsWith("$requestId:") }
        return list.toList()
    }

    /**
     * Record a vote (voteType: +1 or -1) from [userId] for [requestId].
     * Each user can change their vote; the score is always recomputed from all votes.
     * Returns the updated, score-sorted playlist.
     */
    fun vote(sessionCode: String, requestId: String, userId: String, voteType: Int): List<PlaylistTrack> {
        require(voteType == 1 || voteType == -1) { "voteType must be +1 or -1" }
        val sessionVotes = votes.getOrPut(sessionCode) { ConcurrentHashMap() }
        val voteKey = "$requestId:$userId"
        sessionVotes[voteKey] = voteType

        val list = playlists.getOrPut(sessionCode) { mutableListOf() }
        synchronized(list) {
            val idx = list.indexOfFirst { it.requestId == requestId }
            if (idx >= 0) {
                val newScore = sessionVotes.entries
                    .filter { it.key.startsWith("$requestId:") }
                    .sumOf { it.value }
                list[idx] = list[idx].copy(score = newScore)
                // Read curIdx inside synchronized to avoid race with advanceToNext()
                val curIdx = currentIndex.getOrPut(sessionCode) { AtomicInteger(0) }.get()
                sortUnplayed(list, curIdx)
            }
        }
        return list.toList()
    }

    fun getCurrentTrack(sessionCode: String): PlaylistTrack? {
        val list = playlists[sessionCode] ?: return null
        val idx = currentIndex.getOrPut(sessionCode) { AtomicInteger(0) }.get()
        return list.getOrNull(idx)
    }

    /**
     * Advance the internal cursor to the next track and return it.
     * [fromTrackId] is the ID of the track that just ended — acts as an idempotency key
     * so that a stale/duplicate TrackEnded for the same track cannot advance the queue twice.
     * Returns null when the queue is exhausted or the advance was already performed.
     */
    fun advanceToNext(sessionCode: String, fromTrackId: String): PlaylistTrack? {
        // Idempotency guard: if we already advanced away from this track, ignore the duplicate
        val previous = lastAdvancedFromId.put(sessionCode, fromTrackId)
        if (previous == fromTrackId) return null  // duplicate TrackEnded

        val list = playlists[sessionCode] ?: return null
        val idx = currentIndex.getOrPut(sessionCode) { AtomicInteger(0) }
        return list.getOrNull(idx.incrementAndGet())
    }

    /**
     * Remove all tracks up to (but not including) the current index,
     * then reset the cursor to 0.
     */
    fun clearPlayed(sessionCode: String) {
        val list = playlists[sessionCode] ?: return
        val idx = currentIndex[sessionCode]?.get() ?: 0
        synchronized(list) {
            repeat(minOf(idx, list.size)) {
                if (list.isNotEmpty()) list.removeAt(0)
            }
        }
        currentIndex[sessionCode]?.set(0)
    }

    /** Remove all state for a session (on session close). */
    fun clearPlaylist(sessionCode: String) {
        playlists.remove(sessionCode)
        currentIndex.remove(sessionCode)
        lastAdvancedFromId.remove(sessionCode)
        votes.remove(sessionCode)
    }

    fun getCurrentIndex(sessionCode: String): Int =
        currentIndex.getOrPut(sessionCode) { AtomicInteger(0) }.get()

    /** Convert the current playlist to the wire-format QueueEntry list. */
    fun toQueueEntries(sessionCode: String): List<QueueEntry> =
        getPlaylist(sessionCode).map {
            QueueEntry(
                requestId = it.requestId,
                trackInfo = it.trackInfo,
                score = it.score,
                requestedBy = it.requestedBy,
                requestedByName = it.requestedByName,
                source = it.source.name,
                youtubeId = it.youtubeId,
                thumbnailUrl = it.thumbnailUrl
            )
        }
}
