package com.seagull.cili.data.sources

import com.seagull.cili.Net
import com.seagull.cili.data.MagnetItem
import com.seagull.cili.data.MagnetSource
import com.seagull.cili.data.Rss
import com.seagull.cili.data.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** nyaa.si / sukebei.nyaa.si 共用一套 RSS 解析逻辑，靠 base 区分。 */
class NyaaSource(private val adult: Boolean) : MagnetSource {

    override val id: String = if (adult) "sukebei" else "nyaa"
    override val name: String = if (adult) "Sukebei" else "Nyaa"
    override val group: String = "动漫"
    override val siteUrl: String = if (adult) "https://sukebei.nyaa.si" else "https://nyaa.si"
    override val defaultOn: Boolean = !adult
    override val note: String = if (adult) "18+ 分区，默认关闭" else "动漫索引，RSS 接口稳定"

    override suspend fun search(query: String, page: Int): List<MagnetItem> = withContext(Dispatchers.IO) {
        var url = siteUrl + "/?page=rss&q=" + Util.enc(query) + "&c=0_0&f=0"
        if (page > 1) url += "&p=" + page
        val xml = Net.get(url, referer = siteUrl)
        val out = ArrayList<MagnetItem>()
        for (b in Rss.items(xml)) {
            val title = Rss.tag(b, "title")
            if (title.isEmpty()) continue
            val hash = Rss.tag(b, "nyaa:infoHash").ifEmpty {
                Regex("([0-9a-fA-F]{40})").find(b)?.groupValues?.get(1).orEmpty()
            }
            val magnet = if (hash.isNotEmpty()) Util.magnet(hash, title) else null
            if (magnet == null) continue
            out.add(
                MagnetItem(
                    title = title,
                    magnet = magnet,
                    torrentUrl = Rss.tag(b, "link").ifEmpty { null },
                    detailUrl = Rss.tag(b, "guid").ifEmpty { null },
                    sizeBytes = Util.sizeToBytes(Rss.tag(b, "nyaa:size")),
                    seeders = Rss.tag(b, "nyaa:seeders").toIntOrNull() ?: -1,
                    leechers = Rss.tag(b, "nyaa:leechers").toIntOrNull() ?: -1,
                    publishedAt = Util.parseTime(Rss.tag(b, "pubDate")),
                    sourceId = id,
                    sourceName = name,
                    category = Rss.tag(b, "nyaa:category")
                )
            )
        }
        out
    }
}
