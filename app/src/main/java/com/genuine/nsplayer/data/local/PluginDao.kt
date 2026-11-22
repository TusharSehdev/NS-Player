package com.genuine.nsplayer.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PluginDao {
    @Query("SELECT * FROM plugins ORDER BY name ASC")
    fun getAllPlugins(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE status = 'INSTALLED' OR status = 'UPDATE_AVAILABLE' ORDER BY name ASC")
    fun getInstalledPlugins(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE repositoryUrl = :repoUrl ORDER BY name ASC")
    fun getPluginsByRepository(repoUrl: String): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE id = :id LIMIT 1")
    suspend fun getPluginById(id: String): PluginEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(plugin: PluginEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plugins: List<PluginEntity>)

    @Query("UPDATE plugins SET isEnabled = :enabled WHERE id = :id")
    suspend fun setPluginEnabled(id: String, enabled: Boolean)

    @Query("UPDATE plugins SET status = :status WHERE id = :id")
    suspend fun setPluginStatus(id: String, status: String)

    @Query("DELETE FROM plugins WHERE id = :id")
    suspend fun deletePlugin(id: String)

    @Query("DELETE FROM plugins WHERE repositoryUrl = :repoUrl AND status = 'AVAILABLE'")
    suspend fun deleteAvailablePluginsForRepo(repoUrl: String)

    // Plugin Repositories
    @Query("SELECT * FROM plugin_repositories ORDER BY name ASC")
    fun getAllRepositories(): Flow<List<PluginRepositoryEntity>>

    @Query("SELECT * FROM plugin_repositories WHERE id = :id LIMIT 1")
    suspend fun getRepositoryById(id: Long): PluginRepositoryEntity?

    @Query("SELECT * FROM plugin_repositories WHERE url = :url LIMIT 1")
    suspend fun getRepositoryByUrl(url: String): PluginRepositoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepository(repo: PluginRepositoryEntity): Long

    @Update
    suspend fun updateRepository(repo: PluginRepositoryEntity)

    @Query("UPDATE plugin_repositories SET pluginCount = :count, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateRepoStats(id: Long, count: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM plugin_repositories WHERE id = :id")
    suspend fun deleteRepository(id: Long)
}
