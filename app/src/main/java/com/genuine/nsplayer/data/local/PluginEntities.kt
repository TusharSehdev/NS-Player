package com.genuine.nsplayer.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.genuine.nsplayer.data.model.PluginCapability
import com.genuine.nsplayer.data.model.PluginItem
import com.genuine.nsplayer.data.model.PluginRepositoryItem
import com.genuine.nsplayer.data.model.PluginStatus
import org.json.JSONArray

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
