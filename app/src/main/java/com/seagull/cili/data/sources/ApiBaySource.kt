package com.seagull.cili.data.sources

import com.seagull.cili.Net
import com.seagull.cili.data.MagnetItem
import com.seagull.cili.data.MagnetSource
import com.seagull.cili.data.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** The Pirate Bay 的公开 JSON 接口，返回 infohash，磁力自己拼。 */
object ApiBaySource : MagnetSource {
    override val id = "apibay"
    override val name = "海盗湾 TPB"
    override val group = "综合"
    override val siteUrl = "https://thepiratebay.org"
    override val note = "apibay JSON，索引量最大"

    override suspend fun search(query: String, page: Int): List<MagnetItem> = withContext(Dispatchers.IO) {
        if (page > 1) return@withContext emptyList()
        val url = "https://apibay.org/q.php?q=" + Util.enc(query) + "&cat=0"
        val arr = JSONArray(Net.get(url, referer = siteUrl))
        val out = ArrayList<MagnetItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("name").trim()
            val hash = o.optString("info_hash").trim()
            if (title.isEmpty() || hash.length < 32) continue
            if (title.equals("No results returned", true)) continue
            out.add(
                MagnetItem(
                    title = title,
                    magnet = Util.magnet(hash, title),
                    sizeBytes = o.optString("size").toLongOrNull() ?: 0L,
                    seeders = o.optString("seeders").toIntOrNull() ?: -1,
                    leechers = o.optString("leechers").toIntOrNull() ?: -1,
                    publishedAt = o.optString("added").toLongOrNull() ?: 0L,
                    sourceId = id,
                    sourceName = name,
                    category = catName(o.optString("category"))
                )
            )
        }
        out
    }

    private fun catName(c: String): String = when ((c.toIntOrNull() ?: 0) / 100) {
        1 -> "音频"
        2 -> "视频"
        3 -> "软件"
        4 -> "游戏"
        else -> ""
    }
}
