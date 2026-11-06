package com.carrot233.travelmemory.data.place

import com.carrot233.travelmemory.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed interface PlaceSearchOutcome {
    data object Unavailable : PlaceSearchOutcome
    data class Hits(val places: List<PlaceSuggestion>) : PlaceSearchOutcome
    data object Empty : PlaceSearchOutcome
    data class Failed(val message: String) : PlaceSearchOutcome
}

object AmapPlaceSearch {
    fun isConfigured(): Boolean = BuildConfig.AMAP_KEY.isNotBlank()

    suspend fun search(keyword: String): PlaceSearchOutcome = withContext(Dispatchers.IO) {
        val key = BuildConfig.AMAP_KEY
        if (key.isBlank()) return@withContext PlaceSearchOutcome.Unavailable
        val query = keyword.trim()
        if (query.isEmpty()) return@withContext PlaceSearchOutcome.Empty
        val url = buildInputTipsUrl(key, query)
        val body = try {
            readHttps(url)
        } catch (error: Exception) {
            return@withContext PlaceSearchOutcome.Failed("搜索失败，请检查网络后重试")
        }
        when (val parsed = parseAmapInputTips(body)) {
            is AmapTipsParse.Error -> PlaceSearchOutcome.Failed(parsed.message)
            is AmapTipsParse.Ok -> if (parsed.places.isEmpty()) {
                PlaceSearchOutcome.Empty
            } else {
                PlaceSearchOutcome.Hits(parsed.places)
            }
        }
    }
}

internal fun buildInputTipsUrl(key: String, keyword: String): String {
    val encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8.name())
    val encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.name())
    return "https://restapi.amap.com/v3/assistant/inputtips?key=$encodedKey&keywords=$encodedKeyword&datatype=all"
}

private fun readHttps(url: String): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "TravelMemoryAndroid")
    }
    try {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
