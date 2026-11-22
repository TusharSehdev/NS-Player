package com.lagradost.cloudstream3.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

object AppUtils {
    val mapper: JsonMapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()

    @JvmStatic
    fun toJson(any: Any?): String = mapper.writeValueAsString(any)

    inline fun <reified T> parseJson(json: String): T = mapper.readValue(json, T::class.java)

    inline fun <reified T> tryParseJson(json: String?): T? {
        if (json.isNullOrBlank()) return null
        return try {
            parseJson<T>(json)
        } catch (_: Exception) {
            null
        }
    }
}
