package com.genuine.nsplayer

import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class CastleTvExtractorTest {

    @Test
    fun testCastleTvDecryptionAndCatalog() {
        val client = OkHttpClient()
        val secUrl = "https://api.hlowb.com/v0.1/system/getSecurityKey/1?channel=IndiaA&clientType=1&lang=en-US"
        val reqKey = Request.Builder().url(secUrl).header("User-Agent", "okhttp/4.12.0").build()
        val respKey = client.newCall(reqKey).execute()
        assertTrue(respKey.isSuccessful)
        val bodyKey = respKey.body?.string() ?: ""
        val jsonKey = JSONObject(bodyKey)
        val secKeyB64 = jsonKey.getString("data")
        assertNotNull(secKeyB64)

        // Derive key
        val keyBytes = Base64.getDecoder().decode(secKeyB64)
        val suffix = "T!BgJB".toByteArray(StandardCharsets.US_ASCII)
        val combined = keyBytes + suffix
        val finalKey = when {
            combined.size < 16 -> combined.copyOf(16)
            combined.size > 16 -> combined.copyOfRange(0, 16)
            else -> combined
        }

        // Fetch home page
        val homeUrl = "https://api.hlowb.com/film-api/v0.1/category/home?channel=IndiaA&clientType=1&lang=en-US&locationId=1001&mode=1&packageName=com.external.castle&page=1"
        val reqHome = Request.Builder().url(homeUrl).header("User-Agent", "okhttp/4.12.0").build()
        val respHome = client.newCall(reqHome).execute()
        assertTrue(respHome.isSuccessful)
        val encryptedB64 = respHome.body?.string() ?: ""

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(finalKey, "AES"), IvParameterSpec(finalKey))
        val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedB64.trim()))
        val decryptedJson = String(decryptedBytes, StandardCharsets.UTF_8)

        println("Decrypted JSON sample: " + decryptedJson.take(500))
        val root = JSONObject(decryptedJson)
        val data = root.optJSONObject("data") ?: root
        println("Keys in data: " + data.keySet())
        val lists = data.optJSONArray("homePageLists") ?: data.optJSONArray("lists") ?: data.optJSONArray("rows")
        println("Lists array is null? " + (lists == null))
        assertNotNull(lists)
        println("Rows in Castle TV home: ${lists.length()}")
        assertTrue(lists.length() > 0)

        for (i in 0 until minOf(3, lists.length())) {
            val row = lists.getJSONObject(i)
            println("Row $i: ${row.optString("name")}")
            val contents = row.optJSONArray("contents")
            if (contents != null && contents.length() > 0) {
                val item = contents.getJSONObject(0)
                println("  First item: title=${item.optString("title")}, id=${item.opt("id")}, cover=${item.optString("coverVerticalImage")}")
            }
        }
    }

    @Test
    fun testTransformCastleStreamUrl() {
        val rawPreviewUrl = "https://img1.hscow.com/hls_mps/42f4507c383d9e16542bc5fe51f0c979765c7884/480/index_preview_287.m3u8?Expires=1795380913&KeyName=Signature&Signature=test"
        
        val url720 = com.genuine.nsplayer.data.repository.PluginContentResolver.transformCastleStreamUrl(rawPreviewUrl, "720")
        assertEquals(
            "https://img.hscow.com/hls_mps/42f4507c383d9e16542bc5fe51f0c979765c7884/720/index.m3u8",
            url720
        )

        val url1080 = com.genuine.nsplayer.data.repository.PluginContentResolver.transformCastleStreamUrl(rawPreviewUrl, "1080")
        assertEquals(
            "https://img.hscow.com/hls_mps/42f4507c383d9e16542bc5fe51f0c979765c7884/1080/index.m3u8",
            url1080
        )
    }
}
