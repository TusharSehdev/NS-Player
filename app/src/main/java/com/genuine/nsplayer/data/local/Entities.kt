package com.genuine.nsplayer.data.local

import androidx.room.*

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String?,
    val filePath: String?,
    val userAgent: String?,
    val channelCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true
)

@Entity(
    tableName = "channels",
    indices = [
        Index("playlistId"),
        Index("groupTitle"),
        Index("isFavorite"),
        Index("contentType")
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val groupTitle: String,
    val tvgId: String?,
    val tvgName: String?,
    val tvgCountry: String?,
    val tvgLanguage: String?,
    val contentType: String, // LIVE_TV, MOVIE, SERIES
    val headersJson: String, // JSON serialized Map<String, String>
    val drmScheme: String?,
    val drmLicense: String?,
    val isFavorite: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "playback_history",
    indices = [Index("streamUrl", unique = true)]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Long?,
    val streamUrl: String,
    val title: String,
    val logoUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "epg_programs",
    indices = [
        Index("channelTvgId"),
        Index("startEpoch"),
        Index("endEpoch")
    ]
)
data class EpgEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelTvgId: String,
    val title: String,
    val description: String?,
    val startEpoch: Long,
    val endEpoch: Long,
    val category: String?
)
