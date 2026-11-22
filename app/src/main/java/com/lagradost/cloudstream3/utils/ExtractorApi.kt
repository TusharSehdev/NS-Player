package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.SubtitleFile
import java.util.UUID

val CLEARKEY_UUID: UUID = UUID.fromString("e2719d58-a985-b3c9-781a-b030e4d41716")
val INFER_TYPE: ExtractorLinkType = ExtractorLinkType.VIDEO

open class ExtractorApi {
    open val name: String = ""
    open val mainUrl: String = ""
    open val requiresReferer: Boolean = false

    open suspend fun getUrl(
        url: String,
        referer: String? = null,
        subtitleCallback: (SubtitleFile) -> Unit = {},
        callback: (ExtractorLink) -> Unit = {}
    ): List<ExtractorLink> {
        return emptyList()
    }
}

fun httpsify(url: String): String {
    return if (url.startsWith("//")) "https:$url" else url
}

fun getQualityFromName(qualityName: String?): Int {
    if (qualityName == null) return Qualities.Unknown.value
    val q = qualityName.lowercase()
    return when {
        "2160" in q || "4k" in q -> Qualities.P2160.value
        "1440" in q -> Qualities.P1440.value
        "1080" in q -> Qualities.P1080.value
        "720" in q -> Qualities.P720.value
        "480" in q -> Qualities.P480.value
        "360" in q -> Qualities.P360.value
        "240" in q -> Qualities.P240.value
        else -> Qualities.Unknown.value
    }
}

fun getAndUnpack(packedJs: String): String {
    // Unpacker for Dean Edwards eval(function(p,a,c,k,e,d)...)
    val regex = Regex("""\}\s*\('(.*)',\s*(\d+),\s*(\d+),\s*'(.*)'\.split\('\|'\)""")
    val match = regex.find(packedJs) ?: return packedJs
    val (payload, radixStr, countStr, dictStr) = match.destructured
    val radix = radixStr.toIntOrNull() ?: 36
    val dict = dictStr.split("|")
    val wordRegex = Regex("""\b\w+\b""")
    return wordRegex.replace(payload) { wordResult ->
        val word = wordResult.value
        val index = word.toIntOrNull(radix) ?: return@replace word
        if (index < dict.size && dict[index].isNotBlank()) dict[index] else word
    }
}

suspend fun loadExtractor(
    url: String,
    referer: String? = null,
    subtitleCallback: ((SubtitleFile) -> Unit)? = null,
    callback: (ExtractorLink) -> Unit
): Boolean {
    val type = if (url.contains(".m3u8", ignoreCase = true)) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
    val link = ExtractorLink(
        source = "Universal Extractor",
        name = "Stream",
        url = url,
        referer = referer ?: "",
        quality = Qualities.Unknown.value,
        type = type
    )
    callback(link)
    return true
}

suspend fun loadExtractor(
    url: String,
    subtitleCallback: ((SubtitleFile) -> Unit)? = null,
    callback: (ExtractorLink) -> Unit
): Boolean = loadExtractor(url, null, subtitleCallback, callback)

suspend fun newExtractorLink(
    source: String,
    name: String,
    url: String,
    type: ExtractorLinkType = ExtractorLinkType.VIDEO,
    builder: suspend ExtractorLink.() -> Unit = {}
): ExtractorLink {
    val link = ExtractorLink(source, name, url, "", Qualities.Unknown.value, type)
    link.builder()
    return link
}

suspend fun newDrmExtractorLink(
    source: String,
    name: String,
    url: String,
    type: ExtractorLinkType = ExtractorLinkType.VIDEO,
    keyId: UUID? = null,
    builder: suspend DrmExtractorLink.() -> Unit = {}
): DrmExtractorLink {
    val link = DrmExtractorLink(source, name, url, "", Qualities.Unknown.value, type, keyId = keyId)
    link.builder()
    return link
}
