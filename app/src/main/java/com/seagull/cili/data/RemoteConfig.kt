package com.seagull.cili.data

import com.seagull.cili.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** 远程站点定义：站点改版/换域名时改仓库里的 sites.json 就行，不用重新发版。 */
data class SiteSpec(
    val id: String,
    val name: String,
    val group: String = "自定义",
    val siteUrl: String = "",
    val urlTemplate: String = "",
    val itemSelector: String = "",
    val titleSelector: String = "a",
    val titleAttr: String = "text",
    val magnetSelector: String = "a[href^='magnet:']",
    val magnetAttr: String = "href",
    val torrentSelector: String = "",
    val torrentAttr: String = "href",
    val detailSelector: String = "",
    val detailAttr: String = "href",
    val sizeSelector: String = "",
    val sizeAttr: String = "text",
    val seedersSelector: String = "",
    val leechersSelector: String = "",
    val dateSelector: String = "",
    val note: String = "远程配置",
    val defaultOn: Boolean = true
)

object RemoteConfig {

    const val CONFIG_URL = "https://raw.githubusercontent.com/Code-xy/cili_search/main/sites.json"

    data class Config(val disabled: List<String>, val sites: List<SiteSpec>)

    fun parse(json: String): Config {
        val o = JSONObject(json)
        val disabled = ArrayList<String>()
        o.optJSONArray("disabled")?.let { a ->
            for (i in 0 until a.length()) disabled.add(a.optString(i))
        }
        val sites = ArrayList<SiteSpec>()
        o.optJSONArray("sites")?.let { arr ->
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val sid = s.optString("id").trim()
                val tpl = s.optString("urlTemplate").trim()
                val item = s.optString("itemSelector").trim()
                if (sid.isEmpty() || tpl.isEmpty() || item.isEmpty()) continue
                sites.add(
                    SiteSpec(
                        id = sid,
                        name = s.optString("name").trim().ifEmpty { sid },
                        group = s.optString("group").trim().ifEmpty { "自定义" },
                        siteUrl = s.optString("siteUrl").trim(),
                        urlTemplate = tpl,
                        itemSelector = item,
                        titleSelector = s.optString("titleSelector").trim().ifEmpty { "a" },
                        titleAttr = s.optString("titleAttr").trim().ifEmpty { "text" },
                        magnetSelector = s.optString("magnetSelector").trim().ifEmpty { "a[href^='magnet:']" },
                        magnetAttr = s.optString("magnetAttr").trim().ifEmpty { "href" },
                        torrentSelector = s.optString("torrentSelector").trim(),
                        torrentAttr = s.optString("torrentAttr").trim().ifEmpty { "href" },
                        detailSelector = s.optString("detailSelector").trim(),
                        detailAttr = s.optString("detailAttr").trim().ifEmpty { "href" },
                        sizeSelector = s.optString("sizeSelector").trim(),
                        sizeAttr = s.optString("sizeAttr").trim().ifEmpty { "text" },
                        seedersSelector = s.optString("seedersSelector").trim(),
                        leechersSelector = s.optString("leechersSelector").trim(),
                        dateSelector = s.optString("dateSelector").trim(),
                        note = s.optString("note").trim().ifEmpty { "远程配置" },
                        defaultOn = s.optBoolean("defaultOn", true)
                    )
                )
            }
        }
        return Config(disabled, sites)
    }

    suspend fun fetch(url: String = CONFIG_URL): Config = withContext(Dispatchers.IO) {
        parse(Net.get(url))
    }
}
