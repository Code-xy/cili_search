package com.seagull.cili.data.sources

import com.seagull.cili.Net
import com.seagull.cili.data.MagnetItem
import com.seagull.cili.data.MagnetSource
import com.seagull.cili.data.Rss
import com.seagull.cili.data.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 動漫花園镜像站的 RSS。link 形如 show-<infohash>.html，hash 直接从 URL 里薅。 */
object AcgnxSource : MagnetSource {
    override val id = "acgnx"
    override val name = "動漫花園镜像"
    override val group = "动漫"
    override val siteUrl = "https://share.acgnx.se"
    override val note = "dmhy 镜像，RSS 带 infoHash"

    private val HASH_IN_LINK = Regex("show-([0-9a-fA-F]{40})")
    private val SIZE_IN_DESC = Regex("([0-9.]+[ ]*[KMGT]i?B)", RegexOption.IGNORE_CASE)

    override suspend fun search(query: String, page: Int): List<MagnetItem> = withContext(Dispatchers.IO) {
        val url = siteUrl + "/rss.xml?keyword=" + Util.enc(query)
        val xml = Net.get(url, referer = siteUrl + "/")
        val out = ArrayList<MagnetItem>()
        for (b in Rss.items(xml)) {
            val title = Rss.tag(b, "title")
            if (title.isEmpty()) continue
            val link = Rss.tag(b, "link")
            val desc = Rss.rawTag(b, "description")
            val hash = HASH_IN_LINK.find(link)?.groupValues?.get(1)
                ?: HASH_IN_LINK.find(desc)?.groupValues?.get(1)
                ?: Regex("([0-9a-fA-F]{40})").find(desc)?.groupValues?.get(1).orEmpty()
            out.add(
                MagnetItem(
                    title = title,
                    magnet = if (hash.isNotEmpty()) Util.magnet(hash, title) else null,
                    torrentUrl = Rss.attr(b, "enclosure", "url").ifEmpty { null },
                    detailUrl = link.ifEmpty { null },
                    sizeBytes = Util.sizeToBytes(SIZE_IN_DESC.find(desc)?.groupValues?.get(1)),
                    seeders = -1,
                    leechers = -1,
                    publishedAt = Util.parseTime(Rss.tag(b, "pubDate")),
                    sourceId = id,
                    sourceName = name,
                    category = "动画"
                )
            )
        }
        out
    }
}
