package com.seagull.cili.data

/**
 * 把种子标题洗成能拿去站内搜索的片名。
 * 例：[银色子弹字幕组][名侦探柯南][第1216集 茜色的千秋乐][WEBRIP][简日双语MP4][1080P] -> 名侦探柯南
 *     CIA.S02E02.1080p.HDTV.x264-SYNCOPY[EZTVx.to]                                        -> CIA S02E02
 */
object TitleClean {

    private val EXT = Regex("\\.(mkv|mp4|avi|rmvb|rm|wmv|flv|mov|ts|m2ts|mpg|mpeg|torrent|iso)$", RegexOption.IGNORE_CASE)
    private val BRACKET = Regex("[\\[\\(【（]([^\\]\\)】）]*)[\\]\\)】）]")
    private val SIZE_ONLY = Regex("^[0-9.]+ ?[KMGT]i?B$", RegexOption.IGNORE_CASE)

    /** 技术规格类标签，出现即丢 */
    private val TAGS = setOf(
        "1080p", "720p", "2160p", "480p", "4k", "8k", "uhd", "hdr", "sdr", "hd", "sd",
        "webrip", "web-rip", "webdl", "web-dl", "web", "bluray", "blu-ray", "bdrip", "brrip",
        "hdtv", "dvdrip", "dvd", "hdrip", "remux", "x264", "x265", "h264", "h265", "hevc", "avc",
        "aac", "ac3", "dts", "ddp", "eac3", "flac", "truehd", "atmos", "10bit", "8bit",
        "proper", "repack", "internal", "multi", "subs", "sub", "chs", "cht", "jpn", "eng",
        "v2", "v3", "complete", "batch", "atv", "web"
    )

    /** 中文附加信息，包含即丢 */
    private val CN_NOISE = listOf(
        "字幕組", "字幕组", "字幕社", "壓制組", "压制组", "发布组", "發布組", "汉化组", "漢化組",
        "资源组", "資源組", "双语", "雙語", "日语", "日語", "简日", "繁日", "简繁", "中日",
        "中字", "内嵌", "內嵌", "外挂", "外掛", "国语", "國語", "粤语", "粵語",
        "完整版", "合集", "全集", "合集版", "高清", "修复版", "修復版"
    )

    private val EPISODE_ONLY = Regex("^第?[0-9]+[集话話期]?$|^EP?[0-9]+$|^S[0-9]+E[0-9]+$", RegexOption.IGNORE_CASE)
    private val GROUP_SUFFIX = Regex("-[A-Za-z0-9]{2,}$")

    fun clean(raw: String): String {
        val src = raw.trim()
        if (src.isEmpty()) return src

        var t = EXT.replace(src, "")

        // 中括号开头的（动漫/资源组命名法）：挑最像片名的那一段
        if (t.startsWith("[") || t.startsWith("【")) {
            val groups = BRACKET.findAll(t).map { it.groupValues[1].trim() }.toList()
            val best = groups
                .filter { g -> g.isNotEmpty() && !isNoise(g) }
                .maxByOrNull { g -> g.count { it.code in 0x4E00..0x9FFF } }
            if (!best.isNullOrBlank()) return best.take(60)
        }

        // 其余：把括号整块丢掉（多数是规格或站点名）
        t = BRACKET.replace(t, " ")

        // 去掉结尾的资源组后缀，如 -SYNCOPY / -SECTOR7
        t = GROUP_SUFFIX.replace(t, "")

        val parts = t.split(Regex("[\\s._]+")).filter { it.isNotBlank() }
        val kept = parts.filter { p ->
            val low = p.lowercase()
            !TAGS.contains(low) && !isNoise(p) && !low.matches(Regex("x26[45](-[a-z0-9]+)?"))
        }
        var out = kept.joinToString(" ").trim(' ', '-', '_', '.')
        if (out.isEmpty()) out = src
        return out.take(60)
    }

    private fun isNoise(g: String): Boolean {
        if (SIZE_ONLY.matches(g)) return true
        if (EPISODE_ONLY.matches(g)) return true
        if (TAGS.contains(g.lowercase())) return true
        return CN_NOISE.any { g.contains(it) }
    }
}
