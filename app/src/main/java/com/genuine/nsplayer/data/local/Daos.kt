package com.genuine.nsplayer.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY id DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("UPDATE playlists SET channelCount = :count, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateChannelCount(id: Long, count: Int, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND groupTitle = :group ORDER BY orderIndex ASC")
    fun getChannelsByGroup(playlistId: Long, group: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND contentType = :type ORDER BY orderIndex ASC")
    fun getChannelsByType(playlistId: Long, type: String): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE playlistId = :playlistId ORDER BY groupTitle ASC")
    fun getCategories(playlistId: Long): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND isFavorite = 1 ORDER BY orderIndex ASC")
    fun getFavoriteChannelsByPlaylist(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' OR groupTitle LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 100")
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: Long): ChannelEntity?

    @Query("UPDATE channels SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: Long, isFav: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylist(playlistId: Long)

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    suspend fun getChannelCount(playlistId: Long): Int
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT 50")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE positionMs > 0 ORDER BY timestamp DESC LIMIT 20")
    fun getContinueWatching(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: HistoryEntity)

    @Query("DELETE FROM playback_history WHERE streamUrl = :streamUrl")
    suspend fun deleteByStreamUrl(streamUrl: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}

@Dao
interface EpgDao {
    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelTvgId = :tvgId 
        AND endEpoch > :currentTime 
        ORDER BY startEpoch ASC 
        LIMIT 10
    """)
    fun getUpcomingPrograms(tvgId: String, currentTime: Long): Flow<List<EpgEntity>>

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelTvgId = :tvgId 
        AND startEpoch <= :currentTime 
        AND endEpoch >= :currentTime 
        LIMIT 1
    """)
    suspend fun getCurrentProgram(tvgId: String, currentTime: Long): EpgEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgEntity>)

    @Query("DELETE FROM epg_programs WHERE endEpoch < :cutoffTime")
    suspend fun pruneOldPrograms(cutoffTime: Long)

    @Query("DELETE FROM epg_programs")
    suspend fun clearAllEpg()
}
