package com.xiaoai.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoai.assistant.network.ApiClient
import com.xiaoai.assistant.network.MusicResult
import com.xiaoai.assistant.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 音乐搜索下载 Tab
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTab(
    serverUrl: String,
    api: ApiClient,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var keyword by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<MusicResult>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var downloadStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎵 ",
                fontSize = 28.sp
            )
            Text(
                text = "音乐搜索",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 状态提示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "搜索网易云音乐，下载后推送到音箱播放",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 搜索栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = { Text("输入歌名或歌手...", color = TextSecondary) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentCyan,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = Color(0x1AFFFFFF),
                    focusedContainerColor = NavyCard,
                    unfocusedContainerColor = NavyCard
                )
            )
            Button(
                onClick = {
                    if (keyword.isBlank() || loading) return@Button
                    loading = true
                    error = null
                    results = emptyList()
                    scope.launch {
                        val res = api.searchMusic(serverUrl, keyword.trim())
                        if (res.isEmpty()) {
                            error = "未找到相关歌曲，换个关键词试试"
                        }
                        results = res
                        loading = false
                    }
                },
                enabled = !loading && keyword.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = "搜索", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("搜索", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 错误提示
        if (error != null) {
            Text(
                text = error!!,
                fontSize = 13.sp,
                color = ErrorRed,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // 下载状态
        if (downloadStatus != null) {
            Text(
                text = downloadStatus!!,
                fontSize = 13.sp,
                color = WarningYellow,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 搜索结果列表
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0DFFFFFF))
        ) {
            if (results.isEmpty() && !loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔍 搜索你想听的歌",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(results, key = { it.id }) { song ->
                        SongCard(
                            song = song,
                            isDownloading = downloadingId == song.id,
                            onDownload = {
                                downloadingId = song.id
                                downloadStatus = "正在下载 ${song.name}..."
                                scope.launch {
                                    val result = api.downloadMusic(serverUrl, song.id, song.name, song.artist)
                                    if (result != null) {
                                        downloadStatus = "✅ ${song.name} 已下载并推送到音箱"
                                    } else {
                                        downloadStatus = "❌ 下载失败，请重试"
                                    }
                                    downloadingId = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongCard(
    song: MusicResult,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyMid)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (song.duration.isNotEmpty()) {
                        Text(
                            text = "⏱ ${song.duration}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (song.size.isNotEmpty()) {
                        Text(
                            text = "💾 ${song.size}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 下载按钮
            Button(
                onClick = onDownload,
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDownloading) NavyMid else AccentCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AccentCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Download, contentDescription = "下载", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("下载播放", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
