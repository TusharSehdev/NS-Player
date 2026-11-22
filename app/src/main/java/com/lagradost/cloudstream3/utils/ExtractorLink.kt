package com.lagradost.cloudstream3.utils

import java.util.UUID

enum class ExtractorLinkType {
    VIDEO,
    M3U8,
    DASH;

    fun isM3u8(): Boolean = this == M3U8
    fun isDash(): Boolean = this == DASH
}

open class ExtractorLink(
    open var source: String,
    open var name: String,
    open var url: String,
    open var referer: String,
    open var quality: Int,
    open var type: ExtractorLinkType = ExtractorLinkType.VIDEO,
    open var headers: Map<String, String> = emptyMap(),
    open var extractorData: String? = null
) {
    fun isM3u8(): Boolean = type == ExtractorLinkType.M3U8 || url.contains(".m3u8", ignoreCase = true)
    var audioTracks: List<Any>? = null
}

open class DrmExtractorLink(
    source: String,
    name: String,
    url: String,
    referer: String,
    quality: Int,
    type: ExtractorLinkType = ExtractorLinkType.VIDEO,
    headers: Map<String, String> = emptyMap(),
    extractorData: String? = null,
    var keyId: UUID? = null,
    var kid: String? = null,
    var key: String? = null,
    var kty: String? = null,
    var licenseUrl: String? = null
) : ExtractorLink(source, name, url, referer, quality, type, headers, extractorData)
