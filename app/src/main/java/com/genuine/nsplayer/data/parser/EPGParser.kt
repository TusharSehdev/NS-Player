package com.genuine.nsplayer.data.parser

import android.util.Xml
import com.genuine.nsplayer.data.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object EPGParser {

    private val XMLTV_DATE_FORMATS = listOf(
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    )

    /**
     * Parses an XMLTV XML or GZipped stream and emits programs in batches of [batchSize]
     */
    fun parseEpgStream(
        inputStream: InputStream,
        batchSize: Int = 500
    ): Flow<List<EpgProgram>> = flow {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        val batch = mutableListOf<EpgProgram>()

        var currentChannelId: String? = null
        var currentStartEpoch = 0L
        var currentEndEpoch = 0L
        var currentTitle: String? = null
        var currentDesc: String? = null
        var currentCategory: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (name.lowercase()) {
                        "programme" -> {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            currentStartEpoch = parseXmltvDate(startStr)
                            currentEndEpoch = parseXmltvDate(stopStr)
                            currentTitle = null
                            currentDesc = null
                            currentCategory = null
                        }
                        "title" -> {
                            currentTitle = parser.nextText()
                        }
                        "desc" -> {
                            currentDesc = parser.nextText()
                        }
                        "category" -> {
                            currentCategory = parser.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name.equals("programme", ignoreCase = true) && currentChannelId != null && currentTitle != null) {
                        val program = EpgProgram(
                            id = 0,
                            channelTvgId = currentChannelId,
                            title = currentTitle,
                            description = currentDesc,
                            startEpoch = currentStartEpoch,
                            endEpoch = currentEndEpoch,
                            category = currentCategory
                        )
                        batch.add(program)

                        if (batch.size >= batchSize) {
                            emit(batch)
                            batch.clear()
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (batch.isNotEmpty()) {
            emit(batch)
        }
    }.flowOn(Dispatchers.IO)

    private fun parseXmltvDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        for (format in XMLTV_DATE_FORMATS) {
            try {
                val date = format.parse(dateStr.trim())
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return 0L
    }
}
