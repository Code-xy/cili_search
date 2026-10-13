package com.seagull.cili.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seagull.cili.Net
import com.seagull.cili.data.MagnetItem
import com.seagull.cili.data.MagnetSource
import com.seagull.cili.data.RemoteConfig
import com.seagull.cili.data.SearchEngine
import com.seagull.cili.data.SourceEvent
import com.seagull.cili.data.SourceRegistry
import com.seagull.cili.data.SourceState
import com.seagull.cili.data.SourceStatus
import com.seagull.cili.data.Store
import com.seagull.cili.data.sources.SelectorSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _items = MutableStateFlow<List<MagnetItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _status = MutableStateFlow<List<SourceStatus>>(emptyList())
    val status = _status.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching = _searching.asStateFlow()

    private val _allSources = MutableStateFlow<List<MagnetSource>>(SourceRegistry.builtIn)
    val allSources = _allSources.asStateFlow()

    private val _disabled = MutableStateFlow<Set<String>>(emptySet())
    val disabled = _disabled.asStateFlow()

    val history = MutableStateFlow<List<String>>(emptyList())
    val favs = MutableStateFlow<List<MagnetItem>>(emptyList())

    private val _tip = MutableStateFlow("")
    val tip = _tip.asStateFlow()

    private var job: Job? = null
    private var page = 1
    private var lastQuery = ""
    private val seen = HashSet<String>()

    init {
        _disabled.value = store.disabledSources()
        history.value = store.history()
        favs.value = store.favs()
        val cached = store.configCache()
        if (cached != null) _tip.value = "已载入缓存站点配置"
        rebuildSources(cached)
    }

    private fun rebuildSources(configJson: String?) {
        val list = ArrayList<MagnetSource>(SourceRegistry.builtIn)
        if (configJson != null) {
            try {
                RemoteConfig.parse(configJson).sites.forEach { list.add(SelectorSource(it)) }
            } catch (e: Exception) {
                // 配置坏了就用内置的
            }
        }
        _allSources.value = list
    }

    /** 真正参与搜索的源：非成人源 + 用户显式开启的成人源。 */
    val activeSources: List<MagnetSource>
        get() = _allSources.value.filter { src ->
            src.id !in _disabled.value && (src.id != "sukebei" || store.showAdult)
        }

    fun isEnabled(src: MagnetSource): Boolean =
        src.id !in _disabled.value && (src.id != "sukebei" || store.showAdult)

    fun toggleSource(src: MagnetSource, on: Boolean) {
        if (src.id == "sukebei") {
            store.showAdult = on
        } else {
            store.setDisabled(src.id, !on)
            _disabled.value = store.disabledSources()
        }
        _allSources.value = _allSources.value.toList() // 触发重组
    }

    fun setQuery(q: String) { _query.value = q }

    fun search(q: String? = null, more: Boolean = false) {
        val text = (q ?: _query.value).trim()
        if (text.isEmpty()) return
        if (more && lastQuery.isEmpty()) return
        job?.cancel()
        page = if (more) page + 1 else 1
        if (!more) {
            lastQuery = text
            seen.clear()
            _items.value = emptyList()
            store.addHistory(text)
            history.value = store.history()
        }
        _query.value = text
        val list = activeSources
        if (list.isEmpty()) {
            _status.value = emptyList()
            _searching.value = false
            _tip.value = "没有启用任何站点，去设置里开几个"
            return
        }
        _tip.value = ""
        val query = if (more) lastQuery else text
        _status.value = list.map { SourceStatus(it.id, it.name, SourceState.RUNNING) }
        _searching.value = true
        job = viewModelScope.launch {
            SearchEngine.stream(list, query, page).collect { ev ->
                when (ev) {
                    is SourceEvent.Started -> mark(ev.sourceId, SourceState.RUNNING)
                    is SourceEvent.Done -> {
                        mark(
                            ev.sourceId,
                            if (ev.items.isEmpty()) SourceState.EMPTY else SourceState.DONE,
                            ev.items.size, ev.ms, ""
                        )
                        merge(ev.items)
                    }
                    is SourceEvent.Failed -> mark(ev.sourceId, SourceState.FAILED, 0, ev.ms, ev.message)
                }
            }
            _searching.value = false
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _searching.value = false
    }

    private fun mark(id: String, state: SourceState, count: Int = 0, ms: Long = 0L, msg: String = "") {
        _status.value = _status.value.map {
            if (it.id == id) it.copy(state = state, count = count, ms = ms, message = msg) else it
        }
    }

    private fun merge(newItems: List<MagnetItem>) {
        val cur = _items.value.toMutableList()
        for (m in newItems) if (seen.add(m.key)) cur.add(m)
        val sorted = cur.sortedWith(
            compareByDescending<MagnetItem> { if (it.seeders < 0) -1 else it.seeders }
                .thenByDescending { it.publishedAt }
        )
        _items.value = sorted
    }

    fun isFav(item: MagnetItem): Boolean = favs.value.any { it.key == item.key }

    fun toggleFav(item: MagnetItem): Boolean {
        val added = store.toggleFav(item)
        favs.value = store.favs()
        return added
    }

    fun clearHistory() {
        store.clearHistory()
        history.value = emptyList()
    }

    fun refreshConfig() {
        viewModelScope.launch {
            _tip.value = "正在拉取站点配置…"
            try {
                val json = withContext(Dispatchers.IO) { Net.get(RemoteConfig.CONFIG_URL) }
                RemoteConfig.parse(json) // 先校验再落盘
                store.setConfigCache(json)
                rebuildSources(json)
                _tip.value = "站点配置已更新（" + RemoteConfig.parse(json).sites.size + " 个远程站点）"
            } catch (e: Exception) {
                _tip.value = "配置更新失败：" + (e.message ?: "网络错误")
            }
        }
    }
}
