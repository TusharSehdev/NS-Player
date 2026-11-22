package com.lagradost.cloudstream3.network

import okhttp3.Interceptor
import okhttp3.Response

class CloudflareKiller : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .build()
        return chain.proceed(request)
    }
}
