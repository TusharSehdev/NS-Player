package com.genuine.nsplayer.data.repository

import android.content.Context
import com.genuine.nsplayer.data.local.*
import com.genuine.nsplayer.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PluginRepositoryManager(
    private val context: Context? = null,
    private val pluginDao: PluginDao
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun getAllRepositories(): Flow<List<PluginRepositoryItem>> =
        pluginDao.getAllRepositories().map { list -> list.map { it.toModel() } }

    fun getAllPlugins(): Flow<List<PluginItem>> =
        pluginDao.getAllPlugins().map { list -> list.map { it.toModel() } }

    fun getInstalledPlugins(): Flow<List<PluginItem>> =
        pluginDao.getInstalledPlugins().map { list -> list.map { it.toModel() } }

    suspend fun addRepository(name: String, url: String): Long = withContext(Dispatchers.IO) {
        val trimmedUrl = url.trim()
        val displayName = if (name.isBlank()) {
            trimmedUrl.substringAfterLast("/").substringBefore(".").ifBlank { "NEXORA Repository" }
        } else {
            name.trim()
        }

        val existing = pluginDao.getRepositoryByUrl(trimmedUrl)
        val repoId = if (existing != null) {
            existing.id
        } else {
            val entity = PluginRepositoryEntity(
                name = displayName,
                url = trimmedUrl,
                lastUpdated = System.currentTimeMillis()
            )
            pluginDao.insertRepository(entity)
        }

        // Fetch and parse remote repository manifest
        refreshRepository(repoId)
        repoId
    }

    suspend fun refreshRepository(repoId: Long) = withContext(Dispatchers.IO) {
        val repo = pluginDao.getRepositoryById(repoId) ?: return@withContext
        val request = Request.Builder()
            .url(repo.url)
            .header("User-Agent", "NEXORA/1.0 (Android TV & Mobile)")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }

        val bodyString = response.body?.string() ?: throw Exception("Empty repository response")
        val parsedPlugins = parseRepositoryPayload(bodyString, repo.url)

        for (plugin in parsedPlugins) {
            val existing = pluginDao.getPluginById(plugin.id)
            if (existing == null) {
                pluginDao.insertOrUpdate(plugin.toEntity())
            } else {
                // If already installed, check for update
                val currentStatus = try {
                    PluginStatus.valueOf(existing.status)
                } catch (_: Exception) {
                    PluginStatus.AVAILABLE
                }

                val newStatus = if (currentStatus == PluginStatus.INSTALLED && plugin.versionCode > existing.versionCode) {
                    PluginStatus.UPDATE_AVAILABLE
                } else {
                    currentStatus
                }

                val updated = existing.copy(
                    version = plugin.version,
                    versionCode = plugin.versionCode,
                    author = plugin.author,
                    description = plugin.description,
                    iconUrl = plugin.iconUrl ?: existing.iconUrl,
                    packageUrl = plugin.packageUrl ?: existing.packageUrl,
                    status = newStatus.name
                )
                pluginDao.insertOrUpdate(updated)
            }
        }

        pluginDao.updateRepoStats(repoId, parsedPlugins.size, System.currentTimeMillis())
    }

    suspend fun installPlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = pluginDao.getPluginById(pluginId) ?: return@withContext
        pluginDao.insertOrUpdate(
            plugin.copy(
                status = PluginStatus.INSTALLED.name,
                isEnabled = true
            )
        )
    }

    suspend fun updatePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = pluginDao.getPluginById(pluginId) ?: return@withContext
        pluginDao.insertOrUpdate(
            plugin.copy(
                status = PluginStatus.INSTALLED.name,
                isEnabled = true
            )
        )
    }

    suspend fun setPluginEnabled(pluginId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        pluginDao.setPluginEnabled(pluginId, enabled)
    }

    suspend fun uninstallPlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = pluginDao.getPluginById(pluginId) ?: return@withContext
        if (plugin.repositoryUrl.isNullOrBlank()) {
            pluginDao.deletePlugin(pluginId)
        } else {
            pluginDao.setPluginStatus(pluginId, PluginStatus.AVAILABLE.name)
        }
    }

    suspend fun deleteRepository(repoId: Long) = withContext(Dispatchers.IO) {
        val repo = pluginDao.getRepositoryById(repoId) ?: return@withContext
        pluginDao.deleteAvailablePluginsForRepo(repo.url)
        pluginDao.deleteRepository(repoId)
    }

    /**
     * Parses repository payload supporting both CloudStream `plugins.json` structure
     * and NEXORA manifest schemas.
     */
    fun parseRepositoryPayload(payload: String, repoUrl: String): List<PluginItem> {
        val result = mutableListOf<PluginItem>()
        val trimmed = payload.trim()

        try {
            if (trimmed.startsWith("[")) {
                // Array of plugin definitions (Standard CloudStream plugins.json format)
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    parsePluginJson(obj, repoUrl)?.let { result.add(it) }
                }
            } else if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)
                if (jsonObject.has("plugins")) {
                    val jsonArray = jsonObject.getJSONArray("plugins")
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.optJSONObject(i) ?: continue
                        parsePluginJson(obj, repoUrl)?.let { result.add(it) }
                    }
                } else if (jsonObject.has("id") || jsonObject.has("name")) {
                    // Single plugin manifest
                    parsePluginJson(jsonObject, repoUrl)?.let { result.add(it) }
                }
            }
        } catch (_: Exception) {
            // Resilient handling of malformed JSON
        }
        return result
    }

    private fun parsePluginJson(obj: JSONObject, repoUrl: String): PluginItem? {
        val name = obj.optString("name").ifBlank { obj.optString("internalName") }
        if (name.isBlank()) return null

        val id = obj.optString("id").ifBlank {
            obj.optString("internalName").ifBlank {
                name.lowercase().replace(" ", "-")
            }
        }

        val version = obj.optString("version", "1.0.0")
        val versionCode = obj.optInt("versionCode", 1)
        val author = obj.optString("author", "Community")
        val description = obj.optString("description", "Media extension for NEXORA")
        val iconUrl = obj.optString("iconUrl").ifBlank { obj.optString("icon") }.takeIf { it.isNotBlank() }
        val packageUrl = obj.optString("url").ifBlank { obj.optString("packageUrl") }.takeIf { it.isNotBlank() }
        val fileSize = obj.optLong("fileSize", 0L)
        val sha256 = obj.optString("sha256").takeIf { it.isNotBlank() }

        // Parse capabilities
        val capabilities = mutableListOf<PluginCapability>()
        val typesArray = obj.optJSONArray("types") ?: obj.optJSONArray("tvTypes") ?: obj.optJSONArray("capabilities")
        if (typesArray != null) {
            for (i in 0 until typesArray.length()) {
                val typeStr = typesArray.optString(i)
                when (typeStr.lowercase()) {
                    "livestream", "live", "tv" -> capabilities.add(PluginCapability.LIVE_TV)
                    "movie" -> capabilities.add(PluginCapability.MOVIES)
                    "tvseries", "series", "cartoon" -> capabilities.add(PluginCapability.SERIES)
                    "anime", "animemov" -> capabilities.add(PluginCapability.ANIME)
                    "extractor" -> capabilities.add(PluginCapability.STREAM_EXTRACTOR)
                    else -> PluginCapability.fromString(typeStr)?.let { capabilities.add(it) }
                }
            }
        }

        if (capabilities.isEmpty()) {
            capabilities.add(PluginCapability.MOVIES)
            capabilities.add(PluginCapability.SERIES)
            capabilities.add(PluginCapability.LIVE_TV)
        }

        return PluginItem(
            id = id,
            name = name,
            version = version,
            versionCode = versionCode,
            author = author,
            description = description,
            iconUrl = iconUrl,
            packageUrl = packageUrl,
            repositoryUrl = repoUrl,
            capabilities = capabilities.distinct(),
            status = PluginStatus.AVAILABLE,
            fileSize = fileSize,
            sha256 = sha256,
            isEnabled = true
        )
    }
}
