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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoai.assistant.network.ApiClient
import com.xiaoai.assistant.network.LocalSongInfo
import com.xiaoai.assistant.network.MusicResult
import com.xiaoai.assistant.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 音乐 Tab：在线搜索 + 本地音乐
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTab(
    serverUrl: String,
    api: ApiClient
) {
    // 使用自己的 CoroutineScope，避免跨组件传递导致的闭包问题
    val scope = rememberCoroutineScope()
    var keyword by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<MusicResult>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var downloadStatus by remember { mutableStateOf<String?>(null) }

    // 本地音乐
    var localSongs by remember { mutableStateOf(listOf<LocalSongInfo>()) }
    var localLoading by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var playingLocal by remember { mutableStateOf<String?>(null) }

    // 当前子 Tab: 0=在线搜索, 1=本地音乐
    var subTab by remember { mutableStateOf(0) }

    // 加载本地音乐
    fun loadLocalMusic() {
        localLoading = true
        localError = null
        scope.launch {
            try {
                val list = api.getLocalMusic(serverUrl)
                localSongs = list
                if (list.isEmpty() && localError == null) {
                    localError = "暂无本地音乐，先在线下载吧"
                }
            } catch (e: Exception) {
                localError = "加载失败: ${e.localizedMessage ?: "请检查服务器"}"
            } finally {
                localLoading = false
            }
        }
    }

    // 播放本地音乐
    fun playLocal(song: LocalSongInfo) {
        playingLocal = song.file
        downloadStatus = "正在播放: ${song.name}..."
        scope.launch {
            try {
                val success = api.playLocalMusic(serverUrl, song.file, song.name, song.artist)
                downloadStatus = if (success) {
                    "🔊 正在播放: ${song.name}"
                } else {
                    "❌ 播放失败"
                }
            } catch (e: Exception) {
                downloadStatus = "❌ 播放失败: ${e.localizedMessage}"
            } finally {
                playingLocal = null
            }
        }
    }

    // 首次进入本地音乐页时加载
    LaunchedEffect(subTab) {
        if (subTab == 1 && localSongs.isEmpty() && !localLoading && localError == null) {
            loadLocalMusic()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🎵 ", fontSize = 28.sp)
            Text(
                text = "音乐",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 下载/播放状态提示
        if (downloadStatus != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = NavyMid)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (downloadingId != null || playingLocal != null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AccentCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = downloadStatus!!,
                        fontSize = 13.sp,
                        color = if (downloadStatus!!.startsWith("❌")) ErrorRed else WarningYellow
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 子 Tab 切换器
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NavyCard)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 在线搜索按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (subTab == 0) AccentCyan else Color.Transparent)
                    .clickable { subTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (subTab == 0) Color.Black else TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "在线搜索",
                        fontSize = 14.sp,
                        fontWeight = if (subTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (subTab == 0) Color.Black else TextSecondary
                    )
                }
            }
            // 本地音乐按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (subTab == 1) AccentCyan else Color.Transparent)
                    .clickable { subTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (subTab == 1) Color.Black else TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "本地音乐",
                        fontSize = 14.sp,
                        fontWeight = if (subTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (subTab == 1) Color.Black else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ===== 在线搜索 =====
        if (subTab == 0) {
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
                        val searchKeyword = keyword.trim()
                        if (searchKeyword.isBlank() || loading) return@Button
                        loading = true
                        error = null
                        results = emptyList()
                        downloadStatus = null
                        scope.launch {
                            try {
                                val res = api.searchMusic(serverUrl, searchKeyword)
                                results = res
                                if (res.isEmpty() && error == null) {
                                    error = "未找到相关歌曲，换个关键词试试"
                                }
                            } catch (e: Exception) {
                                error = "搜索失败: ${e.localizedMessage ?: "请检查网络"}"
                            } finally {
                                loading = false
                            }
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

            if (error != null) {
                Text(
                    text = error!!,
                    fontSize = 13.sp,
                    color = ErrorRed,
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("搜索你想听的歌", fontSize = 16.sp, color = TextSecondary)
                        }
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
                                    val currentSong = song  // 捕获当前值
                                    downloadingId = currentSong.id
                                    downloadStatus = "正在下载 ${currentSong.name}..."
                                    scope.launch {
                                        try {
                                            val result = api.downloadMusic(serverUrl, currentSong.id, currentSong.name, currentSong.artist)
                                            downloadStatus = if (result != null) {
                                                "✅ ${currentSong.name} 已下载并推送到音箱"
                                            } else {
                                                "❌ 下载失败，请重试"
                                            }
                                        } catch (e: Exception) {
                                            downloadStatus = "❌ 下载失败: ${e.localizedMessage}"
                                        } finally {
                                            downloadingId = null
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // ===== 本地音乐 =====
        if (subTab == 1) {
            // 刷新按钮 + 歌曲数量
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (localSongs.isNotEmpty()) {
                    Text(
                        text = "共 ${localSongs.size} 首",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                TextButton(onClick = { loadLocalMusic() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "刷新",
                        modifier = Modifier.size(16.dp),
                        tint = AccentCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("刷新", color = AccentCyan, fontSize = 13.sp)
                }
            }

            if (localError != null) {
                Text(
                    text = localError!!,
                    fontSize = 13.sp,
                    color = ErrorRed,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0DFFFFFF))
            ) {
                if (localLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentCyan, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("加载本地音乐...", fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                } else if (localSongs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📂", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("暂无本地音乐", fontSize = 16.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("先去「在线搜索」下载歌曲吧", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(localSongs, key = { it.file }) { song ->
                            LocalSongCard(
                                song = song,
                                isPlaying = playingLocal == song.file,
                                onPlay = { playLocal(song) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// =============================================
// 在线歌曲卡片
// =============================================
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
            // 音乐图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                        Text(text = "⏱ ${song.duration}", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (song.size.isNotEmpty()) {
                        Text(text = "💾 ${song.size}", fontSize = 11.sp, color = TextSecondary)
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

// =============================================
// 本地歌曲卡片
// =============================================
@Composable
fun LocalSongCard(
    song: LocalSongInfo,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) AccentCyan.copy(alpha = 0.1f) else NavyMid
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 播放图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isPlaying) AccentCyan.copy(alpha = 0.3f)
                        else SuccessGreen.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AccentCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = SuccessGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPlaying) AccentCyan else TextPrimary,
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
                    if (song.size.isNotEmpty()) {
                        Text(text = "💾 ${song.size}", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 播放按钮
            IconButton(
                onClick = onPlay,
                enabled = !isPlaying
            ) {
                Icon(
                    if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayCircleFilled,
                    contentDescription = if (isPlaying) "播放中" else "播放",
                    tint = if (isPlaying) AccentCyan else SuccessGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
