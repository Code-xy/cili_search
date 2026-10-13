package com.seagull.cili.data.sources

import com.seagull.cili.Net
import com.seagull.cili.data.MagnetItem
import com.seagull.cili.data.MagnetSource
import com.seagull.cili.data.Rss
import com.seagull.cili.data.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 蜜柑计划：字幕组聚合。RSS 只给 .torrent 下载地址，没有 infoHash，
 * 所以这里 magnet 留空、给 torrentUrl + detailUrl，UI 会显示「种子」按钮。
 */
object MikanSource : MagnetSource {
    override val id = "mikan"
    override val name = "蜜柑计划"
    override val group = "动漫"
    override val siteUrl = "https://mikanani.me"
    override val note = "字幕组分集，给 .torrent 不给磁力"

    override suspend fun search(query: String, page: Int): List<MagnetItem> = withContext(Dispatchers.IO) {
        val url = siteUrl + "/RSS/Search?searchstr=" + Util.enc(query)
        val xml = Net.get(url, referer = siteUrl + "/")
        val out = ArrayList<MagnetItem>()
        for (b in Rss.items(xml)) {
            val title = Rss.tag(b, "title")
            if (title.isEmpty()) continue
            val detail = Rss.tag(b, "link")
            val enclosure = Rss.attr(b, "enclosure", "url")
            val len = Rss.tag(b, "contentLength").toLongOrNull() ?: 0L
            out.add(
                MagnetItem(
                    title = title,
                    magnet = null,
                    torrentUrl = enclosure.ifEmpty { null },
                    detailUrl = detail.ifEmpty { null },
                    sizeBytes = len,
                    seeders = -1,
                    leechers = -1,
                    publishedAt = Util.parseTime(Rss.tag(b, "pubDate")),
                    sourceId = id,
                    sourceName = name,
                    category = "番剧"
                )
            )
        }
        out
    }
}
