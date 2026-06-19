package com.xiaoai.assistant.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoai.assistant.viewmodel.ChatMessage
import com.xiaoai.assistant.viewmodel.ChatViewModel
import com.xiaoai.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    onSettingsClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // 初始化 SpeechRecognizer
    val speechRecognizer = remember {
        try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            null
        }
    }

    // 将 recognizer 交给 ViewModel
    LaunchedEffect(speechRecognizer) {
        if (speechRecognizer != null) {
            viewModel.initSpeechRecognizer(speechRecognizer)
        }
    }

    // 首次加载时刷新状态
    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }

    // 自动滚到底部
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // 麦克风权限请求
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        } else {
            Toast.makeText(context, "需要麦克风权限才能语音输入", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

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
            if (onSettingsClick != null) {
                IconButton(onClick = { onSettingsClick() }) {
                    Icon(Icons.Default.Settings, contentDescription = "设置", tint = TextSecondary)
                }
            }
        }

        // ---- 状态栏 ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dotColor = when (uiState.statusType) {
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
            Text(text = uiState.statusText, fontSize = 13.sp, color = TextSecondary)
        }

        // ---- 音箱状态 ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (uiState.speakerName.contains("失败")) "🔇 音箱: ${uiState.speakerName}" else "🔊 音箱: ${uiState.speakerName}",
                fontSize = 12.sp,
                color = if (uiState.speakerName.contains("失败") || uiState.speakerName == "检测中...") TextSecondary else SuccessGreen
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
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- 语音/文字中间结果提示 ----
        if (uiState.isListening && uiState.interimText.isNotBlank()) {
            Text(
                text = "🎤 ${uiState.interimText}",
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

        // ---- 语音 / 文字输入区 ----
        if (uiState.useTextMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.textInput,
                    onValueChange = { viewModel.updateTextInput(it) },
                    placeholder = { Text("输入消息...", color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentCyan,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = DividerColor,
                        focusedContainerColor = NavyCard,
                        unfocusedContainerColor = NavyCard
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (!uiState.isProcessing && uiState.textInput.isNotBlank()) {
                                viewModel.sendMessage(uiState.textInput.trim())
                                viewModel.clearTextInput()
                            }
                        }
                    )
                )
                Button(
                    onClick = {
                        if (!uiState.isProcessing && uiState.textInput.isNotBlank()) {
                            viewModel.sendMessage(uiState.textInput.trim())
                            viewModel.clearTextInput()
                        }
                    },
                    enabled = !uiState.isProcessing && uiState.textInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (uiState.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = androidx.compose.ui.graphics.Color.Black,
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
                            color = if (uiState.isListening) ErrorRed else AccentCyan,
                            shape = CircleShape
                        )
                        .background(
                            if (uiState.isListening) ErrorRed.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (uiState.isProcessing) return@IconButton
                            if (uiState.isListening) {
                                viewModel.stopListening()
                            } else {
                                if (speechRecognizer == null) {
                                    Toast.makeText(context, "设备不支持语音识别", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    viewModel.startListening()
                                }
                            }
                        },
                        enabled = !uiState.isProcessing
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = if (uiState.isListening) "停止录音" else "开始录音",
                            modifier = Modifier.size(34.dp),
                            tint = if (uiState.isListening) ErrorRed else AccentCyan
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (uiState.isListening) "点击停止" else "点击说话",
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
            TextButton(onClick = { viewModel.toggleTextMode() }) {
                Icon(
                    if (uiState.useTextMode) Icons.Default.Mic else Icons.Default.Keyboard,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AccentCyan
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (uiState.useTextMode) "🎤 改用语音输入" else "✏️ 改用文字输入",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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
