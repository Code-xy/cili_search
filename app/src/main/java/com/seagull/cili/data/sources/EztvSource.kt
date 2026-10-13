package com.seagull.cili.data.sources

import com.seagull.cili.Net
import com.seagull.cili.data.MagnetItem
import com.seagull.cili.data.MagnetSource
import com.seagull.cili.data.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** EZTV：美剧专线，官方 JSON 接口直接给 magnet_url。 */
object EztvSource : MagnetSource {
    override val id = "eztv"
    override val name = "EZTV"
    override val group = "剧集"
    override val siteUrl = "https://eztv.re"
    override val note = "欧美剧集，JSON 接口直出磁力"

    override suspend fun search(query: String, page: Int): List<MagnetItem> = withContext(Dispatchers.IO) {
        val url = "https://eztv.re/api/get-torrents?limit=100&page=" + page + "&search=" + Util.enc(query)
        val root = JSONObject(Net.get(url, referer = siteUrl))
        val arr = root.optJSONArray("torrents") ?: return@withContext emptyList()
        val out = ArrayList<MagnetItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("filename").trim().ifEmpty { o.optString("title").trim() }
            if (title.isEmpty()) continue
            val hash = o.optString("hash").trim()
            var mag = o.optString("magnet_url").trim()
            if (!mag.startsWith("magnet")) {
                mag = if (hash.length >= 32) Util.magnet(hash, title) else ""
            }
            if (mag.isEmpty()) continue
            out.add(
                MagnetItem(
                    title = title,
                    magnet = mag,
                    torrentUrl = o.optString("torrent_url").trim().ifEmpty { null },
                    sizeBytes = o.optLong("size_bytes", 0L),
                    seeders = o.optInt("seeds", -1),
                    leechers = o.optInt("peers", -1),
                    publishedAt = o.optLong("date_released_unix", 0L),
                    sourceId = id,
                    sourceName = name,
                    category = "剧集"
                )
            )
        }
        out
    }
}
