package com.lagradost.nicehttp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class Requests(
    val baseClient: OkHttpClient = OkHttpClient(),
    var defaultHeaders: Map<String, String> = emptyMap(),
    val defaultReferer: String? = null,
    val defaultParams: Map<String, String> = emptyMap(),
    val defaultCookies: Map<String, String> = emptyMap(),
    val defaultCacheTime: Int = 0,
    val defaultCacheTimeUnit: TimeUnit = TimeUnit.HOURS,
    val defaultTimeout: Long = 0L,
    val defaultResponseParser: ResponseParser = ResponseParser()
) {
    private fun buildUrl(url: String, params: Map<String, String>): HttpUrl {
        val parsed = url.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL: $url")
        if (params.isEmpty()) return parsed
        val builder = parsed.newBuilder()
        for ((k, v) in params) {
            builder.addQueryParameter(k, v)
        }
        return builder.build()
    }

    private fun buildHeaders(
        headers: Map<String, String>,
        referer: String?,
        cookies: Map<String, String>
    ): Headers {
        val map = HashMap<String, String>()
        map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        map.putAll(defaultHeaders)
        if (defaultReferer != null) map["Referer"] = defaultReferer
        if (referer != null) map["Referer"] = referer
        map.putAll(headers)

        if (cookies.isNotEmpty() || defaultCookies.isNotEmpty()) {
            val allCookies = HashMap(defaultCookies).apply { putAll(cookies) }
            val cookieStr = allCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            map["Cookie"] = cookieStr
        }

        val builder = Headers.Builder()
        for ((k, v) in map) {
            try {
                builder.set(k, v)
            } catch (_: Exception) {
            }
        }
        return builder.build()
    }

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        referer: String? = null,
        params: Map<String, String> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
        allowRedirects: Boolean = true,
        cacheTime: Int = 0,
        cacheTimeUnit: TimeUnit = TimeUnit.HOURS,
        timeout: Long = 0L,
        interceptor: Interceptor? = null,
        verify: Boolean = true,
        responseParser: ResponseParser = defaultResponseParser
    ): NiceResponse = withContext(Dispatchers.IO) {
        val httpUrl = buildUrl(url, params)
        val reqHeaders = buildHeaders(headers, referer, cookies)

        var client = baseClient
        if (!allowRedirects || timeout > 0L || interceptor != null) {
            val b = client.newBuilder()
            if (!allowRedirects) b.followRedirects(false).followSslRedirects(false)
            if (timeout > 0L) {
                b.callTimeout(timeout, TimeUnit.SECONDS)
                b.readTimeout(timeout, TimeUnit.SECONDS)
            }
            if (interceptor != null) b.addInterceptor(interceptor)
            client = b.build()
        }

        val request = Request.Builder()
            .url(httpUrl)
            .headers(reqHeaders)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: ""
            NiceResponse(response, text, responseParser)
        }
    }

    suspend fun post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        referer: String? = null,
        params: Map<String, String> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
        data: Map<String, String> = emptyMap(),
        formUrlEncoded: List<Pair<String, String>> = emptyList(),
        json: Any? = null,
        requestBody: RequestBody? = null,
        allowRedirects: Boolean = true,
        cacheTime: Int = 0,
        cacheTimeUnit: TimeUnit = TimeUnit.HOURS,
        timeout: Long = 0L,
        interceptor: Interceptor? = null,
        verify: Boolean = true,
        responseParser: ResponseParser = defaultResponseParser
    ): NiceResponse = withContext(Dispatchers.IO) {
        val httpUrl = buildUrl(url, params)
        val reqHeaders = buildHeaders(headers, referer, cookies)

        val body: RequestBody = when {
            requestBody != null -> requestBody
            json != null -> {
                val jsonStr = responseParser.mapper.writeValueAsString(json)
                jsonStr.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            }
            data.isNotEmpty() -> {
                val b = FormBody.Builder()
                for ((k, v) in data) {
                    b.add(k, v)
                }
                b.build()
            }
            formUrlEncoded.isNotEmpty() -> {
                val b = FormBody.Builder()
                for ((k, v) in formUrlEncoded) {
                    b.add(k, v)
                }
                b.build()
            }
            else -> "".toRequestBody(null)
        }

        var client = baseClient
        if (!allowRedirects || timeout > 0L || interceptor != null) {
            val b = client.newBuilder()
            if (!allowRedirects) b.followRedirects(false).followSslRedirects(false)
            if (timeout > 0L) {
                b.callTimeout(timeout, TimeUnit.SECONDS)
                b.readTimeout(timeout, TimeUnit.SECONDS)
            }
            if (interceptor != null) b.addInterceptor(interceptor)
            client = b.build()
        }

        val request = Request.Builder()
            .url(httpUrl)
            .headers(reqHeaders)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: ""
            NiceResponse(response, text, responseParser)
        }
    }

    suspend fun head(
        url: String,
        headers: Map<String, String> = emptyMap(),
        referer: String? = null,
        params: Map<String, String> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
        allowRedirects: Boolean = true,
        cacheTime: Int = 0,
        cacheTimeUnit: TimeUnit = TimeUnit.HOURS,
        timeout: Long = 0L,
        interceptor: Interceptor? = null,
        verify: Boolean = true,
        responseParser: ResponseParser = defaultResponseParser
    ): NiceResponse = withContext(Dispatchers.IO) {
        val httpUrl = buildUrl(url, params)
        val reqHeaders = buildHeaders(headers, referer, cookies)

        val request = Request.Builder()
            .url(httpUrl)
            .headers(reqHeaders)
            .head()
            .build()

        baseClient.newCall(request).execute().use { response ->
            NiceResponse(response, "", responseParser)
        }
    }
}
