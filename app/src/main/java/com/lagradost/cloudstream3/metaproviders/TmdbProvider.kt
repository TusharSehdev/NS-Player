package com.lagradost.cloudstream3.metaproviders

data class TmdbLink(
    val imdbId: String? = null,
    val tmdbId: Int? = null,
    val episode: Int? = null,
    val season: Int? = null,
    val movieName: String? = null
)

class TmdbProvider {
    companion object {
        fun getImdbId(url: String): String? = null
    }
}
