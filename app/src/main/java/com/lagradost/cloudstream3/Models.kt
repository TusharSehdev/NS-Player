package com.lagradost.cloudstream3

class ErrorLoadingException(message: String? = null) : Exception(message)

enum class TvType {
    Movie,
    TvSeries,
    Anime,
    AnimeMovie,
    OVA,
    Cartoon,
    Live,
    Documentary,
    Torrent,
    AsianDrama,
    Others,
    NSFW;

    companion object {
        val Companion = this
    }
}

enum class ShowStatus {
    Completed,
    Ongoing,
    Other;

    companion object {
        val Companion = this
    }
}

enum class DubStatus(val id: Int) {
    None(0),
    Dubbed(1),
    Subbed(2);

    companion object {
        val Companion = this
    }
}

enum class SearchQuality {
    UHD,
    HD,
    SD,
    CAM,
    TeleSync,
    HQ,
    FourK,
    BlueRay;

    companion object {
        val Companion = this
    }
}

class Score(val score: Double? = null) {
    companion object {
        @JvmStatic
        fun from(score: Integer?, max: Int = 10): Score = Score(score?.toDouble())

        @JvmStatic
        fun from(score: Double?, max: Int = 10): Score = Score(score)
    }
}

enum class ActorRole {
    Main,
    Supporting,
    Background
}

data class Actor(
    val name: String,
    val image: String? = null
)

data class ActorData(
    val actor: Actor,
    val role: ActorRole? = null,
    val roleString: String? = null,
    val voiceActor: Actor? = null
)

data class MainPageData(
    val name: String,
    val data: String,
    val horizontalImages: Boolean = false
)

data class MainPageRequest(
    val name: String,
    val data: String,
    val horizontalImages: Boolean = false
)

data class HomePageList(
    val name: String,
    val list: List<SearchResponse>,
    val isHorizontalImages: Boolean = false
)

data class HomePageResponse(
    val items: List<HomePageList>,
    val hasNext: Boolean = false
)

data class SearchResponseList(
    val list: List<SearchResponse>
)

interface SearchResponse {
    var name: String
    var url: String
    var apiName: String
    var type: TvType?
    var posterUrl: String?
    var id: Int?
    var quality: SearchQuality?
    var posterHeaders: Map<String, String>?
}

data class MovieSearchResponse(
    override var name: String,
    override var url: String,
    override var apiName: String,
    override var type: TvType? = TvType.Movie,
    override var posterUrl: String? = null,
    override var id: Int? = null,
    override var quality: SearchQuality? = null,
    override var posterHeaders: Map<String, String>? = null,
    var year: Int? = null
) : SearchResponse

data class TvSeriesSearchResponse(
    override var name: String,
    override var url: String,
    override var apiName: String,
    override var type: TvType? = TvType.TvSeries,
    override var posterUrl: String? = null,
    override var id: Int? = null,
    override var quality: SearchQuality? = null,
    override var posterHeaders: Map<String, String>? = null,
    var year: Int? = null,
    var episodes: Int? = null
) : SearchResponse

data class AnimeSearchResponse(
    override var name: String,
    override var url: String,
    override var apiName: String,
    override var type: TvType? = TvType.Anime,
    override var posterUrl: String? = null,
    override var id: Int? = null,
    override var quality: SearchQuality? = null,
    override var posterHeaders: Map<String, String>? = null,
    var year: Int? = null,
    var dubStatus: DubStatus? = null,
    var epSub: Int? = null,
    var epDub: Int? = null
) : SearchResponse

data class LiveSearchResponse(
    override var name: String,
    override var url: String,
    override var apiName: String,
    override var type: TvType? = TvType.Live,
    override var posterUrl: String? = null,
    override var id: Int? = null,
    override var quality: SearchQuality? = null,
    override var posterHeaders: Map<String, String>? = null,
    var lang: String? = null
) : SearchResponse

interface LoadResponse {
    var name: String
    var url: String
    var apiName: String
    var type: TvType
    var posterUrl: String?
    var year: Int?
    var plot: String?
    var duration: Int?
    var score: Score?
    var tags: List<String>?
    var recommendations: List<SearchResponse>?
    var actors: List<ActorData>?
    var backgroundPosterUrl: String?
    var logoUrl: String?
    var contentRating: String?

