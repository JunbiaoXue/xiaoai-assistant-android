package com.xiaoai.assistant.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoai.assistant.ui.theme.*
import com.xiaoai.assistant.network.LocalSongInfo
import com.xiaoai.assistant.network.MusicResult
import com.xiaoai.assistant.viewmodel.MusicViewModel

@Composable
fun MusicScreen(
    musicViewModel: MusicViewModel = viewModel()
) {
    val uiState by musicViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
    ) {
        // 搜索栏
        SearchBar(
            keyword = uiState.keyword,
            onKeywordChange = { musicViewModel.updateKeyword(it) },
            onSearch = { musicViewModel.searchMusic() },
            loading = uiState.loading
        )

        // 子 Tab 切换
        SubTabSelector(
            selectedTab = uiState.subTab,
            onTabSelected = { musicViewModel.switchSubTab(it) }
        )

        // 下载状态提示
        if (uiState.downloadStatus != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = NavyCard
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.downloadStatus!!,
                        color = if (uiState.downloadStatus!!.startsWith("✅") || uiState.downloadStatus!!.startsWith("🔊")) SuccessGreen else if (uiState.downloadStatus!!.startsWith("❌")) ErrorRed else AccentCyan,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { musicViewModel.clearDownloadStatus() }) {
                        Text("关闭", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // 内容区
        when (uiState.subTab) {
            0 -> OnlineContent(
                results = uiState.results,
                loading = uiState.loading,
                error = uiState.error,
                downloadingId = uiState.downloadingId,
                onDownload = { musicViewModel.downloadMusic(it) },
                onClearError = { musicViewModel.clearSearchError() }
            )
            1 -> LocalContent(
                songs = uiState.localSongs,
                loading = uiState.localLoading,
                error = uiState.localError,
                playingFile = uiState.playingLocal,
                onPlay = { musicViewModel.playLocalMusic(it) },
                onRefresh = { musicViewModel.loadLocalMusic() },
                onClearError = { musicViewModel.clearLocalError() }
            )
        }
    }
}

@Composable
private fun SearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    loading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("搜索歌曲或歌手...", color = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = AiBorder,
                cursorColor = AccentCyan,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledIconButton(
            onClick = onSearch,
            enabled = keyword.isNotBlank() && !loading,
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = AccentCyan,
                contentColor = NavyDark,
                disabledContainerColor = AiBorder
            )
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = NavyDark
                )
            } else {
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
        }
    }
}

@Composable
private fun SubTabSelector(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(NavyCard, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        SubTabButton("在线搜索", selectedTab == 0, Modifier.weight(1f)) { onTabSelected(0) }
        SubTabButton("本地音乐", selectedTab == 1, Modifier.weight(1f)) { onTabSelected(1) }
    }
}

@Composable
private fun SubTabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) AccentCyan else NavyCard,
        contentColor = if (selected) NavyDark else TextSecondary
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, fontSize = 14.sp)
        }
    }
}

@Composable
private fun OnlineContent(
    results: List<MusicResult>,
    loading: Boolean,
    error: String?,
    downloadingId: String?,
    onDownload: (MusicResult) -> Unit,
    onClearError: () -> Unit
) {
    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentCyan)
        }
    } else if (error != null) {
        ErrorCard(error, onClearError)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(results) { song ->
                SongCard(
                    name = song.name,
                    artist = song.artist,
                    album = song.album,
                    isDownloading = downloadingId == song.id,
                    onClick = { onDownload(song) },
                    icon = { Icon(Icons.Default.Download, "下载", tint = AccentCyan, modifier = Modifier.size(20.dp)) }
                )
            }
        }
    }
}

@Composable
private fun LocalContent(
    songs: List<LocalSongInfo>,
    loading: Boolean,
    error: String?,
    playingFile: String?,
    onPlay: (LocalSongInfo) -> Unit,
    onRefresh: () -> Unit,
    onClearError: () -> Unit
) {
    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentCyan)
        }
    } else if (error != null) {
        ErrorCard(error, onClearError)
    } else {
        Column {
            // 刷新按钮 + 歌曲数
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${songs.size} 首歌曲", color = TextSecondary, fontSize = 13.sp)
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(16.dp), tint = AccentCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("刷新", color = AccentCyan, fontSize = 13.sp)
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(songs) { song ->
                    SongCard(
                        name = song.name,
                        artist = song.artist,
                        album = null,
                        isPlaying = playingFile == song.file,
                        onClick = { onPlay(song) },
                        icon = {
                            Icon(
                                Icons.Default.PlayArrow, "播放",
                                tint = if (playingFile == song.file) SuccessGreen else AccentCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SongCard(
    name: String,
    artist: String,
    album: String?,
    isDownloading: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = NavyCard
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    color = if (isPlaying) SuccessGreen else TextPrimary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    artist + if (album != null && album.isNotBlank()) " · $album" else "",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = AccentCyan
                )
            } else {
                icon()
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(12.dp),
        color = ErrorRed.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(error, color = ErrorRed, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("关闭", color = TextSecondary)
            }
        }
    }
}
