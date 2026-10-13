package com.seagull.cili.data

import com.seagull.cili.data.sources.AcgnxSource
import com.seagull.cili.data.sources.ApiBaySource
import com.seagull.cili.data.sources.DmhySource
import com.seagull.cili.data.sources.EztvSource
import com.seagull.cili.data.sources.MikanSource
import com.seagull.cili.data.sources.NyaaSource
import com.seagull.cili.data.sources.TorrentKittySource

interface MagnetSource {
    val id: String
    val name: String
    val group: String
    val siteUrl: String
    val note: String get() = ""
    val defaultOn: Boolean get() = true
    suspend fun search(query: String, page: Int): List<MagnetItem>
}

object SourceRegistry {
    val builtIn: List<MagnetSource> = listOf(
        ApiBaySource,
        TorrentKittySource,
        DmhySource,
        AcgnxSource,
        MikanSource,
        NyaaSource(false),
        NyaaSource(true),
        EztvSource
    )
}
