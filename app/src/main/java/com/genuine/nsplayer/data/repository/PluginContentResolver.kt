package com.genuine.nsplayer.data.repository

import android.util.Log
import com.genuine.nsplayer.data.local.PluginDao
import com.genuine.nsplayer.data.local.PluginEntity
import com.genuine.nsplayer.data.local.PluginMediaEntity
import com.genuine.nsplayer.data.local.toModel
import com.genuine.nsplayer.data.model.ContentType
import com.genuine.nsplayer.data.model.PluginEpisode
import com.genuine.nsplayer.data.model.PluginItem
import com.genuine.nsplayer.data.model.PluginMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class ResolvedStream(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val qualities: List<String> = emptyList()
)

object SafeBase64 {
    fun decode(input: String): ByteArray {
        val clean = input.trim().replace("\n", "").replace("\r", "")
        return try {
            android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(clean)
        }
    }
}

class PluginContentResolver(
    private val pluginDao: PluginDao,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    companion object {
        private const val TAG = "PluginContentResolver"
        const val CASTLE_BASE_URL = "https://api.hlowb.com"
        const val CLOWN_PLAYLIST_URL = "https://sxcce.dpdns.org/playlist.m3u"

        /**
         * Converts a Castle preview m3u8 link (e.g. `img1.hscow.com/hls_mps/.../480/index_preview_287.m3u8?Expires=...`)
         * to a direct master stream link (`img.hscow.com/hls_mps/.../480/index.m3u8`).
         */
        fun transformCastleStreamUrl(url: String, quality: String = "720"): String {
            // 1. Remove query parameters
            var clean = url.substringBefore("?")

            // 2. Replace img1. with img.
            clean = clean.replace(Regex("^(https?://)img1\\."), "$1img.")

            // 3. Remove preview file name and replace with index.m3u8
            val baseDir = clean.substringBeforeLast('/')
            clean = "$baseDir/index.m3u8"

            // 4. If desired quality is 720 or 1080 and currently on 480:
            val targetRes = when (quality) {
                "1080", "1080p" -> "1080"
                "720", "720p" -> "720"
                else -> "480"
            }

            clean = clean.replace(Regex("/(?:480|720|1080)/index\\.m3u8$"), "/$targetRes/index.m3u8")
            return clean
        }
    }

    /**
     * Synchronizes media catalog from an installed plugin and saves into Room database.
     */
    suspend fun syncPlugin(plugin: PluginItem): Int = withContext(Dispatchers.IO) {
        val pluginId = plugin.id.lowercase()
        val pluginName = plugin.name.lowercase()

        val count = when {
            pluginId.contains("castle") || pluginName.contains("castle") -> {
                syncCastleTv(plugin)
            }
            pluginId.contains("clown") || pluginName.contains("clown") -> {
                syncClownIptv(plugin)
            }
            else -> {
                syncGenericPlugin(plugin)
            }
        }

        Log.d(TAG, "Synced $count media items for plugin: ${plugin.name} (${plugin.id})")
        count
    }

    /**
     * Resolves playable streaming link for a media item.
     */
    suspend fun resolveStream(
        media: PluginMediaItem,
        episodeId: String? = null,
        preferredQuality: String = "720"
    ): ResolvedStream = withContext(Dispatchers.IO) {
        val pluginId = media.pluginId.lowercase()

        if (pluginId.contains("castle")) {
            resolveCastleTvStream(media, episodeId, preferredQuality)
        } else if (!media.streamUrl.isNullOrBlank()) {
            ResolvedStream(url = media.streamUrl, headers = media.headers)
        } else {
            throw Exception("No stream URL available for ${media.title}")
        }
    }

    /**
     * Fetches episode list for series.
     */
    suspend fun fetchEpisodes(media: PluginMediaItem): List<PluginEpisode> = withContext(Dispatchers.IO) {
        val pluginId = media.pluginId.lowercase()

        if (pluginId.contains("castle")) {
            fetchCastleTvEpisodes(media)
        } else {
            media.episodes
        }
    }

    // =========================================================================
    // Castle TV Provider Implementation
    // =========================================================================

    private suspend fun syncCastleTv(plugin: PluginItem): Int {
        val key = getCastleSecurityKey()
        val homeUrl = "$CASTLE_BASE_URL/film-api/v0.1/category/home?channel=IndiaA&clientType=1&lang=en-US&locationId=1001&mode=1&packageName=com.external.castle&page=1"
        val req = Request.Builder()
            .url(homeUrl)
            .header("User-Agent", "okhttp/4.12.0")
            .header("Referer", "$CASTLE_BASE_URL/")
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw Exception("Castle home HTTP ${resp.code}")
        val encBody = resp.body?.string() ?: throw Exception("Castle home empty")

        val plainJson = decryptAes128Cbc(encBody, key)
        val root = JSONObject(plainJson)
        val dataObj = root.optJSONObject("data") ?: root
        val rows = dataObj.optJSONArray("rows") ?: dataObj.optJSONArray("homePageLists") ?: JSONArray()

        val mediaEntities = mutableListOf<PluginMediaEntity>()

        for (i in 0 until rows.length()) {
            val rowObj = rows.optJSONObject(i) ?: continue
            val rowName = rowObj.optString("name").ifBlank { "Featured" }
            val contents = rowObj.optJSONArray("contents") ?: continue

            for (j in 0 until contents.length()) {
                val item = contents.optJSONObject(j) ?: continue
                val redirectId = item.optLong("redirectId", 0L)
                val idStr = if (redirectId != 0L) redirectId.toString() else item.optString("id")
                if (idStr.isBlank()) continue

                val title = item.optString("title")
                val coverImage = item.optString("coverImage").ifBlank { item.optString("coverVerticalImage") }
                val score = item.optDouble("score", 0.0).takeIf { it > 0 }
                val movieType = item.optInt("movieType", 2) // 1 = Series, 2 = Movie
                val typeStr = if (movieType == 1) ContentType.SERIES.name else ContentType.MOVIE.name

                mediaEntities.add(
                    PluginMediaEntity(
                        id = "castle_$idStr",
                        pluginId = plugin.id,
                        pluginName = plugin.name,
                        title = title,
                        posterUrl = coverImage.takeIf { it.isNotBlank() },
                        bannerUrl = coverImage.takeIf { it.isNotBlank() },
                        plot = item.optString("briefIntroduction").takeIf { it.isNotBlank() },
                        year = null,
                        rating = score,
                        category = rowName,
                        type = typeStr,
                        streamUrl = null, // Lazily resolved on playback
                        dataUrl = idStr
                    )
                )
            }
        }

        if (mediaEntities.isNotEmpty()) {
            pluginDao.deleteMediaByPlugin(plugin.id)
            pluginDao.insertMedia(mediaEntities)
        }

        return mediaEntities.size
    }

    private suspend fun fetchCastleTvEpisodes(media: PluginMediaItem): List<PluginEpisode> {
        val movieId = media.dataUrl ?: media.id.removePrefix("castle_")
        val key = getCastleSecurityKey()
        val detailsUrl = "$CASTLE_BASE_URL/film-api/v1.9.9/movie?channel=IndiaA&clientType=1&lang=en-US&movieId=$movieId&packageName=com.external.castle"

        val req = Request.Builder()
            .url(detailsUrl)
            .header("User-Agent", "okhttp/4.12.0")
            .header("Referer", "$CASTLE_BASE_URL/")
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) return emptyList()
        val enc = resp.body?.string() ?: return emptyList()

        val plain = decryptAes128Cbc(enc, key)
        val data = JSONObject(plain).optJSONObject("data") ?: return emptyList()
        val epArr = data.optJSONArray("episodes") ?: return emptyList()

        val list = mutableListOf<PluginEpisode>()
        for (i in 0 until epArr.length()) {
            val ep = epArr.optJSONObject(i) ?: continue
            val epId = ep.optLong("id", 0L).let { if (it != 0L) it.toString() else ep.optString("id") }
            val epTitle = ep.optString("title").ifBlank { "Episode ${i + 1}" }
            list.add(
                PluginEpisode(
                    id = epId,
                    title = epTitle,
                    number = ep.optInt("number", i + 1),
                    duration = ep.optString("duration").takeIf { it.isNotBlank() },
                    coverImage = ep.optString("coverImage").takeIf { it.isNotBlank() }
                )
            )
        }
        return list
    }

    private suspend fun resolveCastleTvStream(
        media: PluginMediaItem,
        episodeId: String?,
        preferredQuality: String
    ): ResolvedStream {
        val movieId = media.dataUrl ?: media.id.removePrefix("castle_")
        val key = getCastleSecurityKey()

        // If episodeId is null, fetch movie details to get the first episode
        var targetEpId = episodeId
        if (targetEpId.isNullOrBlank()) {
            val eps = fetchCastleTvEpisodes(media)
            if (eps.isNotEmpty()) {
                targetEpId = eps.first().id
            }
        }

        val video2Url = "$CASTLE_BASE_URL/film-api/v2.0.1/movie/getVideo2?clientType=1&packageName=com.external.castle&channel=IndiaA&lang=en-US"
        val jsonBody = JSONObject().apply {
            put("mode", "1")
            put("appMarket", "GuanWang")
            put("clientType", "1")
            put("woolUser", "false")
            put("apkSignKey", "ED0955EB04E67A1D9F3305B95454FED485261475")
            put("androidVersion", "13")
            put("movieId", movieId)
            put("episodeId", targetEpId ?: "")
            put("isNewUser", "true")
            put("resolution", preferredQuality)
            put("packageName", "com.external.castle")
        }

        val reqBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url(video2Url)
            .post(reqBody)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("User-Agent", "okhttp/4.12.0")
            .header("Referer", "$CASTLE_BASE_URL/")
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw Exception("Castle getVideo2 HTTP ${resp.code}")
        val enc = resp.body?.string() ?: throw Exception("Castle getVideo2 empty")

        val plain = decryptAes128Cbc(enc, key)
        val respObj = JSONObject(plain)
        val data = respObj.optJSONObject("data") ?: throw Exception("Castle stream not found: ${respObj.optString("msg")}")
        val rawVideoUrl = data.optString("videoUrl")
        if (rawVideoUrl.isBlank()) throw Exception("Castle empty videoUrl")

        // Transform preview URL to full stream URL via freeStreamUrls algorithm
        val cleanUrl = transformCastleStreamUrl(rawVideoUrl, preferredQuality)

        val headers = mapOf(
            "User-Agent" to "okhttp/4.12.0",
            "Referer" to "$CASTLE_BASE_URL/"
        )

        return ResolvedStream(
            url = cleanUrl,
            headers = headers,
            qualities = listOf("1080p", "720p", "480p")
        )
    }

    private suspend fun getCastleSecurityKey(): ByteArray {
        val secUrl = "$CASTLE_BASE_URL/v0.1/system/getSecurityKey/1?channel=IndiaA&clientType=1&lang=en-US"
        val req = Request.Builder()
            .url(secUrl)
            .header("User-Agent", "okhttp/4.12.0")
            .header("Referer", "$CASTLE_BASE_URL/")
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw Exception("Castle secKey HTTP ${resp.code}")
        val body = resp.body?.string() ?: throw Exception("Castle secKey empty")
        val json = JSONObject(body)
        val b64Key = json.optString("data")
        if (b64Key.isBlank()) throw Exception("Castle missing security key")

        // Derivation: Base64 decode + "T!BgJB" -> trim or pad to 16 bytes
        val keyBytes = SafeBase64.decode(b64Key)
        val suffix = "T!BgJB".toByteArray(StandardCharsets.US_ASCII)
        val combined = keyBytes + suffix
        return when {
            combined.size < 16 -> combined.copyOf(16)
            combined.size > 16 -> combined.copyOfRange(0, 16)
            else -> combined
        }
    }

    private fun decryptAes128Cbc(encryptedBase64: String, key: ByteArray): String {
        val rawEncrypted = SafeBase64.decode(encryptedBase64)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(key))
        val decryptedBytes = cipher.doFinal(rawEncrypted)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    // =========================================================================
    // Clown IPTV Provider Implementation
    // =========================================================================

    private suspend fun syncClownIptv(plugin: PluginItem): Int {
        val req = Request.Builder()
            .url(CLOWN_PLAYLIST_URL)
            .header("User-Agent", "okhttp/4.12.0")
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw Exception("Clown IPTV HTTP ${resp.code}")
        val body = resp.body?.string() ?: throw Exception("Clown IPTV empty")

        val mediaEntities = mutableListOf<PluginMediaEntity>()
        val lines = body.lineSequence()

        var currentTitle: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null
        var count = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                // Parse channel name
                currentTitle = trimmed.substringAfterLast(",").trim()

                // Parse tvg-logo
                val logoMatch = Regex("""tvg-logo="([^"]+)"""").find(trimmed)
                currentLogo = logoMatch?.groupValues?.get(1)

                // Parse group-title
                val groupMatch = Regex("""group-title="([^"]+)"""").find(trimmed)
                currentGroup = groupMatch?.groupValues?.get(1)?.ifBlank { "Live TV" } ?: "Live TV"
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                if (!currentTitle.isNullOrBlank()) {
                    count++
                    mediaEntities.add(
                        PluginMediaEntity(
                            id = "clown_${count}_${trimmed.hashCode()}",
                            pluginId = plugin.id,
                            pluginName = plugin.name,
                            title = currentTitle,
                            posterUrl = currentLogo,
                            bannerUrl = currentLogo,
                            plot = "Live TV Channel from ${plugin.name}",
                            year = null,
                            rating = null,
                            category = currentGroup ?: "Live TV",
                            type = ContentType.LIVE_TV.name,
                            streamUrl = trimmed,
                            dataUrl = trimmed
                        )
                    )
                }
                currentTitle = null
                currentLogo = null
                currentGroup = null

                // Limit initial sync to 500 channels for fast performance
                if (count >= 500) break
            }
        }

        if (mediaEntities.isNotEmpty()) {
            pluginDao.deleteMediaByPlugin(plugin.id)
            pluginDao.insertMedia(mediaEntities)
        }

        return mediaEntities.size
    }

    // =========================================================================
    // Generic CloudStream / M3U Extension Fallback
    // =========================================================================

    private suspend fun syncGenericPlugin(plugin: PluginItem): Int {
        // If plugin has packageUrl or .m3u link in description or package
        val url = plugin.packageUrl ?: return 0
        if (url.endsWith(".m3u", ignoreCase = true) || url.endsWith(".m3u8", ignoreCase = true)) {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val text = resp.body?.string() ?: ""
                val mediaEntities = mutableListOf<PluginMediaEntity>()
                var count = 0
                var currentTitle: String? = null
                var currentLogo: String? = null

                for (line in text.lineSequence()) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#EXTINF:")) {
                        currentTitle = trimmed.substringAfterLast(",").trim()
                        val logoMatch = Regex("""tvg-logo="([^"]+)"""").find(trimmed)
                        currentLogo = logoMatch?.groupValues?.get(1)
                    } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                        if (!currentTitle.isNullOrBlank()) {
                            count++
                            mediaEntities.add(
                                PluginMediaEntity(
                                    id = "${plugin.id}_$count",
                                    pluginId = plugin.id,
                                    pluginName = plugin.name,
                                    title = currentTitle,
                                    posterUrl = currentLogo,
                                    bannerUrl = currentLogo,
                                    plot = null,
                                    year = null,
                                    rating = null,
                                    category = "Live Channels",
                                    type = ContentType.LIVE_TV.name,
                                    streamUrl = trimmed,
                                    dataUrl = trimmed
                                )
                            )
                        }
                        currentTitle = null
                        currentLogo = null
                        if (count >= 300) break
                    }
                }
                if (mediaEntities.isNotEmpty()) {
                    pluginDao.deleteMediaByPlugin(plugin.id)
                    pluginDao.insertMedia(mediaEntities)
                }
                return mediaEntities.size
            }
        }
        return 0
    }
}
