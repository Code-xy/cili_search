package com.seagull.cili.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seagull.cili.data.MagnetItem
import com.seagull.cili.data.SourceState
import com.seagull.cili.data.SourceStatus
import com.seagull.cili.data.Util

private val SEED_OK = Color(0xFF0F9D58)
private val SEED_BAD = Color(0xFFD93025)

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("片名 / 番名 / 关键词", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) { Icon(Icons.Filled.Clear, contentDescription = "清空") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

@Composable
fun Pill(text: String, fg: Color, bg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(7.dp)) {
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SourceStatusStrip(list: List<SourceStatus>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (s in list) {
            val cs = MaterialTheme.colorScheme
            val bg: Color = when (s.state) {
                SourceState.RUNNING -> cs.primaryContainer
                SourceState.DONE -> cs.secondaryContainer
                SourceState.EMPTY -> cs.surfaceVariant
                SourceState.FAILED -> cs.errorContainer
                SourceState.IDLE -> cs.surfaceVariant
            }
            val fg: Color = when (s.state) {
                SourceState.RUNNING -> cs.onPrimaryContainer
                SourceState.DONE -> cs.onSecondaryContainer
                SourceState.EMPTY -> cs.onSurfaceVariant
                SourceState.FAILED -> cs.onErrorContainer
                SourceState.IDLE -> cs.onSurfaceVariant
            }
            val label = when (s.state) {
                SourceState.RUNNING -> s.name + " 搜索中"
                SourceState.DONE -> s.name + " " + s.count
                SourceState.EMPTY -> s.name + " 无结果"
                SourceState.FAILED -> s.name + " 失败"
                SourceState.IDLE -> s.name
            }
            Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (s.state == SourceState.RUNNING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(9.dp),
                            strokeWidth = 1.4.dp,
                            color = fg
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(label, color = fg, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun ResultCard(
    item: MagnetItem,
    fav: Boolean,
    onFav: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onDetail: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onFav, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "收藏",
                        tint = if (fav) cs.tertiary else cs.outline
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Pill(item.sourceName, cs.onPrimaryContainer, cs.primaryContainer)
                if (item.sizeBytes > 0) Pill(item.sizeText, cs.onSurfaceVariant, cs.surfaceVariant)
                if (item.seeders >= 0) {
                    val c = if (item.seeders > 0) SEED_OK else SEED_BAD
                    Pill("种 " + item.seeders, c, c.copy(alpha = 0.12f))
                }
                if (item.leechers >= 0) Pill("吸 " + item.leechers, cs.onSurfaceVariant, cs.surfaceVariant)
                if (item.category.isNotEmpty()) Pill(item.category, cs.onSecondaryContainer, cs.secondaryContainer)
                val t = Util.relTime(item.publishedAt)
                if (t.isNotEmpty()) Pill(t, cs.onSurfaceVariant, cs.surfaceVariant)
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.magnet != null) {
                    FilledTonalButton(onClick = onCopy, shape = RoundedCornerShape(12.dp)) {
                        Text("复制磁力", fontSize = 12.5.sp)
                    }
                } else {
                    FilledTonalButton(onClick = onOpen, shape = RoundedCornerShape(12.dp)) {
                        Text("打开种子", fontSize = 12.5.sp)
                    }
                }
                FilledTonalButton(onClick = onPreview, shape = RoundedCornerShape(12.dp)) {
                    Text("预览", fontSize = 12.5.sp)
                }
                TextButton(onClick = onShare) { Text("分享", fontSize = 12.5.sp) }
                Box(Modifier.weight(1f))
                TextButton(onClick = onDetail) { Text("详情", fontSize = 12.5.sp) }
            }
        }
    }
}
