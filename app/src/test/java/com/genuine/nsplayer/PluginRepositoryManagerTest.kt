package com.genuine.nsplayer

import com.genuine.nsplayer.data.local.PluginDao
import com.genuine.nsplayer.data.local.PluginEntity
import com.genuine.nsplayer.data.local.PluginRepositoryEntity
import com.genuine.nsplayer.data.model.PluginCapability
import com.genuine.nsplayer.data.model.PluginStatus
import com.genuine.nsplayer.data.repository.PluginRepositoryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test

class PluginRepositoryManagerTest {

    private val dummyPluginDao = object : PluginDao {
        override fun getAllPlugins(): Flow<List<PluginEntity>> = flowOf(emptyList())
        override fun getInstalledPlugins(): Flow<List<PluginEntity>> = flowOf(emptyList())
        override fun getPluginsByRepository(repoUrl: String): Flow<List<PluginEntity>> = flowOf(emptyList())
        override suspend fun getPluginById(id: String): PluginEntity? = null
        override suspend fun insertOrUpdate(plugin: PluginEntity) {}
        override suspend fun insertAll(plugins: List<PluginEntity>) {}
        override suspend fun setPluginEnabled(id: String, enabled: Boolean) {}
        override suspend fun setPluginStatus(id: String, status: String) {}
        override suspend fun deletePlugin(id: String) {}
        override suspend fun deleteAvailablePluginsForRepo(repoUrl: String) {}
        override fun getAllRepositories(): Flow<List<PluginRepositoryEntity>> = flowOf(emptyList())
        override suspend fun getRepositoryById(id: Long): PluginRepositoryEntity? = null
        override suspend fun getRepositoryByUrl(url: String): PluginRepositoryEntity? = null
        override suspend fun insertRepository(repo: PluginRepositoryEntity): Long = 1L
        override suspend fun updateRepository(repo: PluginRepositoryEntity) {}
        override suspend fun updateRepoStats(id: Long, count: Int, timestamp: Long) {}
        override suspend fun deleteRepository(id: Long) {}

        override fun getAllPluginMedia(): Flow<List<com.genuine.nsplayer.data.local.PluginMediaEntity>> = flowOf(emptyList())
        override fun getMediaByPlugin(pluginId: String): Flow<List<com.genuine.nsplayer.data.local.PluginMediaEntity>> = flowOf(emptyList())
        override fun getMediaByType(type: String): Flow<List<com.genuine.nsplayer.data.local.PluginMediaEntity>> = flowOf(emptyList())
        override fun getMediaByCategory(category: String): Flow<List<com.genuine.nsplayer.data.local.PluginMediaEntity>> = flowOf(emptyList())
        override suspend fun getMediaById(id: String): com.genuine.nsplayer.data.local.PluginMediaEntity? = null
        override fun searchPluginMedia(query: String): Flow<List<com.genuine.nsplayer.data.local.PluginMediaEntity>> = flowOf(emptyList())
        override suspend fun insertMedia(items: List<com.genuine.nsplayer.data.local.PluginMediaEntity>) {}
        override suspend fun deleteMediaByPlugin(pluginId: String) {}
        override suspend fun deleteAllMedia() {}
        override suspend fun getMediaCountForPlugin(pluginId: String): Int = 0
    }

    @Test
    fun testParseCloudStreamPluginsJson() {
        val manager = PluginRepositoryManager(
            context = null,
            pluginDao = dummyPluginDao
        )

        val sampleJson = """
        [
            {
                "name": "Super Stream",
                "internalName": "super-stream",
                "version": "2.1.0",
                "versionCode": 21,
                "author": "CommunityDev",
                "description": "High quality streams for movies and series",
                "iconUrl": "https://example.com/icon.png",
                "url": "https://example.com/plugin.cs3",
                "tvTypes": ["Movie", "TvSeries", "LiveStream"]
            },
            {
                "name": "Anime Hub",
                "internalName": "anime-hub",
                "version": "1.0.4",
                "versionCode": 5,
                "author": "AnimeFan",
                "description": "Anime sub and dub catalog",
                "icon": "https://example.com/anime.png",
                "types": ["Anime"]
            }
        ]
        """.trimIndent()

        val parsed = manager.parseRepositoryPayload(sampleJson, "https://example.com/plugins.json")

        assertEquals(2, parsed.size)

        val p1 = parsed[0]
        assertEquals("Super Stream", p1.name)
        assertEquals("super-stream", p1.id)
        assertEquals("2.1.0", p1.version)
        assertEquals(21, p1.versionCode)
        assertEquals("CommunityDev", p1.author)
        assertTrue(p1.capabilities.contains(PluginCapability.MOVIES))
        assertTrue(p1.capabilities.contains(PluginCapability.SERIES))
        assertTrue(p1.capabilities.contains(PluginCapability.LIVE_TV))
        assertEquals(PluginStatus.AVAILABLE, p1.status)

        val p2 = parsed[1]
        assertEquals("Anime Hub", p2.name)
        assertEquals("anime-hub", p2.id)
        assertTrue(p2.capabilities.contains(PluginCapability.ANIME))
    }

    @Test
    fun testParseSinglePluginManifest() {
        val manager = PluginRepositoryManager(
            context = null,
            pluginDao = dummyPluginDao
        )

        val manifestJson = """
        {
            "id": "sports-live",
            "name": "Global Sports Live",
            "version": "1.2.0",
            "versionCode": 12,
            "author": "SportsStreamer",
            "description": "Live global sports streams",
            "capabilities": ["LIVE_TV"]
        }
        """.trimIndent()

        val parsed = manager.parseRepositoryPayload(manifestJson, "https://example.com/manifest.json")
        assertEquals(1, parsed.size)
        assertEquals("Global Sports Live", parsed[0].name)
        assertEquals("sports-live", parsed[0].id)
        assertTrue(parsed[0].capabilities.contains(PluginCapability.LIVE_TV))
    }
}
