package com.genuine.nsplayer.data.parser

import com.genuine.nsplayer.data.model.Channel
import com.genuine.nsplayer.data.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

data class ParsedPlaylistResult(
    val epgUrl: String? = null,
    val channels: List<Channel> = emptyList()
)

object M3UParser {

    private val TVG_ID_PATTERN = Pattern.compile("tvg-id=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
    private val TVG_NAME_PATTERN = Pattern.compile("tvg-name=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
    private val TVG_LOGO_PATTERN = Pattern.compile("tvg-logo=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
    private val GROUP_TITLE_PATTERN = Pattern.compile("group-title=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
    private val TVG_COUNTRY_PATTERN = Pattern.compile("tvg-country=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
    private val TVG_LANG_PATTERN = Pattern.compile("tvg-language=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
    private val URL_TVG_PATTERN = Pattern.compile("(?:url-tvg|x-tvg-url)=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)

    /**
     * Parses an M3U stream asynchronously and emits channel batches of [batchSize] to prevent OOM
     * and allow reactive progress reporting when processing 10,000+ channels.
     */
    fun parseStreamInBatches(
        inputStream: InputStream,
        playlistId: Long,
        defaultUserAgent: String? = null,
        batchSize: Int = 500
    ): Flow<List<Channel>> = flow {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        var currentBatch = mutableListOf<Channel>()
        var order = 0

        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentTvgLogo: String? = null
        var currentGroupTitle = "Uncategorized"
        var currentCountry: String? = null
        var currentLanguage: String? = null
        var currentChannelName = ""
        var currentHeaders = mutableMapOf<String, String>()
        defaultUserAgent?.let { currentHeaders["User-Agent"] = it }

        reader.useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue

                if (line.startsWith("#EXTM3U")) {
                    // Extract playlist-level EPG url if present
                    continue
                }

                if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    // Extract attributes
                    currentTvgId = extractMatch(TVG_ID_PATTERN, line)
                    currentTvgName = extractMatch(TVG_NAME_PATTERN, line)
                    currentTvgLogo = extractMatch(TVG_LOGO_PATTERN, line)
                    currentGroupTitle = extractMatch(GROUP_TITLE_PATTERN, line) ?: "Uncategorized"
                    currentCountry = extractMatch(TVG_COUNTRY_PATTERN, line)
                    currentLanguage = extractMatch(TVG_LANG_PATTERN, line)

                    // Channel display name is everything after the last comma
                    val commaIndex = line.lastIndexOf(',')
                    currentChannelName = if (commaIndex != -1 && commaIndex < line.length - 1) {
                        line.substring(commaIndex + 1).trim()
                    } else {
                        currentTvgName ?: "Channel ${order + 1}"
                    }
                } else if (line.startsWith("#EXTVLCOPT:", ignoreCase = true)) {
                    val opt = line.substring(11).trim()
                    if (opt.startsWith("http-user-agent=", ignoreCase = true)) {
                        currentHeaders["User-Agent"] = opt.substring(16).trim()
                    } else if (opt.startsWith("http-referrer=", ignoreCase = true)) {
                        currentHeaders["Referer"] = opt.substring(14).trim()
                    }
                } else if (line.startsWith("#EXTHTTP:", ignoreCase = true)) {
                    // Custom HTTP headers JSON
                    val json = line.substring(9).trim()
                    parseSimpleHeadersJson(json, currentHeaders)
                } else if (!line.startsWith("#")) {
                    // This is a stream URL!
                    var streamUrl = line
                    var drmScheme: String? = null
                    var drmLicense: String? = null

                    // Parse pipe syntax: url|User-Agent=...&Referer=...
                    val pipeIndex = streamUrl.indexOf('|')
                    if (pipeIndex != -1) {
                        val headersPart = streamUrl.substring(pipeIndex + 1)
                        streamUrl = streamUrl.substring(0, pipeIndex)
                        headersPart.split('&').forEach { param ->
                            val kv = param.split('=', limit = 2)
                            if (kv.size == 2) {
                                val key = kv[0].trim()
                                val value = kv[1].trim()
                                when (key.lowercase()) {
                                    "user-agent" -> currentHeaders["User-Agent"] = value
                                    "referer" -> currentHeaders["Referer"] = value
                                    "origin" -> currentHeaders["Origin"] = value
                                    "cookie" -> currentHeaders["Cookie"] = value
                                    "drmscheme" -> drmScheme = value
                                    "drmlicense" -> drmLicense = value
                                }
                            }
                        }
                    }

                    val contentType = determineContentType(currentGroupTitle, streamUrl)

                    val channel = Channel(
                        id = 0,
                        playlistId = playlistId,
                        name = currentChannelName.ifEmpty { "Channel ${order + 1}" },
                        streamUrl = streamUrl,
                        logoUrl = currentTvgLogo,
                        groupTitle = currentGroupTitle,
                        tvgId = currentTvgId,
                        tvgName = currentTvgName,
                        tvgCountry = currentCountry,
                        tvgLanguage = currentLanguage,
                        contentType = contentType,
                        headers = HashMap(currentHeaders),
                        drmScheme = drmScheme,
                        drmLicense = drmLicense,
                        orderIndex = order++
                    )

                    currentBatch.add(channel)

                    if (currentBatch.size >= batchSize) {
                        emit(currentBatch)
                        currentBatch = mutableListOf()
                    }

                    // Reset per-channel metadata
                    currentTvgId = null
                    currentTvgName = null
                    currentTvgLogo = null
                    currentGroupTitle = "Uncategorized"
                    currentCountry = null
                    currentLanguage = null
                    currentChannelName = ""
                    currentHeaders = mutableMapOf()
                    defaultUserAgent?.let { currentHeaders["User-Agent"] = it }
                }
            }

            if (currentBatch.isNotEmpty()) {
                emit(currentBatch)
            }
        }
    }.flowOn(Dispatchers.IO)

    fun extractEpgUrl(inputStream: InputStream): String? {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        reader.useLines { lines ->
            for (line in lines.take(15)) {
                if (line.startsWith("#EXTM3U", ignoreCase = true)) {
                    val m = URL_TVG_PATTERN.matcher(line)
                    if (m.find()) {
                        return m.group(1)?.split(',')?.firstOrNull()?.trim()
                    }
                }
            }
        }
        return null
    }

    private fun extractMatch(pattern: Pattern, text: String): String? {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    private fun determineContentType(group: String, url: String): ContentType {
        val g = group.lowercase()
        val u = url.lowercase()

        return when {
            g.contains("movie") || g.contains("vod") || g.contains("cinema") || g.contains("film") ||
                    u.contains("/movie/") || u.endsWith(".mp4") || u.endsWith(".mkv") -> ContentType.MOVIE

            g.contains("series") || g.contains("show") || g.contains("season") || g.contains("episode") ||
                    u.contains("/series/") -> ContentType.SERIES

            else -> ContentType.LIVE_TV
        }
    }

    private fun parseSimpleHeadersJson(json: String, headers: MutableMap<String, String>) {
        // Simple regex parser for JSON {"User-Agent": "...", "Referer": "..."}
        val matcher = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"").matcher(json)
        while (matcher.find()) {
            val key = matcher.group(1) ?: continue
            val value = matcher.group(2) ?: continue
            headers[key] = value
        }
    }
}
