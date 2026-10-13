package com.seagull.cili.data

/**
 * 极简 RSS/Atom 解析。
 * 不用 XML 库：nyaa / acgnx / mikan 三个源的字段位置都很固定，
 * 正则切 item 块 + 单标签提取比引 XML 解析器更抗脏数据，也更快。
 */
object Rss {

    private val ITEM = Regex("<item[^>]*>.*?</item>", RegexOption.DOT_MATCHES_ALL)
    private val TAG = Regex("<[^>]+>")

    fun items(xml: String): List<String> = ITEM.findAll(xml).map { it.value }.toList()

    fun tag(block: String, name: String): String {
        val m = Regex("<" + name + "[^>]*>(.*?)</" + name + ">", RegexOption.DOT_MATCHES_ALL).find(block)
            ?: return ""
        return clean(m.groupValues[1])
    }

    /** 保留内部标签（description 里常带 <a>），用于再抠正则。 */
    fun rawTag(block: String, name: String): String {
        val m = Regex("<" + name + "[^>]*>(.*?)</" + name + ">", RegexOption.DOT_MATCHES_ALL).find(block)
            ?: return ""
        return stripCdata(m.groupValues[1]).trim()
    }

    fun attr(block: String, tagName: String, attrName: String): String {
        val tagRe = Regex("<" + tagName + "[^>]*>", RegexOption.DOT_MATCHES_ALL).find(block) ?: return ""
        val a = Regex(attrName + "=\"([^\"]*)\"").find(tagRe.value) ?: return ""
        return unescape(a.groupValues[1])
    }

    fun clean(s: String): String = unescape(TAG.replace(stripCdata(s), " ")).replace(Regex("[ ]+"), " ").trim()

    private fun stripCdata(s: String): String =
        s.replace("<![CDATA[", "").replace("]]>", "")

    fun unescape(s: String): String = s
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
}
