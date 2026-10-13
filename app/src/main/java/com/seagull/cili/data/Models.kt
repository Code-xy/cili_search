package com.seagull.cili.data

data class MagnetItem(
    val title: String,
    val magnet: String? = null,
    val torrentUrl: String? = null,
    val detailUrl: String? = null,
    val sizeBytes: Long = 0L,
    val seeders: Int = -1,
    val leechers: Int = -1,
    val publishedAt: Long = 0L,
    val sourceId: String = "",
    val sourceName: String = "",
    val category: String = ""
) {
    val hash: String get() = Util.parseHash(magnet) ?: ""
    val sizeText: String get() = Util.fmtSize(sizeBytes)

    /** 去重键：优先 infohash，没有就退化成标题。 */
    val key: String get() = if (hash.isNotEmpty()) hash else title.lowercase()
}

sealed interface SourceEvent {
    data class Started(val sourceId: String, val sourceName: String) : SourceEvent
    data class Done(val sourceId: String, val sourceName: String, val items: List<MagnetItem>, val ms: Long) : SourceEvent
    data class Failed(val sourceId: String, val sourceName: String, val message: String, val ms: Long) : SourceEvent
}

enum class SourceState { IDLE, RUNNING, DONE, EMPTY, FAILED }

data class SourceStatus(
    val id: String,
    val name: String,
    val state: SourceState = SourceState.IDLE,
    val count: Int = 0,
    val ms: Long = 0L,
    val message: String = ""
)
