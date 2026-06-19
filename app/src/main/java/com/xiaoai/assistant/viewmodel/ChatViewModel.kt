package com.xiaoai.assistant.viewmodel

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoai.assistant.network.ApiClient
import com.xiaoai.assistant.network.ChatResult
import com.xiaoai.assistant.network.ServerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 聊天消息模型
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val id: Long = System.currentTimeMillis()
)

/**
 * 聊天界面 UI 状态
 */
data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage("你好！我是小管家，点击 🎤 按钮说话，我让小爱音箱回答你 🎵", isUser = false)
    ),
    val isProcessing: Boolean = false,
    val statusText: String = "就绪",
    val statusType: String = "idle",        // idle | thinking | speaking | error
    val speakerName: String = "检测中...",
    val isListening: Boolean = false,
    val useTextMode: Boolean = false,
    val textInput: String = "",
    val interimText: String = ""
)

/**
 * 聊天 ViewModel —— 管理对话状态、语音识别状态、消息发送
 */
class ChatViewModel : ViewModel() {

    private val api = ApiClient()

    // ======== 服务器地址（由外部设置） ========
    private val _serverUrl = MutableStateFlow("http://192.168.1.15:8765")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    // ======== UI 状态 ========
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // ======== SpeechRecognizer 引用 ========
    private var speechRecognizer: SpeechRecognizer? = null

    // ======== 设置服务器地址 ========
    fun updateServerUrl(url: String) {
        _serverUrl.value = url.trimEnd('/')
    }

    // ======== 刷新服务端状态 ========
    fun refreshStatus() {
        viewModelScope.launch {
            try {
                val status: ServerStatus = api.getStatus(_serverUrl.value)
                _uiState.value = _uiState.value.copy(
                    speakerName = status.speaker,
                    statusText = if (!status.running) "❌ 无法连接服务器" else "已连接",
                    statusType = if (!status.running) "error" else "idle"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusText = "❌ 连接失败",
                    statusType = "error",
                    speakerName = "连接失败"
                )
            }
        }
    }

    // ======== 发送消息到服务器（统一入口） ========
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _uiState.value.isProcessing) return

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            statusText = "🤔 小管家思考中...",
            statusType = "thinking",
            messages = _uiState.value.messages + ChatMessage(trimmed, isUser = true)
        )

        viewModelScope.launch {
            try {
                val result: ChatResult = api.chat(_serverUrl.value, trimmed)
                if (result.error != null) {
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + ChatMessage("❌ ${result.error}", isUser = false),
                        statusText = "❌ ${result.error}",
                        statusType = "error"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + ChatMessage(result.reply, isUser = false),
                        statusText = if (result.pushedToSpeaker) "🔊 小爱音箱播报中..." else "✅ 回复完成",
                        statusType = if (result.pushedToSpeaker) "speaking" else "idle"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + ChatMessage("❌ 发送失败: ${e.localizedMessage}", isUser = false),
                    statusText = "❌ 发送失败",
                    statusType = "error"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false)
            }
        }
    }

    // ======== 初始化 SpeechRecognizer ========
    fun initSpeechRecognizer(recognizer: SpeechRecognizer) {
        speechRecognizer = recognizer
    }

    // ======== 开始语音识别 ========
    fun startListening() {
        if (speechRecognizer == null || _uiState.value.isListening) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话...")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _uiState.value = _uiState.value.copy(
                    statusText = "🎤 请说话...",
                    statusType = "thinking",
                    interimText = ""
                )
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _uiState.value = _uiState.value.copy(statusText = "🎤 识别中...")
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "没听清，请再说一遍"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有说话"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_AUDIO -> "麦克风问题"
                    else -> "语音识别错误 ($error)"
                }
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    statusText = "❌ $msg",
                    statusType = "error"
                )
            }

            override fun onResults(results: Bundle?) {
                _uiState.value = _uiState.value.copy(isListening = false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(interimText = "")
                    sendMessage(matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        interimText = matches[0],
                        statusText = "🎤 ${matches[0]}",
                        statusType = "thinking"
                    )
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)
        try {
            speechRecognizer?.startListening(intent)
            _uiState.value = _uiState.value.copy(isListening = true)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isListening = false)
        }
    }

    // ======== 停止语音识别 ========
    fun stopListening() {
        speechRecognizer?.stopListening()
        _uiState.value = _uiState.value.copy(
            isListening = false,
            statusText = "就绪",
            statusType = "idle",
            interimText = ""
        )
    }

    // ======== 文本模式切换 ========
    fun toggleTextMode() {
        _uiState.value = _uiState.value.copy(useTextMode = !_uiState.value.useTextMode)
    }

    // ======== 更新文字输入 ========
    fun updateTextInput(text: String) {
        _uiState.value = _uiState.value.copy(textInput = text)
    }

    // ======== 清除文字输入 ========
    fun clearTextInput() {
        _uiState.value = _uiState.value.copy(textInput = "")
    }

    // ======== 重置状态为就绪 ========
    fun resetStatusToIdle() {
        _uiState.value = _uiState.value.copy(
            statusText = "就绪",
            statusType = "idle"
        )
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
