package com.xiaoai.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoai.assistant.network.ApiClient
import com.xiaoai.assistant.network.LocalSongInfo
import com.xiaoai.assistant.network.MusicResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 音乐页面 UI 状态
 */
data class MusicUiState(
    val keyword: String = "",
    val results: List<MusicResult> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val downloadingId: String? = null,
    val downloadStatus: String? = null,
    // 本地音乐
    val localSongs: List<LocalSongInfo> = emptyList(),
    val localLoading: Boolean = false,
    val localError: String? = null,
    val playingLocal: String? = null,
    // 子 Tab
    val subTab: Int = 0  // 0=在线搜索, 1=本地音乐
)

/**
 * 音乐页 ViewModel — 管理 ALL 音乐相关状态
 */
class MusicViewModel : ViewModel() {

    private val api = ApiClient()

    // ======== 服务器地址（由外部设置） ========
    private val _serverUrl = MutableStateFlow("http://192.168.1.15:8765")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    // ======== UI 状态 ========
    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    // ======== 设置服务器地址 ========
    fun updateServerUrl(url: String) {
        _serverUrl.value = url.trimEnd('/')
    }

    // ======== 更新搜索关键词 ========
    fun updateKeyword(text: String) {
        _uiState.value = _uiState.value.copy(keyword = text)
    }

    // ======== 切换子 Tab ========
    fun switchSubTab(tab: Int) {
        _uiState.value = _uiState.value.copy(subTab = tab)
        // 首次切换到本地音乐页时自动加载
        if (tab == 1 && _uiState.value.localSongs.isEmpty()
            && !_uiState.value.localLoading && _uiState.value.localError == null) {
            loadLocalMusic()
        }
    }

    // ======== 搜索音乐 ========
    fun searchMusic() {
        val keyword = _uiState.value.keyword.trim()
        if (keyword.isBlank() || _uiState.value.loading) return

        _uiState.value = _uiState.value.copy(
            loading = true,
            error = null,
            results = emptyList(),
            downloadStatus = null
        )

        viewModelScope.launch {
            try {
                val results: List<MusicResult> = api.searchMusic(_serverUrl.value, keyword)
                _uiState.value = _uiState.value.copy(
                    results = results,
                    error = if (results.isEmpty()) "未找到相关歌曲，换个关键词试试" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "搜索失败: ${e.localizedMessage ?: "请检查网络"}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(loading = false)
            }
        }
    }

    // ======== 下载音乐并推送到音箱 ========
    fun downloadMusic(song: MusicResult) {
        _uiState.value = _uiState.value.copy(
            downloadingId = song.id,
            downloadStatus = "正在下载 ${song.name}..."
        )

        viewModelScope.launch {
            try {
                val result: String? = api.downloadMusic(
                    _serverUrl.value, song.id, song.name, song.artist
                )
                _uiState.value = _uiState.value.copy(
                    downloadStatus = if (result != null) {
                        "✅ ${song.name} 已下载并推送到音箱"
                    } else {
                        "❌ 下载失败，请重试"
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    downloadStatus = "❌ 下载失败: ${e.localizedMessage}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(downloadingId = null)
            }
        }
    }

    // ======== 加载本地音乐列表 ========
    fun loadLocalMusic() {
        if (_uiState.value.localLoading) return

        _uiState.value = _uiState.value.copy(localLoading = true, localError = null)

        viewModelScope.launch {
            try {
                val songs: List<LocalSongInfo> = api.getLocalMusic(_serverUrl.value)
                _uiState.value = _uiState.value.copy(
                    localSongs = songs,
                    localError = if (songs.isEmpty()) "暂无本地音乐，先在线下载吧" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    localError = "加载失败: ${e.localizedMessage ?: "请检查服务器"}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(localLoading = false)
            }
        }
    }

    // ======== 播放本地音乐 ========
    fun playLocalMusic(song: LocalSongInfo) {
        _uiState.value = _uiState.value.copy(
            playingLocal = song.file,
            downloadStatus = "正在播放: ${song.name}..."
        )

        viewModelScope.launch {
            try {
                val success: Boolean = api.playLocalMusic(
                    _serverUrl.value, song.file, song.name, song.artist
                )
                _uiState.value = _uiState.value.copy(
                    downloadStatus = if (success) "🔊 正在播放: ${song.name}" else "❌ 播放失败"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    downloadStatus = "❌ 播放失败: ${e.localizedMessage}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(playingLocal = null)
            }
        }
    }

    // ======== 清除下载状态 ========
    fun clearDownloadStatus() {
        _uiState.value = _uiState.value.copy(downloadStatus = null)
    }

    // ======== 清除搜索错误 ========
    fun clearSearchError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ======== 清除本地错误 ========
    fun clearLocalError() {
        _uiState.value = _uiState.value.copy(localError = null)
    }
}
