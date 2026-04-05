package com.syncjam.app.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.syncjam.app.db.entity.LocalTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalTrackDao {
    @Query("SELECT * FROM local_tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<LocalTrackEntity>>

    @Query("SELECT * FROM local_tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    fun searchTracks(query: String): Flow<List<LocalTrackEntity>>

    @Query("SELECT * FROM local_tracks WHERE album = :album")
    fun getTracksByAlbum(album: String): Flow<List<LocalTrackEntity>>

    @Query("SELECT * FROM local_tracks WHERE artist = :artist")
    fun getTracksByArtist(artist: String): Flow<List<LocalTrackEntity>>

    @Query("SELECT * FROM local_tracks ORDER BY title ASC")
    suspend fun getAllTracksOnce(): List<LocalTrackEntity>

    @Query("SELECT * FROM local_tracks WHERE id = :id")
    suspend fun getTrackById(id: String): LocalTrackEntity?

    @Upsert
    suspend fun upsertTracks(tracks: List<LocalTrackEntity>)

    @Delete
    suspend fun deleteTrack(track: LocalTrackEntity)

    @Query("DELETE FROM local_tracks")
    suspend fun deleteAll()

    @Query("UPDATE local_tracks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("SELECT * FROM local_tracks WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteTracks(): Flow<List<LocalTrackEntity>>

    @Query("UPDATE local_tracks SET remoteCoverUrl = :url WHERE id = :id")
    suspend fun updateRemoteCoverUrl(id: String, url: String)

    @Query("SELECT * FROM local_tracks WHERE remoteCoverUrl IS NULL AND albumArtUri IS NULL")
    suspend fun getTracksWithoutCover(): List<LocalTrackEntity>
}
