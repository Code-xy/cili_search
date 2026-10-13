package com.seagull.cili

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 统一网络出口：一个 OkHttp 客户端，浏览器 UA，超时短一点保证多源并发体验。 */
object Net {

    const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(28, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    fun get(url: String, referer: String? = null): String {
        val b = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        if (!referer.isNullOrBlank()) b.header("Referer", referer)
        client.newCall(b.build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP " + r.code)
            return r.body?.string().orEmpty()
        }
    }

    fun postJson(url: String, body: String, referer: String? = null): String {
        val b = Request.Builder()
            .url(url)
            .post(body.toRequestBody(JSON_TYPE))
            .header("User-Agent", UA)
            .header("Accept", "application/json, */*")
            .header("Content-Type", "application/json")
        if (!referer.isNullOrBlank()) b.header("Referer", referer)
        client.newCall(b.build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP " + r.code)
            return r.body?.string().orEmpty()
        }
    }
}
