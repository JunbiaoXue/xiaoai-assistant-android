package com.xiaoai.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoai.assistant.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置页面 ViewModel
 */
data class SettingsUiState(
    val serverUrl: String = "http://192.168.1.15:8765",
    val speakerName: String = "检测中...",
    val llmModel: String = "未知",
    val isConnected: Boolean = false,
    val isChecking: Boolean = false,
    val saveMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient()
    private val prefs = application.getSharedPreferences("xiaoai", 0)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            serverUrl = prefs.getString("server_url", "http://192.168.1.15:8765")
                ?: "http://192.168.1.15:8765"
        )
        checkConnection()
    }

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
    }

    fun saveServerUrl() {
        val url = _uiState.value.serverUrl.trimEnd('/')
        with(prefs.edit()) {
            putString("server_url", url)
            apply()
        }
        _uiState.value = _uiState.value.copy(saveMessage = "✅ 已保存")
        checkConnection()
    }

    fun clearSaveMessage() {
        _uiState.value = _uiState.value.copy(saveMessage = null)
    }

    fun checkConnection() {
        _uiState.value = _uiState.value.copy(isChecking = true)
        viewModelScope.launch {
            try {
                val status = api.getStatus(_uiState.value.serverUrl)
                _uiState.value = _uiState.value.copy(
                    speakerName = status.speaker,
                    llmModel = status.llmModel,
                    isConnected = status.running,
                    isChecking = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    speakerName = "连接失败",
                    isConnected = false,
                    isChecking = false
                )
            }
        }
    }
}
