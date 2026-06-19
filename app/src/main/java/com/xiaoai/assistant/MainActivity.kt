package com.xiaoai.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.xiaoai.assistant.network.ApiClient
import com.xiaoai.assistant.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XiaoaiApp()
        }
    }
}

// =============================================
// 主题包装
// =============================================
@Composable
fun XiaoaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AccentCyan,
            onPrimary = Color.Black,
            background = NavyDark,
            onBackground = TextPrimary,
            surface = NavyCard,
            onSurface = TextPrimary,
            secondary = TextSecondary,
            onSecondary = TextPrimary,
            error = ErrorRed,
            onError = Color.White,
            surfaceVariant = NavyMid,
            onSurfaceVariant = TextSecondary,
        ),
        content = content
    )
}

// =============================================
// 消息模型
// =============================================
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val id: Long = System.currentTimeMillis()
)

// =============================================
// 主页面
// =============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XiaoaiApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient() }

    // ======== 所有状态统一在这里管理 ========
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var serverUrl by remember { mutableStateOf("http://192.168.1.15:8765") }
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("就绪") }
    var statusType by remember { mutableStateOf("idle") }
    var speakerName by remember { mutableStateOf("检测中...") }
    var showSettings by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var useTextMode by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var interimText by remember { mutableStateOf("") }
    var currentTab by remember { mutableStateOf(0) }

    // SharedPreferences
    val prefs = remember { context.getSharedPreferences("xiaoai", 0) }

    // 读取保存的服务器地址
    LaunchedEffect(Unit) {
        serverUrl = prefs.getString("server_url", "http://192.168.1.15:8765") ?: "http://192.168.1.15:8765"
    }

    // 刷新状态
    fun refreshStatus() {
        scope.launch {
            try {
                val status = api.getStatus(serverUrl)
                speakerName = status.speaker
                if (!status.running) {
                    statusText = "❌ 无法连接服务器"
                    statusType = "error"
                } else {
                    statusText = "已连接"
                    statusType = "idle"
                }
            } catch (e: Exception) {
                statusText = "❌ 连接失败"
                statusType = "error"
                speakerName = "连接失败"
            }
        }
    }

    // 首次加载
    LaunchedEffect(serverUrl) {
        refreshStatus()
        messages = listOf(
            ChatMessage("你好！我是小管家，点击 🎤 按钮说话，我让小爱音箱回答你 🎵", isUser = false)
        )
    }

    // ======== 发送消息到服务器（统一入口） ========
    fun sendToServer(text: String) {
        if (text.isBlank() || isProcessing) return
        isProcessing = true
        statusText = "🤔 小管家思考中..."
        statusType = "thinking"
        messages = messages + ChatMessage(text.trim(), isUser = true)

        scope.launch {
            try {
                val result = api.chat(serverUrl, text.trim())
                if (result.error != null) {
                    messages = messages + ChatMessage("❌ ${result.error}", isUser = false)
                    statusText = "❌ ${result.error}"
                    statusType = "error"
                } else {
                    messages = messages + ChatMessage(result.reply, isUser = false)
                    if (result.pushedToSpeaker) {
                        statusText = "🔊 小爱音箱播报中..."
                        statusType = "speaking"
                    } else {
                        statusText = "✅ 回复完成"
                        statusType = "idle"
                    }
                }
            } catch (e: Exception) {
                messages = messages + ChatMessage("❌ 发送失败: ${e.localizedMessage}", isUser = false)
                statusText = "❌ 发送失败"
                statusType = "error"
            }
            isProcessing = false
        }
    }

    // ======== 语音识别 ========
    val speechRecognizer = remember {
        try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            null
        }
    }

    // 生命周期监听：Activity 销毁时释放
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                speechRecognizer?.destroy()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            Toast.makeText(context, "设备不支持语音识别", Toast.LENGTH_SHORT).show()
            return
        }
        if (isListening) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话...")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                statusText = "🎤 请说话..."
                statusType = "thinking"
                interimText = ""
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                statusText = "🎤 识别中..."
            }
            override fun onError(error: Int) {
                isListening = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "没听清，请再说一遍"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有说话"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_AUDIO -> "麦克风问题"
                    else -> "语音识别错误 ($error)"
                }
                statusText = "❌ $msg"
                statusType = "error"
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    interimText = ""
                    sendToServer(text)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    interimText = matches[0]
                    statusText = "🎤 $interimText"
                    statusType = "thinking"
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(listener)
        try {
            speechRecognizer.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(context, "语音识别启动失败", Toast.LENGTH_SHORT).show()
        }
    }

    // 麦克风权限请求
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            Toast.makeText(context, "需要麦克风权限才能语音输入", Toast.LENGTH_SHORT).show()
        }
    }

    // 自动滚到底部
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // =========================================
    // UI 布局
    // =========================================
    XiaoaiTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NavyDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ---- Tab 导航栏（始终显示） ----
                TabRow(
                    selectedTabIndex = currentTab,
                    containerColor = NavyDark,
                    contentColor = AccentCyan
                ) {
                    Tab(selected = currentTab == 0, onClick = { currentTab = 0 }, text = { Text("💬 对话") })
                    Tab(selected = currentTab == 1, onClick = { currentTab = 1 }, text = { Text("🎵 音乐") })
                }

                // ---- 对话页 ----
                if (currentTab == 0) {
                    // ---- 标题栏 ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎙️ ", fontSize = 28.sp)
                        Text(
                            text = "小管家 AI 助手",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置", tint = TextSecondary)
                        }
                    }

                    // ---- 状态栏 ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dotColor = when (statusType) {
                            "idle" -> SuccessGreen
                            "thinking" -> AccentCyan
                            "speaking" -> WarningYellow
                            "error" -> ErrorRed
                            else -> TextSecondary
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = statusText, fontSize = 13.sp, color = TextSecondary)
                    }

                    // ---- 音箱状态 ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (speakerName.contains("失败")) "🔇 音箱: $speakerName" else "🔊 音箱: $speakerName",
                            fontSize = 12.sp,
                            color = if (speakerName.contains("失败") || speakerName == "检测中...") TextSecondary else SuccessGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ---- 对话区 ----
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NavyCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0DFFFFFF))
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                ChatBubble(message = msg)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ---- 语音 / 文字输入区 ----
                    if (isListening && interimText.isNotBlank()) {
                        Text(
                            text = "🎤 $interimText",
                            fontSize = 14.sp,
                            color = AccentCyan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NavyMid, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (useTextMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("输入消息...", color = TextSecondary) },
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
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (!isProcessing && textInput.isNotBlank()) {
                                            sendToServer(textInput.trim())
                                            textInput = ""
                                        }
                                    }
                                )
                            )
                            Button(
                                onClick = {
                                    if (!isProcessing && textInput.isNotBlank()) {
                                        sendToServer(textInput.trim())
                                        textInput = ""
                                    }
                                },
                                enabled = !isProcessing && textInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("发送", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 3.dp,
                                        color = if (isListening) ErrorRed else AccentCyan,
                                        shape = CircleShape
                                    )
                                    .background(
                                        if (isListening) Color(0x33FF5252) else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isProcessing) return@IconButton
                                        if (isListening) {
                                            speechRecognizer?.stopListening()
                                            isListening = false
                                            statusText = "就绪"
                                            statusType = "idle"
                                            interimText = ""
                                        } else {
                                            if (ContextCompat.checkSelfPermission(
                                                    context, Manifest.permission.RECORD_AUDIO
                                                ) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                startListening()
                                            }
                                        }
                                    },
                                    enabled = !isProcessing
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = if (isListening) "停止录音" else "开始录音",
                                        modifier = Modifier.size(34.dp),
                                        tint = if (isListening) ErrorRed else AccentCyan
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isListening) "点击停止" else "点击说话",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ---- 文字/语音切换 ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { useTextMode = !useTextMode }) {
                            Icon(
                                if (useTextMode) Icons.Default.Mic else Icons.Default.Keyboard,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AccentCyan
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (useTextMode) "🎤 改用语音输入" else "✏️ 改用文字输入",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ---- 音乐页 ----
                if (currentTab == 1) {
                    MusicTab(
                        serverUrl = serverUrl,
                        api = api
                    )
                }

                // ---- 设置弹窗 ----
                if (showSettings) {
                    SettingsDialog(
                        currentUrl = serverUrl,
                        speakerName = speakerName,
                        onDismiss = { showSettings = false },
                        onSave = { newUrl ->
                            serverUrl = newUrl.trimEnd('/')
                            with(prefs.edit()) {
                                putString("server_url", serverUrl)
                                apply()
                            }
                            showSettings = false
                            refreshStatus()
                        }
                    )
                }
            }
        }
    }
}

// =============================================
// 聊天气泡组件
// =============================================
@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp, topEnd = 14.dp,
                        bottomStart = if (message.isUser) 14.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 14.dp
                    )
                )
                .background(if (message.isUser) UserBubble else AiBubble)
                .then(
                    if (!message.isUser) Modifier.border(
                        1.dp, AiBorder, RoundedCornerShape(
                            topStart = 14.dp, topEnd = 14.dp,
                            bottomStart = 4.dp, bottomEnd = 14.dp
                        )
                    ) else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = TextPrimary
            )
        }
    }
}

// =============================================
// 设置弹窗
// =============================================
@Composable
fun SettingsDialog(
    currentUrl: String,
    speakerName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = AccentCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("设置", color = TextPrimary)
            }
        },
        text = {
            Column {
                Text("服务器地址", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentCyan,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                        focusedContainerColor = NavyDark,
                        unfocusedContainerColor = NavyDark
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔊 ", color = TextSecondary)
                    Text("音箱: $speakerName", color = if (speakerName.contains("失败")) ErrorRed else SuccessGreen, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(url) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
            ) {
                Text("保存", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}
