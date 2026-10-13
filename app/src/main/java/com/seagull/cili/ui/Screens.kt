package com.seagull.cili.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import android.content.Intent
import com.seagull.cili.data.MagnetItem
import com.seagull.cili.data.PreviewSite
import com.seagull.cili.data.TitleClean
import com.seagull.cili.data.Util

@Composable
fun RootScaffold() {
    val vm: SearchViewModel = viewModel()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf("磁力搜", "我的收藏", "设置")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(titles[tab], fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("搜索") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    label = { Text("收藏") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("设置") }
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> SearchScreen(vm)
                1 -> FavScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
    }
}

@Composable
private fun SearchScreen(vm: SearchViewModel) {
    val ctx = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val query by vm.query.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val favs by vm.favs.collectAsStateWithLifecycle()
    val tip by vm.tip.collectAsStateWithLifecycle()
    val previewSite by vm.previewSite.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<MagnetItem?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(4.dp))
        SearchField(
            value = query,
            onValueChange = vm::setQuery,
            onSearch = {
                keyboard?.hide()
                vm.search()
            },
            onClear = { vm.setQuery("") }
        )
        Spacer(Modifier.height(10.dp))

        if (status.isNotEmpty()) {
            SourceStatusStrip(status)
            Spacer(Modifier.height(10.dp))
        }

        when {
            items.isEmpty() && !searching -> EmptyZone(
                history = history,
                tip = tip,
                onPick = { vm.search(it) },
                onClear = { vm.clearHistory() }
            )

            items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.key }) { item ->
                    ResultCard(
                        item = item,
                        fav = favs.any { it.key == item.key },
                        onFav = {
                            val added = vm.toggleFav(item)
                            Actions.toast(ctx, if (added) "已收藏" else "已取消收藏")
                        },
                        onCopy = { item.magnet?.let { Actions.copy(ctx, it, "磁力链接已复制") } },
                        onShare = { Actions.share(ctx, item.magnet ?: item.torrentUrl ?: item.detailUrl ?: item.title) },
                        onOpen = {
                            val target = item.magnet ?: item.torrentUrl ?: item.detailUrl
                            if (target != null) Actions.open(ctx, target)
                        },
                        onPreview = { openPreview(ctx, previewSite, item.title) },
                        onDetail = { detail = item }
                    )
                }
                item {
                    if (searching) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        TextButton(
                            onClick = { vm.search(null, true) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("加载更多") }
                    }
                }
            }
        }
    }

    detail?.let { d ->
        DetailSheet(
            item = d,
            onDismiss = { detail = null },
            onCopy = {
                val link = d.magnet ?: d.torrentUrl
                if (link != null) Actions.copy(ctx, link, if (d.magnet != null) "磁力链接已复制" else "种子链接已复制")
            },
            onPreview = { openPreview(ctx, previewSite, d.title) },
            onShare = { Actions.share(ctx, d.magnet ?: d.torrentUrl ?: d.detailUrl ?: d.title) },
            onOpen = {
                val t = d.magnet ?: d.torrentUrl ?: d.detailUrl
                if (t != null) Actions.open(ctx, t)
            }
        )
    }
}

