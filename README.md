# 磁力搜 (cili_search)

多站点磁力聚合搜索安卓客户端。Kotlin + Jetpack Compose + Material3，薄荷青主题，全 Compose UI。

## 特性

- **并发聚合**：一次关键词同时并发查多个索引站，谁先回来谁先上屏，单个站点失败不影响其它站点
- **按源显示状态**：每个站点独立显示「搜索中 / N 条 / 无结果 / 失败」，哪个站挂了立刻看得见
- **源可开关**：设置页逐站开关，18+ 源默认关闭需手动打开
- **磁力去重 + 排序**：按 infohash 去重，按做种数降序排列
- **收藏 / 历史**：本地存储，无需账号
- **远程站点配置**：站点改版或换域名时，改仓库根目录的 `sites.json` 即可，无需重新发版

## 内置站点（全部实测可用）

| 站点 | 分组 | 接入方式 | 说明 |
|------|------|----------|------|
| 海盗湾 TPB | 综合 | apibay.org JSON | 返回 infohash，索引量最大 |
| TorrentKitty | 综合 | HTML 解析 | 行内直出磁力，自带 .net 镜像回退 |
| 動漫花園 | 动漫 | HTML 解析 | `table#topic_list`，行内直出磁力 |
| 動漫花園镜像 (acgnx) | 动漫 | RSS | 从 `show-<hash>.html` 提取 infohash |
| 蜜柑计划 (Mikan) | 动漫 | RSS | 只提供 .torrent，UI 显示「打开种子」 |
| Nyaa | 动漫 | RSS | `nyaa:infoHash` 字段 |
| Sukebei | 动漫 (18+) | RSS | 默认关闭 |
| EZTV | 剧集 | 官方 JSON | 直接给 `magnet_url` |

## 远程站点配置 `sites.json`

```json
{
  "disabled": ["apibay"],
  "sites": [
    {
      "id": "唯一ID",
      "name": "站点名",
      "group": "分组",
      "siteUrl": "https://example.com",
      "urlTemplate": "https://example.com/search?q={q}&p={page}",
      "itemSelector": "div.result-item",
      "titleSelector": "a.title",
      "titleAttr": "text",
      "magnetSelector": "a[href^='magnet:']",
      "magnetAttr": "href",
      "sizeSelector": "span.size",
      "seedersSelector": "span.seed",
      "leechersSelector": "span.leech",
      "dateSelector": "span.date",
      "detailSelector": "a.title",
      "detailAttr": "href",
      "note": "备注",
      "defaultOn": true
    }
  ]
}
```

- `urlTemplate`：`{q}` / `{query}` 关键词（URL 编码），`{page}` 页码
- 所有选择器都是标准 CSS 选择器（jsoup 语法），`*Attr` 填 `text` 表示取文本，否则取该属性
- `magnetSelector` 抓不到磁力时会尝试 `torrentSelector`（.torrent 直链）
- 只写必填项：`id` / `urlTemplate` / `itemSelector` 三个，其余有默认值

## 构建

走 GitHub Actions 云端构建，无需本地 Android SDK。push 到 main 自动出包，产物在 Actions 的 Artifacts 或 `ci-logs` 分支。

构建模板复用：`Code-xy/video_hex/.github/workflows/android-build.yml@main`

签名：仓库内置 `signing/dev-release.jks`（固定签名，保证后续版本可覆盖安装）

## 免责声明

本应用仅做搜索结果的聚合展示，不存储、不托管、不分发任何资源文件。
所有索引内容均来自第三方公开站点。
