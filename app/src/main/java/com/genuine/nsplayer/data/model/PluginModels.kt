package com.genuine.nsplayer.data.model

enum class PluginCapability(val label: String) {
    LIVE_TV("Live TV"),
    MOVIES("Movies"),
    SERIES("TV Series"),
    EPISODES("Episodes"),
    ANIME("Anime"),
    SEARCH("Search"),
    STREAM_EXTRACTOR("Stream Extractor");

    companion object {
        fun fromString(str: String): PluginCapability? {
            return values().firstOrNull { 
                it.name.equals(str, ignoreCase = true) || it.label.equals(str, ignoreCase = true) 
            }
        }
    }
}

enum class PluginStatus {
    AVAILABLE,
    INSTALLED,
    DISABLED,
    UPDATE_AVAILABLE
}

data class PluginItem(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Int,
    val author: String,
    val description: String,
    val iconUrl: String? = null,
    val packageUrl: String? = null,
    val repositoryUrl: String? = null,
    val capabilities: List<PluginCapability> = emptyList(),
    val status: PluginStatus = PluginStatus.AVAILABLE,
    val fileSize: Long = 0L,
    val sha256: String? = null,
    val isEnabled: Boolean = true
)

data class PluginRepositoryItem(
    val id: Long = 0,
    val name: String,
    val url: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val pluginCount: Int = 0,
    val isEnabled: Boolean = true
)

data class PluginMediaItem(
    val id: String,
    val pluginId: String,
    val pluginName: String = "",
    val title: String,
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val plot: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val category: String = "Catalog",
    val type: ContentType = ContentType.MOVIE,
    val streamUrl: String? = null,
    val dataUrl: String? = null,
    val episodes: List<PluginEpisode> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val drmScheme: String? = null,
    val drmLicense: String? = null
)

data class PluginEpisode(
    val id: String,
    val title: String,
    val number: Int,
    val season: Int = 1,
    val duration: String? = null,
    val coverImage: String? = null
)
