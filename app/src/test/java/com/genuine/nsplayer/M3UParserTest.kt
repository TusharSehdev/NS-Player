package com.genuine.nsplayer

import com.genuine.nsplayer.data.model.ContentType
import com.genuine.nsplayer.data.parser.M3UParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class M3UParserTest {

    @Test
    fun testParseStandardM3UWithMetadata() = runBlocking {
        val sampleM3U = """
            #EXTM3U url-tvg="http://guide.xmltv.se/epg.xml.gz"
            #EXTINF:-1 tvg-id="cnn.us" tvg-name="CNN US HD" tvg-logo="https://logos.com/cnn.png" group-title="News",CNN News HD
            http://stream.example.com/live/cnn.m3u8|User-Agent=CustomUA&Referer=https://ref.com
            #EXTINF:-1 tvg-id="hbo.us" tvg-name="HBO Hits" tvg-logo="https://logos.com/hbo.png" group-title="Movies",HBO Hits VOD
            https://stream.example.com/movie/avatar.mp4
            #EXTINF:-1 tvg-id="bbcone.uk" tvg-name="BBC One" tvg-logo="https://logos.com/bbc.png" group-title="UK Entertainment",BBC One
            http://stream.example.com/series/sherlock_s01e01.m3u8
        """.trimIndent()

        val inputStream = ByteArrayInputStream(sampleM3U.toByteArray(Charsets.UTF_8))
        val channels = M3UParser.parseStreamInBatches(inputStream, playlistId = 1).first()

        assertEquals(3, channels.size)

        // Channel 1: CNN (Live TV)
        val ch1 = channels[0]
        assertEquals("CNN News HD", ch1.name)
        assertEquals("http://stream.example.com/live/cnn.m3u8", ch1.streamUrl)
        assertEquals("cnn.us", ch1.tvgId)
        assertEquals("News", ch1.groupTitle)
        assertEquals("https://logos.com/cnn.png", ch1.logoUrl)
        assertEquals(ContentType.LIVE_TV, ch1.contentType)
        assertEquals("CustomUA", ch1.headers["User-Agent"])
        assertEquals("https://ref.com", ch1.headers["Referer"])

        // Channel 2: Movie (VOD)
        val ch2 = channels[1]
        assertEquals("HBO Hits VOD", ch2.name)
        assertEquals("https://stream.example.com/movie/avatar.mp4", ch2.streamUrl)
        assertEquals(ContentType.MOVIE, ch2.contentType)

        // Channel 3: Series
        val ch3 = channels[2]
        assertEquals("BBC One", ch3.name)
        assertEquals("http://stream.example.com/series/sherlock_s01e01.m3u8", ch3.streamUrl)
        assertEquals(ContentType.SERIES, ch3.contentType)
    }

    @Test
    fun testExtractEpgUrl() {
        val sampleM3U = """
            #EXTM3U url-tvg="http://example.com/epg.xml.gz"
            #EXTINF:-1,Channel 1
            http://stream.com/live.m3u8
        """.trimIndent()

        val epgUrl = M3UParser.extractEpgUrl(ByteArrayInputStream(sampleM3U.toByteArray()))
        assertEquals("http://example.com/epg.xml.gz", epgUrl)
    }

    @Test
    fun testMalformedM3UHandling() = runBlocking {
        val malformedM3U = """
            This is random garbage text
            #EXTINF: invalid line without comma
            http://valid.com/stream1.m3u8
            #EXTINF:-1,Valid Channel
            http://valid.com/stream2.m3u8
        """.trimIndent()

        val inputStream = ByteArrayInputStream(malformedM3U.toByteArray(Charsets.UTF_8))
        val channels = M3UParser.parseStreamInBatches(inputStream, playlistId = 2).first()

        // Should successfully extract streams without crashing
        assertTrue(channels.isNotEmpty())
        assertTrue(channels.any { it.name == "Valid Channel" })
    }
}
