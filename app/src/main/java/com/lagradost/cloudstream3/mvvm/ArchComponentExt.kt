package com.lagradost.cloudstream3.mvvm

import android.util.Log

fun logError(t: Throwable) {
    Log.e("CloudStream", "Plugin Error: ${t.message}", t)
}

fun <T> safe(block: () -> T): T? {
    return try {
        block()
    } catch (e: Throwable) {
        logError(e)
        null
    }
}
