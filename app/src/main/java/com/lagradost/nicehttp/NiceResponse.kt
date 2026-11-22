package com.lagradost.nicehttp

import okhttp3.Headers
import okhttp3.Response
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class NiceResponse(
    val okhttpResponse: Response,
    val text: String,
    val parser: ResponseParser = ResponseParser()
) {
    val code: Int get() = okhttpResponse.code
    val isSuccessful: Boolean get() = okhttpResponse.isSuccessful
    val url: String get() = okhttpResponse.request.url.toString()
    val headers: Headers get() = okhttpResponse.headers
    val cookies: Map<String, String> get() = emptyMap()
    val body: ResponseBody? get() = okhttpResponse.body

    val document: Document by lazy {
        Jsoup.parse(text, url)
    }

    inline fun <reified T : Any> parsed(): T {
        return parser.parse(text, T::class)
    }

    inline fun <reified T : Any> parsedSafe(): T? {
        return parser.parseSafe(text, T::class)
    }
}
