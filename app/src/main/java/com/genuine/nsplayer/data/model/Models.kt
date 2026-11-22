package com.genuine.nsplayer.data.model

enum class ContentType {
    LIVE_TV,
    MOVIE,
    SERIES
}

data class Channel(
    val id: Long = 0,
    val playlistId: Long,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = "Uncategorized",
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgCountry: String? = null,
    val tvgLanguage: String? = null,
    val contentType: ContentType = ContentType.LIVE_TV,
    val headers: Map<String, String> = emptyMap(),
    val drmScheme: String? = null,
    val drmLicense: String? = null,
    val isFavorite: Boolean = false,
    val orderIndex: Int = 0
)

data class Playlist(
    val id: Long = 0,
    val name: String,
    val url: String? = null,
    val filePath: String? = null,
    val userAgent: String? = null,
    val channelCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true
)

data class PlaybackHistory(
    val id: Long = 0,
    val channelId: Long? = null,
    val streamUrl: String,
    val title: String,
    val logoUrl: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class EpgProgram(
    val id: Long = 0,
    val channelTvgId: String,
    val title: String,
    val description: String? = null,
    val startEpoch: Long,
    val endEpoch: Long,
    val category: String? = null
)
