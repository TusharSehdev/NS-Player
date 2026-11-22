package com.genuine.nsplayer.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.genuine.nsplayer.data.model.*
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "plugins",
    indices = [
        Index("repositoryUrl"),
        Index("status"),
        Index("isEnabled")
    ]
)
data class PluginEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val versionCode: Int,
    val author: String,
    val description: String,
    val iconUrl: String?,
    val packageUrl: String?,
    val repositoryUrl: String?,
    val capabilitiesJson: String, // JSON array of string names
    val status: String,
    val fileSize: Long = 0L,
    val sha256: String? = null,
    val isEnabled: Boolean = true,
    val installTime: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "plugin_repositories",
    indices = [Index("url", unique = true)]
)
data class PluginRepositoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val pluginCount: Int = 0,
    val isEnabled: Boolean = true
)

@Entity(
    tableName = "plugin_media",
    indices = [
        Index("pluginId"),
        Index("category"),
        Index("type"),
        Index("title")
    ]
)
data class PluginMediaEntity(
    @PrimaryKey val id: String,
    val pluginId: String,
    val pluginName: String,
    val title: String,
    val posterUrl: String?,
    val bannerUrl: String?,
    val plot: String?,
    val year: Int?,
    val rating: Double?,
    val category: String,
    val type: String, // "MOVIE", "SERIES", "LIVE_TV", "ANIME"
    val streamUrl: String?,
    val dataUrl: String?,
    val episodesJson: String? = null,
    val headersJson: String? = null,
    val addedTime: Long = System.currentTimeMillis()
)

data class PluginSourceItem(
    val pluginId: String,
    val pluginName: String
)

// Mapping helpers
fun PluginEntity.toModel(): PluginItem {
    val caps = mutableListOf<PluginCapability>()
    try {
        if (capabilitiesJson.isNotEmpty()) {
            val arr = JSONArray(capabilitiesJson)
            for (i in 0 until arr.length()) {
                val capStr = arr.getString(i)
                PluginCapability.fromString(capStr)?.let { caps.add(it) }
            }
        }
    } catch (_: Exception) {}

    val s = try {
        PluginStatus.valueOf(status)
    } catch (_: Exception) {
        PluginStatus.AVAILABLE
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
        repositoryUrl = repositoryUrl,
        capabilities = caps,
        status = s,
        fileSize = fileSize,
        sha256 = sha256,
        isEnabled = isEnabled
    )
}

fun PluginItem.toEntity(): PluginEntity {
    val arr = JSONArray()
    capabilities.forEach { arr.put(it.name) }
    return PluginEntity(
        id = id,
        name = name,
        version = version,
        versionCode = versionCode,
        author = author,
        description = description,
        iconUrl = iconUrl,
        packageUrl = packageUrl,
        repositoryUrl = repositoryUrl,
        capabilitiesJson = arr.toString(),
        status = status.name,
        fileSize = fileSize,
        sha256 = sha256,
        isEnabled = isEnabled
    )
}

fun PluginRepositoryEntity.toModel() = PluginRepositoryItem(
    id = id,
    name = name,
    url = url,
    lastUpdated = lastUpdated,
    pluginCount = pluginCount,
    isEnabled = isEnabled
)

fun PluginRepositoryItem.toEntity() = PluginRepositoryEntity(
    id = id,
    name = name,
    url = url,
    lastUpdated = lastUpdated,
    pluginCount = pluginCount,
    isEnabled = isEnabled
)

fun PluginMediaEntity.toModel(): PluginMediaItem {
    val ct = try {
        ContentType.valueOf(type)
    } catch (_: Exception) {
        ContentType.MOVIE
    }

    val episodesList = mutableListOf<PluginEpisode>()
    if (!episodesJson.isNullOrBlank()) {
        try {
            val arr = JSONArray(episodesJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                episodesList.add(
                    PluginEpisode(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        number = obj.optInt("number", i + 1),
                        season = obj.optInt("season", 1),
                        duration = obj.optString("duration").takeIf { it.isNotBlank() },
                        coverImage = obj.optString("coverImage").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (_: Exception) {}
    }

    val headersMap = mutableMapOf<String, String>()
    if (!headersJson.isNullOrBlank()) {
        try {
            val obj = JSONObject(headersJson)
            obj.keys().forEach { k -> headersMap[k] = obj.optString(k) }
        } catch (_: Exception) {}
    }

    return PluginMediaItem(
        id = id,
        pluginId = pluginId,
        pluginName = pluginName,
        title = title,
        posterUrl = posterUrl,
        bannerUrl = bannerUrl,
        plot = plot,
        year = year,
        rating = rating,
        category = category,
        type = ct,
        streamUrl = streamUrl,
        dataUrl = dataUrl,
        episodes = episodesList,
        headers = headersMap
    )
}

fun PluginMediaItem.toEntity(): PluginMediaEntity {
    val epArr = JSONArray()
    episodes.forEach { ep ->
        val obj = JSONObject()
        obj.put("id", ep.id)
        obj.put("title", ep.title)
        obj.put("number", ep.number)
        obj.put("season", ep.season)
        ep.duration?.let { obj.put("duration", it) }
        ep.coverImage?.let { obj.put("coverImage", it) }
        epArr.put(obj)
    }

    val hObj = JSONObject()
    headers.forEach { (k, v) -> hObj.put(k, v) }

    return PluginMediaEntity(
        id = id,
        pluginId = pluginId,
        pluginName = pluginName,
        title = title,
        posterUrl = posterUrl,
        bannerUrl = bannerUrl,
        plot = plot,
        year = year,
        rating = rating,
        category = category,
        type = type.name,
        streamUrl = streamUrl,
        dataUrl = dataUrl,
        episodesJson = if (epArr.length() > 0) epArr.toString() else null,
        headersJson = if (hObj.length() > 0) hObj.toString() else null
    )
}
