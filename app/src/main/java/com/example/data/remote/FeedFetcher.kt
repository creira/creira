package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object FeedFetcher {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    suspend fun fetchRss(url: String, category: String = "General"): Result<RssParser.ParsedFeedResult> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; PulseFeed/1.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
                    .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
                }

                val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty feed response"))
                val parsed = RssParser.parse(url, bodyString, category)
                Result.success(parsed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
