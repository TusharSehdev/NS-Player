package com.genuine.nsplayer.data.repository

import android.content.Context
import android.net.Uri
import com.genuine.nsplayer.data.local.*
import com.genuine.nsplayer.data.model.*
import com.genuine.nsplayer.data.parser.EPGParser
import com.genuine.nsplayer.data.parser.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

class PlaylistRepository(
    private val context: Context,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val epgDao: EpgDao
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { list ->
        list.map { it.toModel() }
    }

    suspend fun addOrUpdatePlaylist(
        name: String,
        url: String?,
        filePath: String?,
        userAgent: String?,
        onProgress: (Int) -> Unit = {}
    ): Long = withContext(Dispatchers.IO) {
        val entity = PlaylistEntity(
            name = name,
            url = url,
            filePath = filePath,
            userAgent = userAgent
        )
        val playlistId = playlistDao.insertPlaylist(entity)

        // Parse and populate channels
        refreshPlaylistChannels(playlistId, onProgress)
        playlistId
    }

    suspend fun refreshPlaylistChannels(
        playlistId: Long,
        onProgress: (Int) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return@withContext

        // Clear existing channels for this playlist
        channelDao.deleteChannelsByPlaylist(playlistId)

        var totalChannels = 0
        val inputStream: InputStream? = when {
            !playlist.url.isNullOrBlank() -> {
                val reqBuilder = Request.Builder().url(playlist.url)
                if (!playlist.userAgent.isNullOrBlank()) {
                    reqBuilder.header("User-Agent", playlist.userAgent)
                }
                val resp = okHttpClient.newCall(reqBuilder.build()).execute()
                if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}: ${resp.message}")
                resp.body?.byteStream()
            }
            !playlist.filePath.isNullOrBlank() -> {
                if (playlist.filePath.startsWith("content://")) {
                    context.contentResolver.openInputStream(Uri.parse(playlist.filePath))
                } else {
                    FileInputStream(File(playlist.filePath))
                }
            }
            else -> null
        }

        inputStream?.use { stream ->
            M3UParser.parseStreamInBatches(
                inputStream = stream,
                playlistId = playlistId,
                defaultUserAgent = playlist.userAgent
            ).collect { batch ->
                val entities = batch.map { it.toEntity() }
                channelDao.insertChannels(entities)
                totalChannels += batch.size
                onProgress(totalChannels)
            }
        }

        playlistDao.updateChannelCount(playlistId, totalChannels)
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        channelDao.deleteChannelsByPlaylist(id)
        playlistDao.deletePlaylist(id)
    }

    suspend fun importEpg(epgUrl: String) = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(epgUrl).build()
            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext

            val bodyStream = resp.body?.byteStream() ?: return@withContext
            val stream = if (epgUrl.endsWith(".gz", ignoreCase = true)) {
                GZIPInputStream(bodyStream)
            } else {
                bodyStream
            }

            stream.use { s ->
                EPGParser.parseEpgStream(s).collect { batch ->
                    val entities = batch.map { it.toEntity() }
                    epgDao.insertPrograms(entities)
                }
            }
            // Prune expired programs older than 24 hours
            val cutoff = System.currentTimeMillis() - 86400000L
            epgDao.pruneOldPrograms(cutoff)
        } catch (_: Exception) {
        }
    }
}

class ChannelRepository(
    private val channelDao: ChannelDao,
    private val epgDao: EpgDao
) {
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<Channel>> =
        channelDao.getChannelsByPlaylist(playlistId).map { list -> list.map { it.toModel() } }

    fun getChannelsByGroup(playlistId: Long, group: String): Flow<List<Channel>> =
        channelDao.getChannelsByGroup(playlistId, group).map { list -> list.map { it.toModel() } }

    fun getChannelsByType(playlistId: Long, type: ContentType): Flow<List<Channel>> =
        channelDao.getChannelsByType(playlistId, type.name).map { list -> list.map { it.toModel() } }

    fun getCategories(playlistId: Long): Flow<List<String>> =
        channelDao.getCategories(playlistId)

    fun getFavoriteChannels(): Flow<List<Channel>> =
        channelDao.getFavoriteChannels().map { list -> list.map { it.toModel() } }

    fun getFavoriteChannelsByPlaylist(playlistId: Long): Flow<List<Channel>> =
        channelDao.getFavoriteChannelsByPlaylist(playlistId).map { list -> list.map { it.toModel() } }

    fun searchChannels(query: String): Flow<List<Channel>> =
        channelDao.searchChannels(query).map { list -> list.map { it.toModel() } }

    suspend fun getChannelById(id: Long): Channel? =
        channelDao.getChannelById(id)?.toModel()

    suspend fun toggleFavorite(id: Long, isFav: Boolean) = withContext(Dispatchers.IO) {
        channelDao.setFavorite(id, isFav)
    }

    suspend fun getCurrentProgram(tvgId: String?): EpgProgram? = withContext(Dispatchers.IO) {
        if (tvgId.isNullOrBlank()) return@withContext null
        epgDao.getCurrentProgram(tvgId, System.currentTimeMillis())?.toModel()
    }
}

