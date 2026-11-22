package com.lagradost.cloudstream3

import android.util.Base64
import com.fasterxml.jackson.databind.json.JsonMapper
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.serialization.json.Json

abstract class MainAPI {
    open var name: String = "Unknown Provider"
    open var mainUrl: String = ""
    open var lang: String = "en"
    open var hasMainPage: Boolean = false
    open var supportedTypes: Set<TvType> = setOf(TvType.Movie, TvType.TvSeries)
    open var mainPage: List<MainPageData> = emptyList()

    open fun init() {}

    open suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        return null
    }

    open suspend fun search(query: String): List<SearchResponse>? {
        return null
    }

    open suspend fun load(url: String): LoadResponse? {
        return null
    }

    open suspend fun loadLinks(
        data: String,
        isCasting: Boolean = false,
        subtitleCallback: (SubtitleFile) -> Unit = {},
        callback: (ExtractorLink) -> Unit = {}
    ): Boolean {
        return false
    }
}

val json: Json = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}

val mapper: JsonMapper get() = AppUtils.mapper

fun base64Encode(bytes: ByteArray): String {
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

fun base64Decode(str: String): String {
    return String(Base64.decode(str, Base64.DEFAULT))
}

fun base64DecodeArray(str: String): ByteArray {
    return Base64.decode(str, Base64.DEFAULT)
}

fun MainAPI.fixUrl(url: String): String {
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    val base = mainUrl.trimEnd('/')
    val path = url.trimStart('/')
    return "$base/$path"
}

fun MainAPI.fixUrlNull(url: String?): String? {
    if (url == null) return null
    return fixUrl(url)
}

fun mainPageOf(vararg pairs: Pair<String, String>): List<MainPageData> {
    return pairs.map { MainPageData(it.second, it.first) }
}

fun newHomePageResponse(
    name: String,
    list: List<SearchResponse>,
    hasNext: Boolean = false
): HomePageResponse {
    return HomePageResponse(listOf(HomePageList(name, list)), hasNext)
}

fun newHomePageResponse(
    list: List<HomePageList>,
    hasNext: Boolean = false
): HomePageResponse {
    return HomePageResponse(list, hasNext)
}

fun newHomePageResponse(
    item: HomePageList,
    hasNext: Boolean = false
): HomePageResponse {
    return HomePageResponse(listOf(item), hasNext)
}

fun toNewSearchResponseList(
    list: List<SearchResponse>,
    hasNext: Boolean = false
): SearchResponseList {
    return SearchResponseList(list)
}

fun MainAPI.newMovieSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.Movie,
    fix: Boolean = true,
    builder: MovieSearchResponse.() -> Unit = {}
): MovieSearchResponse {
    val res = MovieSearchResponse(name, if (fix) fixUrl(url) else url, this.name, type)
    res.builder()
    return res
}

fun MainAPI.newTvSeriesSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.TvSeries,
    fix: Boolean = true,
    builder: TvSeriesSearchResponse.() -> Unit = {}
): TvSeriesSearchResponse {
    val res = TvSeriesSearchResponse(name, if (fix) fixUrl(url) else url, this.name, type)
    res.builder()
    return res
}

fun MainAPI.newAnimeSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.Anime,
    fix: Boolean = true,
    builder: AnimeSearchResponse.() -> Unit = {}
): AnimeSearchResponse {
    val res = AnimeSearchResponse(name, if (fix) fixUrl(url) else url, this.name, type)
    res.builder()
    return res
}

fun MainAPI.newLiveSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.Live,
    fix: Boolean = true,
    builder: LiveSearchResponse.() -> Unit = {}
): LiveSearchResponse {
    val res = LiveSearchResponse(name, if (fix) fixUrl(url) else url, this.name, type)
    res.builder()
    return res
}

suspend fun MainAPI.newMovieLoadResponse(
    name: String,
    url: String,
    type: TvType = TvType.Movie,
    dataUrl: String = url,
    builder: suspend MovieLoadResponse.() -> Unit = {}
): MovieLoadResponse {
    val res = MovieLoadResponse(name, url, this.name, type, dataUrl)
    res.builder()
    return res
}

suspend fun MainAPI.newTvSeriesLoadResponse(
    name: String,
    url: String,
    type: TvType = TvType.TvSeries,
    episodes: List<Episode> = emptyList(),
    builder: suspend TvSeriesLoadResponse.() -> Unit = {}
): TvSeriesLoadResponse {
    val res = TvSeriesLoadResponse(name, url, this.name, type, episodes)
    res.builder()
    return res
}

suspend fun MainAPI.newLiveStreamLoadResponse(
    name: String,
    url: String,
    type: TvType = TvType.Live,
    dataUrl: String = url,
    builder: suspend LiveStreamLoadResponse.() -> Unit = {}
): LiveStreamLoadResponse {
    val res = LiveStreamLoadResponse(name, url, this.name, type, dataUrl)
    res.builder()
    return res
}

suspend fun MainAPI.newAnimeLoadResponse(
    name: String,
    url: String,
    type: TvType = TvType.Anime,
    builder: suspend AnimeLoadResponse.() -> Unit = {}
): AnimeLoadResponse {
    val res = AnimeLoadResponse(name, url, this.name, type)
    res.builder()
    return res
}

fun MainAPI.newEpisode(
    data: Any,
    builder: Episode.() -> Unit = {}
): Episode {
    val ep = Episode(data.toString())
    ep.builder()
    return ep
}

fun Episode.addDate(date: String?, pattern: String? = null) {
    this.date = date
}

fun AnimeSearchResponse.addDubStatus(
    isDub: Boolean,
    isSub: Boolean,
    episodes: Int? = null,
    dubEpisodes: Int? = null
) {
    this.dubStatus = when {
        isDub && isSub -> DubStatus.Subbed
        isDub -> DubStatus.Dubbed
        else -> DubStatus.Subbed
    }
    this.epSub = episodes
    this.epDub = dubEpisodes
}

fun AnimeLoadResponse.addEpisodes(
    status: DubStatus,
    episodes: List<Episode>
) {
    this.episodes[status] = episodes
}

suspend fun newSubtitleFile(
    lang: String,
    url: String,
    builder: suspend SubtitleFile.() -> Unit = {}
): SubtitleFile {
    val sub = SubtitleFile(lang, url)
    sub.builder()
    return sub
}

suspend fun newAudioFile(
    url: String,
    builder: suspend AudioFile.() -> Unit = {}
): AudioFile {
    val audio = AudioFile(url)
    audio.builder()
    return audio
}
