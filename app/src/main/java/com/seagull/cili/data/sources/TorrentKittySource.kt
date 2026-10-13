package com.seagull.cili.data.sources

import com.seagull.cili.Net
import com.seagull.cili.data.MagnetItem
import com.seagull.cili.data.MagnetSource
import com.seagull.cili.data.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/** TorrentKitty，HTML 表格，行内直接给磁力。tv 挂了自动换 .net 镜像。 */
object TorrentKittySource : MagnetSource {
    override val id = "tkty"
    override val name = "TorrentKitty"
    override val group = "综合"
    override val siteUrl = "https://www.torrentkitty.tv"
    override val note = "老牌索引，含多域名回退"

    private val hosts = listOf("https://www.torrentkitty.tv", "https://www.torrentkitty.net")

    override suspend fun search(query: String, page: Int): List<MagnetItem> = withContext(Dispatchers.IO) {
        var last: Throwable? = null
        for (h in hosts) {
            try {
                val path = if (page <= 1) "/search/" + Util.encPath(query) + "/"
                           else "/search/" + Util.encPath(query) + "/" + page + "/"
                return@withContext parse(h, Net.get(h + path, referer = h + "/"))
            } catch (e: Throwable) {
                last = e
            }
        }
        throw (last ?: RuntimeException("全部镜像不可用"))
    }

    private fun parse(base: String, html: String): List<MagnetItem> {
        val doc = Jsoup.parse(html, base)
        val out = ArrayList<MagnetItem>()
        for (tr in doc.select("table#archiveResult tr")) {
            val nameCell = tr.selectFirst("td.name") ?: continue
            val title = nameCell.text().trim()
            if (title.isEmpty()) continue
            val magnet = tr.selectFirst("a[href^=magnet:]")?.attr("href")?.trim()
            val rel = tr.selectFirst("a[rel=information]")?.attr("href")?.trim().orEmpty()
            out.add(
                MagnetItem(
                    title = title,
                    magnet = magnet,
                    detailUrl = if (rel.isNotEmpty()) base + rel else null,
                    sizeBytes = Util.sizeToBytes(tr.selectFirst("td.size")?.text()),
                    seeders = -1,
                    leechers = -1,
                    sourceId = id,
                    sourceName = name,
                    category = ""
                )
            )
        }
        return out
    }
}
