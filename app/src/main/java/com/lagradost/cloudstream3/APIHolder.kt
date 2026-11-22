package com.lagradost.cloudstream3

object APIHolder {
    val unixTimeMS: Long get() = System.currentTimeMillis()
    val unixTime: Long get() = unixTimeMS / 1000L
}
