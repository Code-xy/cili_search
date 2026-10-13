package com.seagull.cili.data

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object Util {

    /** 公共 tracker，构造磁力时带上，能明显提高吸血端的连通率。 */
    val DEFAULT_TRACKERS = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.tracker.cl:1337/announce",
        "udp://tracker.openbittorrent.com:6969/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://open.demonii.com:1337/announce",
        "http://tracker.openbittorrent.com:80/announce",
        "udp://tracker.dler.org:6969/announce",
        "udp://tracker.tiny-vps.com:6969/announce"
    )

    private val SIZE_RE = Regex("([0-9.]+)[ ]*([KMGT]?i?B)", RegexOption.IGNORE_CASE)
    private val HASH_RE = Regex("btih:([0-9a-zA-Z]{32,40})")

    fun sizeToBytes(text: String?): Long {
        if (text.isNullOrBlank()) return 0L
        val m = SIZE_RE.find(text) ?: return 0L
        val v = m.groupValues[1].toDoubleOrNull() ?: return 0L
        val unit = m.groupValues[2].uppercase(Locale.US).replace("I", "")
        val mul = when (unit) {
            "TB" -> 1024L * 1024 * 1024 * 1024
            "GB" -> 1024L * 1024 * 1024
            "MB" -> 1024L * 1024
            "KB" -> 1024L
            else -> 1L
        }
        return (v * mul).toLong()
    }

    fun fmtSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val tb = 1024.0 * 1024 * 1024 * 1024
        val gb = 1024.0 * 1024 * 1024
        val mb = 1024.0 * 1024
        val kb = 1024.0
        return when {
            bytes >= tb -> String.format(Locale.US, "%.2f TB", bytes / tb)
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> bytes.toString() + " B"
        }
    }

    fun enc(s: String): String = try {
        URLEncoder.encode(s, "UTF-8")
    } catch (e: Exception) {
        s
    }

    /** URL 路径里用，空格必须 %20 而不是 + */
    fun encPath(s: String): String = enc(s).replace("+", "%20")

    fun magnet(infohash: String, name: String?, trackers: List<String> = DEFAULT_TRACKERS): String {
        val sb = StringBuilder("magnet:?xt=urn:btih:").append(infohash)
        if (!name.isNullOrBlank()) sb.append("&dn=").append(enc(name))
        for (t in trackers) sb.append("&tr=").append(enc(t))
        return sb.toString()
    }

    fun parseHash(magnet: String?): String? {
        if (magnet.isNullOrBlank()) return null
        val m = HASH_RE.find(magnet) ?: return null
        return m.groupValues[1].uppercase(Locale.US)
    }

    /** "今天 08:20" / RFC822 / ISO8601 都吃，失败返回 0。 */
    fun parseTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val s = raw.trim()
        try {
            val iso = Regex("([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})").find(s)
            if (iso != null) {
                val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                c.clear()
                c.set(
                    iso.groupValues[1].toInt(), iso.groupValues[2].toInt() - 1, iso.groupValues[3].toInt(),
                    iso.groupValues[4].toInt(), iso.groupValues[5].toInt(), iso.groupValues[6].toInt()
                )
                return c.timeInMillis / 1000
            }
            val fmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            return fmt.parse(s)?.time?.div(1000) ?: 0L
        } catch (e: Exception) {
            return 0L
        }
    }

    fun relTime(epochSec: Long): String {
        if (epochSec <= 0L) return ""
        val diff = System.currentTimeMillis() / 1000 - epochSec
        if (diff < 0) return ""
        return when {
            diff < 60 -> "刚刚"
            diff < 3600 -> (diff / 60).toString() + " 分钟前"
            diff < 86400 -> (diff / 3600).toString() + " 小时前"
            diff < 86400 * 30 -> (diff / 86400).toString() + " 天前"
            diff < 86400 * 365 -> (diff / 86400 / 30).toString() + " 个月前"
            else -> (diff / 86400 / 365).toString() + " 年前"
        }
    }
}
