package com.xiaoai.assistant.ui.chat

import android.speech.SpeechRecognizer
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoai.assistant.ui.theme.*
import com.xiaoai.assistant.viewmodel.ChatMessage
import com.xiaoai.assistant.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel = viewModel(),
    onNeedSpeechRecognizer: () -> SpeechRecognizer?
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // 自动滚动到底部
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
    ) {
        // 顶部状态栏
        StatusHeader(
            speakerName = uiState.speakerName,
            statusText = uiState.statusText,
            statusType = uiState.statusType
        )

        // 消息列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.messages, key = { it.id }) { msg ->
                MessageBubble(msg)
            }
        }

        // 中间识别状态
        if (uiState.interimText.isNotBlank()) {
            Text(
                text = uiState.interimText,
                color = AccentCyan,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // 底部输入区
        InputBar(
            useTextMode = uiState.useTextMode,
            isProcessing = uiState.isProcessing,
            isListening = uiState.isListening,
            textInput = uiState.textInput,
            onToggleMode = { chatViewModel.toggleTextMode() },
            onTextInputChange = { chatViewModel.updateTextInput(it) },
            onSend = {
                chatViewModel.sendMessage(it)
                chatViewModel.clearTextInput()
            },
            onVoiceStart = {
                val recognizer = onNeedSpeechRecognizer()
                if (recognizer != null) {
                    chatViewModel.initSpeechRecognizer(recognizer)
                    chatViewModel.startListening()
                }
            },
            onVoiceStop = { chatViewModel.stopListening() }
        )
    }
}

@Composable
private fun StatusHeader(speakerName: String, statusText: String, statusType: String) {
    val bgColor = when (statusType) {
        "error" -> ErrorRed.copy(alpha = 0.15f)
        "thinking" -> AccentCyan.copy(alpha = 0.1f)
        "speaking" -> SuccessGreen.copy(alpha = 0.1f)
        else -> NavyCard
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("🔊 $speakerName", color = TextPrimary, fontSize = 13.sp)
                Text(statusText, color = TextSecondary, fontSize = 12.sp)
            }
            val dotColor = when (statusType) {
                "error" -> ErrorRed
                "thinking" -> WarningYellow
                "speaking" -> SuccessGreen
                else -> SuccessGreen
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) UserBubble.copy(alpha = 0.2f) else AiBubble,
            border = if (!isUser) null else null,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.text,
                color = if (isUser) AccentCyan else TextPrimary,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun InputBar(
    useTextMode: Boolean,
    isProcessing: Boolean,
    isListening: Boolean,
    textInput: String,
    onToggleMode: () -> Unit,
    onTextInputChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyCard
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 切换模式按钮
            IconButton(onClick = onToggleMode) {
                Icon(
                    imageVector = if (useTextMode) Icons.Default.MicNone else Icons.Default.Keyboard,
                    contentDescription = if (useTextMode) "语音模式" else "文字模式",
                    tint = TextSecondary
                )
            }

            if (useTextMode) {
                // 文字输入
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...", color = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = AiBorder,
                        cursorColor = AccentCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { if (textInput.isNotBlank()) onSend(textInput) },
                    enabled = textInput.isNotBlank() && !isProcessing,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = NavyDark
                    )
                ) {
                    Text("发送")
                }
            } else {
                // 语音按钮
                Text(
                    text = if (isListening) "松开发送" else if (isProcessing) "处理中..." else "按住说话",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {},
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    containerColor = if (isListening) ErrorRed else AccentCyan,
                    contentColor = NavyDark
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "语音",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
