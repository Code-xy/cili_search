package com.seagull.cili.data.sources

import com.seagull.cili.Net
import com.seagull.cili.data.MagnetItem
import com.seagull.cili.data.MagnetSource
import com.seagull.cili.data.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * 動漫花園原站，HTML 表格。
 * 列序：完成时间 | 分類 | 標題 | 磁力 | 大小 | 下載數(btl) | 種子數(bts) | 完成數 | 發布者
 */
object DmhySource : MagnetSource {
    override val id = "dmhy"
    override val name = "動漫花園"
    override val group = "动漫"
    override val siteUrl = "https://share.dmhy.org"
    override val note = "HTML 站点，直出磁力"

    override suspend fun search(query: String, page: Int): List<MagnetItem> = withContext(Dispatchers.IO) {
        val url = siteUrl + "/topics/list?keyword=" + Util.enc(query) + "&page=" + page
        val doc = Jsoup.parse(Net.get(url, referer = siteUrl + "/"), siteUrl)
        val out = ArrayList<MagnetItem>()
        for (tr in doc.select("table#topic_list tbody tr")) {
            val a = tr.select("td.title a").firstOrNull { it.attr("href").contains("/topics/view/") } ?: continue
            val title = a.text().trim()
            if (title.isEmpty()) continue
            val magnet = tr.selectFirst("a.arrow-magnet")?.attr("href")?.trim()
                ?.takeIf { it.startsWith("magnet") }
            val tds = tr.select("td")
            val size = tds.getOrNull(4)?.text()?.trim().orEmpty()
            val leechers = tds.getOrNull(5)?.text()?.trim()?.toIntOrNull() ?: -1
            val seeders = tds.getOrNull(6)?.text()?.trim()?.toIntOrNull() ?: -1
            out.add(
                MagnetItem(
                    title = title,
                    magnet = magnet,
                    detailUrl = siteUrl + a.attr("href"),
                    sizeBytes = Util.sizeToBytes(size),
                    seeders = seeders,
                    leechers = leechers,
                    sourceId = id,
                    sourceName = name,
                    category = tds.getOrNull(1)?.text()?.trim().orEmpty()
                )
            )
        }
        out
    }
}
