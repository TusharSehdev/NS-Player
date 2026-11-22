package com.genuine.nsplayer.data.repository

import android.content.Context
import android.util.Log
import com.genuine.nsplayer.NSPlayerApplication
import com.genuine.nsplayer.data.local.PluginDao
import com.genuine.nsplayer.data.local.PluginMediaEntity
import com.genuine.nsplayer.data.local.toModel
import com.genuine.nsplayer.data.model.ContentType
import com.genuine.nsplayer.data.model.PluginEpisode
import com.genuine.nsplayer.data.model.PluginItem
import com.genuine.nsplayer.data.model.PluginMediaItem
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile

class UniversalPluginLoader(
    private val context: Context? = null,
    private val pluginDao: PluginDao,
    private val client: OkHttpClient = OkHttpClient()
) {
    companion object {
        private const val TAG = "UniversalPluginLoader"
        private val loadedApis = ConcurrentHashMap<String, MainAPI>()
        private val loadedPlugins = ConcurrentHashMap<String, Plugin>()
    }

    private fun getContext(): Context {
        return context ?: runCatching { NSPlayerApplication.instance }.getOrNull()
            ?: throw IllegalStateException("Application Context not available for plugin loading")
    }

    /**
     * Loads the plugin DEX and instantiates its MainAPI.
     */
    suspend fun loadPluginApi(plugin: PluginItem): MainAPI? = withContext(Dispatchers.IO) {
        val pluginId = plugin.id
        loadedApis[pluginId]?.let { return@withContext it }

        val ctx = try { getContext() } catch (e: Exception) { return@withContext null }
        val packageUrl = plugin.packageUrl ?: return@withContext null

        try {
            val pluginsDir = File(ctx.filesDir, "plugins").apply { mkdirs() }
            val pluginFolder = File(pluginsDir, pluginId).apply { mkdirs() }
            val cs3File = File(pluginFolder, "$pluginId.cs3")
            val dexFile = File(pluginFolder, "classes.dex")
            val manifestFile = File(pluginFolder, "manifest.json")
            val codeCacheDir = File(pluginFolder, "code_cache").apply { mkdirs() }

            // Download .cs3 if not present or empty
            if (!cs3File.exists() || cs3File.length() == 0L || !dexFile.exists()) {
                val encodedUrl = packageUrl.replace(" ", "%20")
                Log.d(TAG, "Downloading plugin $pluginId from $encodedUrl")
                val request = Request.Builder()
                    .url(encodedUrl)
                    .header("User-Agent", "NEXORA/1.0 (Android TV & Mobile)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to download $pluginId: HTTP ${response.code}")
                        return@withContext null
                    }
                    val bodyStream = response.body?.byteStream() ?: return@withContext null
                    FileOutputStream(cs3File).use { out ->
                        bodyStream.copyTo(out)
                    }
                }

                // Extract classes.dex and manifest.json from .cs3 (which is a ZIP archive)
                ZipFile(cs3File).use { zip ->
                    val dexEntry = zip.getEntry("classes.dex")
                    if (dexEntry != null) {
                        zip.getInputStream(dexEntry).use { input ->
                            FileOutputStream(dexFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    val manifestEntry = zip.getEntry("manifest.json")
                    if (manifestEntry != null) {
                        zip.getInputStream(manifestEntry).use { input ->
                            FileOutputStream(manifestFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }

            if (!dexFile.exists()) {
                Log.e(TAG, "classes.dex not found for plugin $pluginId")
                return@withContext null
            }

            // Read pluginClassName from manifest.json
            var pluginClassName: String? = null
            if (manifestFile.exists()) {
                val manifestJson = manifestFile.readText()
                val json = JSONObject(manifestJson)
                pluginClassName = json.optString("pluginClassName").takeIf { it.isNotBlank() }
            }

            // Create DexClassLoader with current classloader as parent
            val classLoader = DexClassLoader(
                dexFile.absolutePath,
                codeCacheDir.absolutePath,
                null,
                ctx.classLoader
            )

            var api: MainAPI? = null

            // Strategy A: Load using manifest pluginClassName
            if (!pluginClassName.isNullOrBlank()) {
                try {
                    val clazz = classLoader.loadClass(pluginClassName)
                    if (Plugin::class.java.isAssignableFrom(clazz)) {
                        val pluginInstance = clazz.getDeclaredConstructor().newInstance() as Plugin
                        pluginInstance.load(ctx)
                        loadedPlugins[pluginId] = pluginInstance
                        api = pluginInstance.registeredApis.firstOrNull()
                    } else if (MainAPI::class.java.isAssignableFrom(clazz)) {
                        api = clazz.getDeclaredConstructor().newInstance() as MainAPI
                        api.init()
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed loading via manifest className $pluginClassName: ${e.message}")
                }
            }

            // Strategy B: If still null, try common naming conventions
            if (api == null) {
                val candidates = listOf(
                    "com.cncverse.${plugin.name}Plugin",
                    "com.cncverse.${plugin.name}",
                    "com.cncverse.${plugin.name}Provider",
                    "com.lagradost.cloudstream3.plugins.${plugin.name}Plugin"
                )
                for (cand in candidates) {
                    try {
                        val clazz = classLoader.loadClass(cand)
                        if (Plugin::class.java.isAssignableFrom(clazz)) {
                            val pluginInstance = clazz.getDeclaredConstructor().newInstance() as Plugin
                            pluginInstance.load(ctx)
                            loadedPlugins[pluginId] = pluginInstance
                            api = pluginInstance.registeredApis.firstOrNull()
                            if (api != null) break
                        } else if (MainAPI::class.java.isAssignableFrom(clazz)) {
                            api = clazz.getDeclaredConstructor().newInstance() as MainAPI
                            api.init()
                            break
                        }
                    } catch (_: Throwable) {
                    }
                }
            }

            // Strategy C: Inspect DEX entries if available
            if (api == null) {
                try {
                    @Suppress("DEPRECATION")
                    val dex = dalvik.system.DexFile(dexFile.absolutePath)
                    val entries = dex.entries()
                    while (entries.hasMoreElements()) {
                        val entryName = entries.nextElement()
                        if (entryName.endsWith("Plugin") || entryName.endsWith("Provider")) {
                            try {
                                val clazz = classLoader.loadClass(entryName)
                                if (Plugin::class.java.isAssignableFrom(clazz) && !java.lang.reflect.Modifier.isAbstract(clazz.modifiers)) {
                                    val pluginInstance = clazz.getDeclaredConstructor().newInstance() as Plugin
                                    pluginInstance.load(ctx)
                                    loadedPlugins[pluginId] = pluginInstance
                                    api = pluginInstance.registeredApis.firstOrNull()
                                    if (api != null) break
                                } else if (MainAPI::class.java.isAssignableFrom(clazz) && !java.lang.reflect.Modifier.isAbstract(clazz.modifiers)) {
                                    api = clazz.getDeclaredConstructor().newInstance() as MainAPI
                                    api.init()
                                    break
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                } catch (_: Throwable) {}
            }

            if (api != null) {
                Log.i(TAG, "Successfully loaded universal plugin: ${api.name} ($pluginId)")
                loadedApis[pluginId] = api
            } else {
                Log.w(TAG, "No MainAPI found in plugin $pluginId")
            }

            api
        } catch (e: Throwable) {
            Log.e(TAG, "Exception while loading plugin $pluginId: ${e.message}", e)
            null
        }
    }

    /**
     * Universal sync: executes plugin to fetch all catalog items (movies, series, live TV)
     * and persists them in the database.
     */
    suspend fun syncPlugin(plugin: PluginItem): Int = withContext(Dispatchers.IO) {
        val api = loadPluginApi(plugin) ?: return@withContext 0

        val mediaEntities = mutableListOf<PluginMediaEntity>()
        val pages = if (api.mainPage.isNotEmpty()) api.mainPage else listOf(MainPageData("Home", ""))

        for (pageData in pages) {
            try {
                val req = MainPageRequest(pageData.name, pageData.data, pageData.horizontalImages)
                val resp = api.getMainPage(1, req) ?: continue
                for (itemGroup in resp.items) {
                    val groupTitle = itemGroup.name.ifBlank { pageData.name.ifBlank { "Featured" } }
                    for (item in itemGroup.list) {
                        val contentType = when (item.type) {
                            TvType.Movie -> ContentType.MOVIE
                            TvType.TvSeries -> ContentType.SERIES
                            TvType.Live -> ContentType.LIVE_TV
                            else -> ContentType.MOVIE
                        }
                        val year = (item as? MovieSearchResponse)?.year
                            ?: (item as? TvSeriesSearchResponse)?.year
                            ?: (item as? AnimeSearchResponse)?.year

                        val id = "${plugin.id}_${item.url.hashCode()}"
                        mediaEntities.add(
                            PluginMediaEntity(
                                id = id,
                                pluginId = plugin.id,
                                pluginName = plugin.name,
                                title = item.name,
                                posterUrl = item.posterUrl,
                                bannerUrl = item.posterUrl,
                                plot = "${item.name} (${item.type?.name ?: "Media"})",
                                year = year,
                                rating = null,
                                category = groupTitle,
                                type = contentType.name,
                                streamUrl = item.url,
                                dataUrl = item.url
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error fetching main page ${pageData.name} for ${plugin.name}: ${e.message}")
            }
        }

        // If mainPage didn't produce results, try search discovery
        if (mediaEntities.isEmpty()) {
            val searchQueries = listOf("", "2024", "live", "movie", "show")
            for (q in searchQueries) {
                try {
                    val searchResults = api.search(q)
                    if (!searchResults.isNullOrEmpty()) {
                        for (item in searchResults) {
                            val contentType = when (item.type) {
                                TvType.Movie -> ContentType.MOVIE
                                TvType.TvSeries -> ContentType.SERIES
                                TvType.Live -> ContentType.LIVE_TV
                                else -> ContentType.MOVIE
                            }
                            val id = "${plugin.id}_${item.url.hashCode()}"
                            mediaEntities.add(
                                PluginMediaEntity(
                                    id = id,
                                    pluginId = plugin.id,
                                    pluginName = plugin.name,
                                    title = item.name,
                                    posterUrl = item.posterUrl,
                                    bannerUrl = item.posterUrl,
                                    plot = "${item.name} (${item.type?.name ?: "Media"})",
                                    year = (item as? MovieSearchResponse)?.year ?: (item as? TvSeriesSearchResponse)?.year,
                                    rating = null,
                                    category = "Catalog",
                                    type = contentType.name,
                                    streamUrl = item.url,
                                    dataUrl = item.url
                                )
                            )
                        }
                        break
                    }
                } catch (_: Throwable) {}
            }
        }

        if (mediaEntities.isNotEmpty()) {
            pluginDao.deleteMediaByPlugin(plugin.id)
            pluginDao.insertMedia(mediaEntities)
            Log.i(TAG, "Synced ${mediaEntities.size} items for plugin ${plugin.name}")
        }

        mediaEntities.size
    }

    /**
     * Universal episode fetcher.
     */
    suspend fun fetchEpisodes(media: PluginMediaItem): List<PluginEpisode> = withContext(Dispatchers.IO) {
        val plugin = pluginDao.getPluginById(media.pluginId)?.toModel() ?: return@withContext emptyList()
        val api = loadPluginApi(plugin) ?: return@withContext emptyList()
        val targetUrl = media.dataUrl ?: media.streamUrl ?: return@withContext emptyList()

        try {
            val loadRes = api.load(targetUrl) ?: return@withContext emptyList()
            when (loadRes) {
                is TvSeriesLoadResponse -> {
                    return@withContext loadRes.episodes.mapIndexed { index, ep ->
                        PluginEpisode(
                            id = ep.data,
                            title = ep.name ?: "Episode ${ep.episode ?: (index + 1)}",
                            number = ep.episode ?: (index + 1),
                            season = ep.season ?: 1,
                            duration = ep.runTime?.let { "${it}m" },
                            coverImage = ep.posterUrl ?: media.posterUrl
                        )
                    }
                }
                is AnimeLoadResponse -> {
                    val epList = loadRes.episodes[DubStatus.Subbed]
                        ?: loadRes.episodes[DubStatus.Dubbed]
                        ?: loadRes.episodes.values.firstOrNull()
                        ?: emptyList()
                    return@withContext epList.mapIndexed { index, ep ->
                        PluginEpisode(
                            id = ep.data,
                            title = ep.name ?: "Episode ${ep.episode ?: (index + 1)}",
                            number = ep.episode ?: (index + 1),
                            season = ep.season ?: 1,
                            duration = ep.runTime?.let { "${it}m" },
                            coverImage = ep.posterUrl ?: media.posterUrl
                        )
                    }
                }
                else -> emptyList()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error fetching episodes for ${media.title}: ${e.message}")
            emptyList()
        }
    }

    /**
     * Universal stream link resolver.
     */
    suspend fun resolveStream(
        media: PluginMediaItem,
        episodeId: String? = null,
        preferredQuality: String = "720"
    ): ResolvedStream? = withContext(Dispatchers.IO) {
        val plugin = pluginDao.getPluginById(media.pluginId)?.toModel() ?: return@withContext null
        val api = loadPluginApi(plugin) ?: return@withContext null

        try {
            var targetData = episodeId
            if (targetData.isNullOrBlank()) {
                val mediaUrl = media.dataUrl ?: media.streamUrl
                val loadRes = if (!mediaUrl.isNullOrBlank()) api.load(mediaUrl) else null
                targetData = when (loadRes) {
                    is MovieLoadResponse -> loadRes.dataUrl.ifBlank { mediaUrl }
                    is LiveStreamLoadResponse -> loadRes.dataUrl.ifBlank { mediaUrl }
                    is TvSeriesLoadResponse -> loadRes.episodes.firstOrNull()?.data ?: mediaUrl
                    is AnimeLoadResponse -> {
                        val firstEp = loadRes.episodes[DubStatus.Subbed]?.firstOrNull()
                            ?: loadRes.episodes[DubStatus.Dubbed]?.firstOrNull()
                            ?: loadRes.episodes.values.firstOrNull()?.firstOrNull()
                        firstEp?.data ?: mediaUrl
                    }
                    else -> mediaUrl
                }
            }

            if (targetData.isNullOrBlank()) return@withContext null

            val links = mutableListOf<ExtractorLink>()
            try {
                api.loadLinks(
                    data = targetData,
                    isCasting = false,
                    subtitleCallback = {},
                    callback = { link -> links.add(link) }
                )
            } catch (e: Throwable) {
                Log.w(TAG, "loadLinks error: ${e.message}")
            }

            if (links.isNotEmpty()) {
                val matched = links.firstOrNull { it.quality.toString().contains(preferredQuality) }
                    ?: links.maxByOrNull { it.quality }
                    ?: links.first()
                val qualities = links.map { Qualities.getStringByInt(it.quality) }.distinct()
                return@withContext ResolvedStream(
                    url = matched.url,
                    headers = matched.headers,
                    qualities = qualities
                )
            }

            // If loadLinks didn't produce an ExtractorLink but targetData is a direct playable stream URL
            if (targetData.startsWith("http://") || targetData.startsWith("https://")) {
                return@withContext ResolvedStream(url = targetData)
            }

            null
        } catch (e: Throwable) {
            Log.e(TAG, "Error resolving stream for ${media.title}: ${e.message}", e)
            null
        }
    }
}
