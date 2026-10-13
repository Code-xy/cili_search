package com.seagull.cili.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object Actions {

    fun copy(ctx: Context, text: String, label: String = "已复制") {
        try {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("magnet", text))
            toast(ctx, label)
        } catch (e: Exception) {
            toast(ctx, "复制失败")
        }
    }

    fun share(ctx: Context, text: String) {
        try {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            ctx.startActivity(Intent.createChooser(i, "分享"))
        } catch (e: Exception) {
            toast(ctx, "没有可分享的应用")
        }
    }

    fun open(ctx: Context, url: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            toast(ctx, "没有能打开该链接的应用")
        }
    }

    fun toast(ctx: Context, msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }
}
