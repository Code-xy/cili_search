package com.seagull.cili.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 轻量本地存储：搜索历史、收藏、源开关、远程配置缓存。 */
class Store(ctx: Context) {

    private val sp = ctx.getSharedPreferences("cili", Context.MODE_PRIVATE)

    // ── 搜索历史 ──
    fun history(): List<String> = sp.getString(KEY_HIST, "").orEmpty()
        .split("\n").filter { it.isNotBlank() }

    fun addHistory(q: String) {
        val t = q.trim()
        if (t.isEmpty()) return
        val list = history().filter { it != t }.toMutableList()
        list.add(0, t)
        sp.edit().putString(KEY_HIST, list.take(30).joinToString("\n")).apply()
    }

    fun clearHistory() { sp.edit().remove(KEY_HIST).apply() }

    // ── 收藏 ──
    fun favs(): List<MagnetItem> {
        val raw = sp.getString(KEY_FAV, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                MagnetItem(
                    title = o.optString("title"),
                    magnet = o.optString("magnet").ifEmpty { null },
                    torrentUrl = o.optString("torrent").ifEmpty { null },
                    detailUrl = o.optString("detail").ifEmpty { null },
                    sizeBytes = o.optLong("size", 0L),
                    seeders = o.optInt("seeders", -1),
                    leechers = o.optInt("leechers", -1),
                    publishedAt = o.optLong("time", 0L),
                    sourceId = o.optString("sid"),
                    sourceName = o.optString("sname"),
                    category = o.optString("cat")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isFav(key: String): Boolean = favs().any { it.key == key }

    /** 返回 true 表示这次是「加入收藏」。 */
    fun toggleFav(item: MagnetItem): Boolean {
        val list = favs().toMutableList()
        val idx = list.indexOfFirst { it.key == item.key }
        if (idx >= 0) {
            list.removeAt(idx)
            saveFav(list)
            return false
        }
        list.add(0, item)
        saveFav(list)
        return true
    }

    private fun saveFav(list: List<MagnetItem>) {
        val arr = JSONArray()
        for (m in list) {
            arr.put(JSONObject().apply {
                put("title", m.title)
                put("magnet", m.magnet.orEmpty())
                put("torrent", m.torrentUrl.orEmpty())
                put("detail", m.detailUrl.orEmpty())
                put("size", m.sizeBytes)
                put("seeders", m.seeders)
                put("leechers", m.leechers)
                put("time", m.publishedAt)
                put("sid", m.sourceId)
                put("sname", m.sourceName)
                put("cat", m.category)
            })
        }
        sp.edit().putString(KEY_FAV, arr.toString()).apply()
    }

    // ── 源开关 ──
    fun disabledSources(): MutableSet<String> =
        sp.getStringSet(KEY_DISABLED, emptySet())?.toMutableSet() ?: mutableSetOf()

    fun setDisabled(id: String, disabled: Boolean) {
        val s = disabledSources()
        if (disabled) s.add(id) else s.remove(id)
        sp.edit().putStringSet(KEY_DISABLED, s).apply()
    }

    // ── 远程配置缓存 ──
    fun configCache(): String? = sp.getString(KEY_CFG, null)
    fun setConfigCache(json: String) { sp.edit().putString(KEY_CFG, json).apply() }

    // ── 设置 ──
    var showAdult: Boolean
        get() = sp.getBoolean(KEY_ADULT, false)
        set(v) { sp.edit().putBoolean(KEY_ADULT, v).apply() }

    private companion object {
        const val KEY_HIST = "hist"
        const val KEY_FAV = "fav"
        const val KEY_DISABLED = "disabled"
        const val KEY_CFG = "cfg"
        const val KEY_ADULT = "adult"
    }
}