class HistoryRepository(
    private val historyDao: HistoryDao
) {
    fun getHistory(): Flow<List<PlaybackHistory>> =
        historyDao.getHistory().map { list -> list.map { it.toModel() } }

    fun getContinueWatching(): Flow<List<PlaybackHistory>> =
        historyDao.getContinueWatching().map { list -> list.map { it.toModel() } }

    suspend fun recordHistory(
        channelId: Long?,
        streamUrl: String,
        title: String,
        logoUrl: String?,
        positionMs: Long,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        historyDao.insertOrUpdate(
            HistoryEntity(
                channelId = channelId,
                streamUrl = streamUrl,
                title = title,
                logoUrl = logoUrl,
                positionMs = positionMs,
                durationMs = durationMs,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearHistory()
    }
}

// Extensions for mapping between Entity and Domain models
fun PlaylistEntity.toModel() = Playlist(
    id = id,
    name = name,
    url = url,
    filePath = filePath,
    userAgent = userAgent,
    channelCount = channelCount,
    lastUpdated = lastUpdated,
    isEnabled = isEnabled
)

fun ChannelEntity.toModel(): Channel {
    val headersMap = mutableMapOf<String, String>()
    try {
        if (headersJson.isNotEmpty()) {
            val json = JSONObject(headersJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                headersMap[key] = json.getString(key)
            }
        }
    } catch (_: Exception) {}

    val cType = try {
        ContentType.valueOf(contentType)
    } catch (_: Exception) {
        ContentType.LIVE_TV
    }

    return Channel(
        id = id,
        playlistId = playlistId,
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        groupTitle = groupTitle,
        tvgId = tvgId,
        tvgName = tvgName,
        tvgCountry = tvgCountry,
        tvgLanguage = tvgLanguage,
        contentType = cType,
        headers = headersMap,
        drmScheme = drmScheme,
        drmLicense = drmLicense,
        isFavorite = isFavorite,
        orderIndex = orderIndex
    )
}

fun Channel.toEntity(): ChannelEntity {
    val json = JSONObject()
    headers.forEach { (k, v) -> json.put(k, v) }
    return ChannelEntity(
        id = id,
        playlistId = playlistId,
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        groupTitle = groupTitle,
        tvgId = tvgId,
        tvgName = tvgName,
        tvgCountry = tvgCountry,
        tvgLanguage = tvgLanguage,
        contentType = contentType.name,
        headersJson = json.toString(),
        drmScheme = drmScheme,
        drmLicense = drmLicense,
        isFavorite = isFavorite,
        orderIndex = orderIndex
    )
}

fun HistoryEntity.toModel() = PlaybackHistory(
    id = id,
    channelId = channelId,
    streamUrl = streamUrl,
    title = title,
    logoUrl = logoUrl,
    positionMs = positionMs,
    durationMs = durationMs,
    timestamp = timestamp
)

fun EpgEntity.toModel() = EpgProgram(
    id = id,
    channelTvgId = channelTvgId,
    title = title,
    description = description,
    startEpoch = startEpoch,
    endEpoch = endEpoch,
    category = category
)

fun EpgProgram.toEntity() = EpgEntity(
    id = id,
    channelTvgId = channelTvgId,
    title = title,
    description = description,
    startEpoch = startEpoch,
    endEpoch = endEpoch,
    category = category
)
