package com.seagull.cili.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * 站内预览：拿洗好的片名去 iyf.tv 搜，直接看片。
 * 用的就是站点自己的 /search 路由，WebView 里跑它的前端。
 */
class PreviewActivity : ComponentActivity() {

    private var web: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val keyword = intent.getStringExtra(EXTRA_KEYWORD).orEmpty()
        val siteName = intent.getStringExtra(EXTRA_SITE).orEmpty().ifEmpty { "在线预览" }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(14), dp(10), dp(8), dp(10))
        }
        val title = TextView(this).apply {
            text = if (keyword.isEmpty()) siteName else siteName + " · " + keyword
            textSize = 14f
            setTextColor(Color.parseColor("#101817"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        bar.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val openOut = TextView(this).apply {
            text = "浏览器打开"
            textSize = 13f
            setTextColor(Color.parseColor("#0E9F94"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }
        }
        bar.addView(openOut)

        val close = TextView(this).apply {
            text = "关闭"
            textSize = 13f
            setTextColor(Color.parseColor("#48605B"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { finish() }
        }
        bar.addView(close)

        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = ProgressBar.GONE
        }

        val wv = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress in 1..99) ProgressBar.VISIBLE else ProgressBar.GONE
                }
            }
        }
        web = wv

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)))
        }

        // 顶部系统栏留白
        root.setOnApplyWindowInsetsListener { v, insets ->
            @Suppress("DEPRECATION")
            v.setPadding(0, insets.systemWindowInsetTop, 0, 0)
            insets
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(root, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(wv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(container)

        if (url.isNotEmpty()) wv.loadUrl(url)
    }

    override fun onBackPressed() {
        val w = web
        if (w != null && w.canGoBack()) w.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        web?.destroy()
        web = null
        super.onDestroy()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_KEYWORD = "keyword"
        const val EXTRA_SITE = "site"
    }
}