    companion object {
        @JvmStatic
        fun addActorNames(response: LoadResponse, names: List<String>) {
            response.actors = names.map { ActorData(Actor(it)) }
        }

        @JvmStatic
        fun addActorsOnly(response: LoadResponse, actors: List<Actor>) {
            response.actors = actors.map { ActorData(it) }
        }

        @JvmStatic
        fun addImdbId(response: LoadResponse, id: String) {}

        @JvmStatic
        fun addImdbUrl(response: LoadResponse, url: String) {}

        @JvmStatic
        fun addTMDbId(response: LoadResponse, id: String) {}

        @JvmStatic
        suspend fun addTrailer(
            response: LoadResponse,
            url: String?,
            name: String? = null,
            mirrors: Boolean = false
        ) {}
    }
}

data class MovieLoadResponse(
    override var name: String,
    override var url: String,
    override var apiName: String,
    override var type: TvType = TvType.Movie,
    var dataUrl: String = "",
    override var posterUrl: String? = null,
    override var year: Int? = null,
    override var plot: String? = null,
    override var duration: Int? = null,
    override var score: Score? = null,
    override var tags: List<String>? = null,
    override var recommendations: List<SearchResponse>? = null,
    override var actors: List<ActorData>? = null,
    override var backgroundPosterUrl: String? = null,
    override var logoUrl: String? = null,
    override var contentRating: String? = null
) : LoadResponse

data class Episode(
    var data: String,
    var name: String? = null,
    var season: Int? = null,
    var episode: Int? = null,
    var posterUrl: String? = null,
    var score: Score? = null,
    var description: String? = null,
    var runTime: Int? = null,
    var date: String? = null
)

data class TvSeriesLoadResponse(
    override var name: String,
    override var url: String,
    override var apiName: String,
    override var type: TvType = TvType.TvSeries,
    var episodes: List<Episode> = emptyList(),
    override var posterUrl: String? = null,
    override var year: Int? = null,
    override var plot: String? = null,
    override var duration: Int? = null,
    override var score: Score? = null,
    override var tags: List<String>? = null,
    override var recommendations: List<SearchResponse>? = null,
    override var actors: List<ActorData>? = null,
    override var backgroundPosterUrl: String? = null,
    override var logoUrl: String? = null,
    override var contentRating: String? = null,
    var showStatus: ShowStatus? = null
) : LoadResponse

data class LiveStreamLoadResponse(
    override var name: String,
    override var url: String,
    override var apiName: String,
    override var type: TvType = TvType.Live,
    var dataUrl: String = "",
    override var posterUrl: String? = null,
    override var year: Int? = null,
    override var plot: String? = null,
    override var duration: Int? = null,
    override var score: Score? = null,
    override var tags: List<String>? = null,
    override var recommendations: List<SearchResponse>? = null,
    override var actors: List<ActorData>? = null,
    override var backgroundPosterUrl: String? = null,
    override var logoUrl: String? = null,
    override var contentRating: String? = null
) : LoadResponse

data class AnimeLoadResponse(
    override var name: String,
    override var url: String,
    override var apiName: String,
    override var type: TvType = TvType.Anime,
    var episodes: MutableMap<DubStatus, List<Episode>> = mutableMapOf(),
    override var posterUrl: String? = null,
    override var year: Int? = null,
    override var plot: String? = null,
    override var duration: Int? = null,
    override var score: Score? = null,
    override var tags: List<String>? = null,
    override var recommendations: List<SearchResponse>? = null,
    override var actors: List<ActorData>? = null,
    override var backgroundPosterUrl: String? = null,
    override var logoUrl: String? = null,
    override var contentRating: String? = null,
    var showStatus: ShowStatus? = null
) : LoadResponse

data class SubtitleFile(
    val lang: String,
    val url: String,
    var headers: Map<String, String> = emptyMap()
)

data class AudioFile(
    val url: String,
    var headers: Map<String, String> = emptyMap()
)
