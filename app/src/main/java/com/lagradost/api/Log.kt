package com.lagradost.api

import android.util.Log as AndroidLog

object Log {
    @JvmStatic
    fun d(tag: String, msg: String) {
        AndroidLog.d(tag, msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        AndroidLog.i(tag, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        AndroidLog.w(tag, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        AndroidLog.e(tag, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String, tr: Throwable?) {
        AndroidLog.e(tag, msg, tr)
    }
}
