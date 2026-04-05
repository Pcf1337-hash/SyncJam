package com.syncjam.app.feature.library.data

import android.util.Log
import com.syncjam.app.db.dao.LocalTrackDao
import com.syncjam.app.db.entity.LocalTrackEntity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches album cover art from free APIs:
 * 1. iTunes Search API (primary) — no auth, fast, high-quality
 * 2. Deezer API (fallback) — no auth
 * 3. MusicBrainz + CoverArtArchive (last resort) — no auth, slower
 */
@Singleton
class CoverArtDownloader @Inject constructor(
    private val httpClient: HttpClient,
    private val trackDao: LocalTrackDao
) {
    companion object {
        private const val TAG = "CoverArtDownloader"
        private const val ITUNES_URL = "https://itunes.apple.com/search"
        private const val DEEZER_URL = "https://api.deezer.com/search/album"
        private const val MUSICBRAINZ_URL = "https://musicbrainz.org/ws/2/release"
        private const val CAA_URL = "https://coverartarchive.org/release"
        private const val REQUEST_DELAY_MS = 300L
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun downloadMissingCovers() {
        withContext(Dispatchers.IO) {
            val tracks = trackDao.getTracksWithoutCover()
            Log.i(TAG, "Downloading covers for ${tracks.size} tracks without art")
            tracks.forEach { track ->
                try {
                    val coverUrl = fetchCoverUrl(track)
                    if (coverUrl != null) {
                        trackDao.updateRemoteCoverUrl(track.id, coverUrl)
                        Log.d(TAG, "Cover found for '${track.title}': $coverUrl")
                    }
                    delay(REQUEST_DELAY_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "Cover fetch failed for '${track.title}': ${e.message}")
                }
            }
            Log.i(TAG, "Cover download pass complete")
        }
    }

    private suspend fun fetchCoverUrl(track: LocalTrackEntity): String? {
        itunesCoverUrl(track.artist, track.album)?.let { return it }
        delay(REQUEST_DELAY_MS)
        deezerCoverUrl(track.artist, track.album)?.let { return it }
        delay(REQUEST_DELAY_MS)
        return musicBrainzCoverUrl(track.artist, track.album)
    }

    private suspend fun itunesCoverUrl(artist: String, album: String): String? {
        return try {
            val response = httpClient.get(ITUNES_URL) {
                url {
                    parameters.append("term", "$artist $album")
                    parameters.append("entity", "album")
                    parameters.append("limit", "1")
                }
            }
            if (!response.status.isSuccess()) return null
            val body = response.body<String>()
            val root = json.parseToJsonElement(body).jsonObject
            val results = root["results"]?.jsonArray ?: return null
            if (results.isEmpty()) return null
            results[0].jsonObject["artworkUrl100"]?.jsonPrimitive?.content
                ?.replace("100x100bb", "600x600bb")
        } catch (e: Exception) {
            Log.d(TAG, "iTunes lookup failed: ${e.message}")
            null
        }
    }

    private suspend fun deezerCoverUrl(artist: String, album: String): String? {
        return try {
            val response = httpClient.get(DEEZER_URL) {
                url { parameters.append("q", "$artist $album") }
            }
            if (!response.status.isSuccess()) return null
            val body = response.body<String>()
            val root = json.parseToJsonElement(body).jsonObject
            val data = root["data"]?.jsonArray ?: return null
            if (data.isEmpty()) return null
            data[0].jsonObject["cover_xl"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Log.d(TAG, "Deezer lookup failed: ${e.message}")
            null
        }
    }

    private suspend fun musicBrainzCoverUrl(artist: String, album: String): String? {
        return try {
            val response = httpClient.get(MUSICBRAINZ_URL) {
                url {
                    parameters.append("query", "release:$album artist:$artist")
                    parameters.append("fmt", "json")
                    parameters.append("limit", "1")
                }
                header("User-Agent", "SyncJam/2.0 (github.com/syncjam)")
            }
            if (!response.status.isSuccess()) return null
            val body = response.body<String>()
            val root = json.parseToJsonElement(body).jsonObject
            val releases = root["releases"]?.jsonArray ?: return null
            if (releases.isEmpty()) return null
            val mbid = releases[0].jsonObject["id"]?.jsonPrimitive?.content ?: return null
            val caaResponse = httpClient.get("$CAA_URL/$mbid/front-250") {
                header("User-Agent", "SyncJam/2.0 (github.com/syncjam)")
            }
            if (caaResponse.status.isSuccess()) "$CAA_URL/$mbid/front-250" else null
        } catch (e: Exception) {
            Log.d(TAG, "MusicBrainz lookup failed: ${e.message}")
            null
        }
    }
}
