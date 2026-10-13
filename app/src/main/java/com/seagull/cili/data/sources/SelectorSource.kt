package com.seagull.cili.data.sources

import com.seagull.cili.Net
import com.seagull.cili.data.MagnetItem
import com.seagull.cili.data.MagnetSource
import com.seagull.cili.data.SiteSpec
import com.seagull.cili.data.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * 选择器驱动的通用源：给一份 SiteSpec（来自 sites.json）就能抓任意站点。
 * 站点改版时只改 JSON 里的 CSS 选择器，App 不用重装。
 */
class SelectorSource(private val spec: SiteSpec) : MagnetSource {

    override val id: String get() = spec.id
    override val name: String get() = spec.name
    override val group: String get() = spec.group
    override val siteUrl: String get() = spec.siteUrl
    override val note: String get() = spec.note
    override val defaultOn: Boolean get() = spec.defaultOn

    override suspend fun search(query: String, page: Int): List<MagnetItem> = withContext(Dispatchers.IO) {
        val url = spec.urlTemplate
            .replace("{q}", Util.enc(query))
            .replace("{query}", Util.enc(query))
            .replace("{page}", page.toString())
        val html = Net.get(url, referer = spec.siteUrl.ifEmpty { null })
        val doc = Jsoup.parse(html, spec.siteUrl.ifEmpty { url })
        val out = ArrayList<MagnetItem>()
        for (el in doc.select(spec.itemSelector)) {
            val title = pick(el, spec.titleSelector, spec.titleAttr) ?: continue
            val magnet = pick(el, spec.magnetSelector, spec.magnetAttr)
                ?.takeIf { it.startsWith("magnet") }
            val torrent = pick(el, spec.torrentSelector, spec.torrentAttr)
            if (magnet == null && torrent == null) continue
            val detail = pick(el, spec.detailSelector, spec.detailAttr)?.let { abs(it) }
            out.add(
                MagnetItem(
                    title = title,
                    magnet = magnet,
                    torrentUrl = torrent?.let { abs(it) },
                    detailUrl = detail,
                    sizeBytes = Util.sizeToBytes(pick(el, spec.sizeSelector, spec.sizeAttr)),
                    seeders = pick(el, spec.seedersSelector, "text")?.toIntOrNull() ?: -1,
                    leechers = pick(el, spec.leechersSelector, "text")?.toIntOrNull() ?: -1,
                    publishedAt = Util.parseTime(pick(el, spec.dateSelector, "text")),
                    sourceId = id,
                    sourceName = name,
                    category = ""
                )
            )
        }
        out
    }

    private fun abs(u: String): String = when {
        u.startsWith("http") -> u
        u.startsWith("//") -> "https:" + u
        spec.siteUrl.isNotEmpty() -> spec.siteUrl.trimEnd('/') + "/" + u.trimStart('/')
        else -> u
    }

    private fun pick(el: Element, selector: String, attr: String): String? {
        if (selector.isEmpty()) return null
        val target: Element = if (selector == ":self") el else el.selectFirst(selector) ?: return null
        val v = if (attr.isEmpty() || attr == "text") target.text() else target.attr(attr)
        return v.trim().ifEmpty { null }
    }
}
