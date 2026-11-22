package com.lagradost.cloudstream3.utils

enum class Qualities(val value: Int) {
    Unknown(0),
    P240(240),
    P360(360),
    P480(480),
    P720(720),
    P1080(1080),
    P1440(1440),
    P2160(2160);

    companion object {
        fun getStringByInt(qual: Int?): String {
            return when (qual) {
                P2160.value -> "4K"
                P1440.value -> "1440p"
                P1080.value -> "1080p"
                P720.value -> "720p"
                P480.value -> "480p"
                P360.value -> "360p"
                P240.value -> "240p"
                else -> "Auto"
            }
        }
    }
}