@Composable
private fun EmptyZone(
    history: List<String>,
    tip: String,
    onPick: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "输入关键词，一次搜遍所有站点",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.5.sp
        )
        if (tip.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(tip, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
        if (history.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("搜索历史", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClear) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("清空", fontSize = 12.sp)
                }
            }
            for (h in history) {
                AssistChip(
                    onClick = { onPick(h) },
                    label = { Text(h, fontSize = 12.5.sp) },
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun FavScreen(vm: SearchViewModel) {
    val ctx = LocalContext.current
    val favs by vm.favs.collectAsStateWithLifecycle()
    val previewSite by vm.previewSite.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<MagnetItem?>(null) }

    if (favs.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("还没有收藏", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(favs, key = { it.key }) { item ->
            ResultCard(
                item = item,
                fav = true,
                onFav = {
                    vm.toggleFav(item)
                    Actions.toast(ctx, "已取消收藏")
                },
                onCopy = { item.magnet?.let { Actions.copy(ctx, it, "磁力链接已复制") } },
                onShare = { Actions.share(ctx, item.magnet ?: item.torrentUrl ?: item.detailUrl ?: item.title) },
                onOpen = {
                    val t = item.magnet ?: item.torrentUrl ?: item.detailUrl
                    if (t != null) Actions.open(ctx, t)
                },
                onPreview = { openPreview(ctx, previewSite, item.title) },
                onDetail = { detail = item }
            )
        }
    }

    detail?.let { d ->
        DetailSheet(
            item = d,
            onDismiss = { detail = null },
            onCopy = {
                val link = d.magnet ?: d.torrentUrl
                if (link != null) Actions.copy(ctx, link, if (d.magnet != null) "磁力链接已复制" else "种子链接已复制")
            },
            onPreview = { openPreview(ctx, previewSite, d.title) },
            onShare = { Actions.share(ctx, d.magnet ?: d.torrentUrl ?: d.detailUrl ?: d.title) },
            onOpen = {
                val t = d.magnet ?: d.torrentUrl ?: d.detailUrl
                if (t != null) Actions.open(ctx, t)
            }
        )
    }
}

@Composable
private fun SettingsScreen(vm: SearchViewModel) {
    val ctx = LocalContext.current
    val all by vm.allSources.collectAsStateWithLifecycle()
    val tip by vm.tip.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)
    ) {
        Text("搜索站点", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "关掉的站点不参与并发搜索。组内站越多越慢，建议常留 4~6 个。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        for (src in all) {
            val enabled = vm.isEnabled(src)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            src.name + (if (src.id == "sukebei") "  (18+)" else ""),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            src.group + " · " + src.note,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = { vm.toggleSource(src, it) })
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(14.dp))

        Text("站点配置", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "站点改版或换域名时，从远程 sites.json 拉一份新配置就能修好，不用重装。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.refreshConfig() }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("更新配置")
            }
            OutlinedButton(
                onClick = {
                    vm.setQuery("")
                    vm.clearHistory()
                    Actions.toast(ctx, "已清空搜索历史")
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("清空历史") }
        }
        if (tip.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(24.dp))
        Text("关于", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "磁力搜只做聚合展示：并发查询公开索引站，把磁力/种子链接整理给你。" +
                    "所有内容都在第三方站点，本应用不存储任何资源、不提供下载。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun DetailSheet(
    item: MagnetItem,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onPreview: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)
        ) {
            Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(item.sourceName, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer)
                if (item.sizeBytes > 0) Pill(item.sizeText, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                if (item.seeders >= 0) Pill("做种 " + item.seeders, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                if (item.leechers >= 0) Pill("下载 " + item.leechers, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
            }
            if (item.hash.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                LabeledText("INFO HASH", item.hash)
            }
            val t = Util.relTime(item.publishedAt)
            if (t.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LabeledText("发布时间", t)
            }
            if (item.detailUrl != null) {
                Spacer(Modifier.height(8.dp))
                LabeledText("详情页", item.detailUrl)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = onPreview, shape = RoundedCornerShape(12.dp)) {
                    Text("预览", fontSize = 13.sp)
                }
                TextButton(onClick = onCopy) { Text("复制链接", fontSize = 13.sp) }
                TextButton(onClick = onOpen) { Text("打开", fontSize = 13.sp) }
                TextButton(onClick = onShare) { Text("分享", fontSize = 13.sp) }
            }
        }
    }
}

private fun openPreview(ctx: Context, site: PreviewSite, title: String) {
    val q = TitleClean.clean(title)
    if (q.isEmpty()) {
        Actions.toast(ctx, "标题洗不出关键词，换个条目试试")
        return
    }
    val url = site.searchUrlTemplate.replace("{q}", Util.enc(q))
    try {
        ctx.startActivity(
            Intent(ctx, PreviewActivity::class.java)
                .putExtra(PreviewActivity.EXTRA_URL, url)
                .putExtra(PreviewActivity.EXTRA_KEYWORD, q)
                .putExtra(PreviewActivity.EXTRA_SITE, site.name)
        )
    } catch (e: Exception) {
        Actions.open(ctx, url)
    }
}

@Composable
private fun LabeledText(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 12.5.sp)
    }
}
