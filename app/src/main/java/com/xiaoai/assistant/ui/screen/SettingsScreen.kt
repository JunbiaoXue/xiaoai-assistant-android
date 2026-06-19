package com.xiaoai.assistant.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoai.assistant.viewmodel.ChatViewModel
import com.xiaoai.assistant.viewmodel.MusicViewModel
import com.xiaoai.assistant.viewmodel.SettingsViewModel
import com.xiaoai.assistant.ui.theme.*

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    musicViewModel: MusicViewModel = viewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⚙️ ", fontSize = 28.sp)
            Text(
                text = "设置",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 服务器连接状态卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        tint = if (uiState.isConnected) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "服务器连接",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (uiState.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AccentCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (uiState.isConnected) "已连接" else "未连接",
                            fontSize = 14.sp,
                            color = if (uiState.isConnected) SuccessGreen else ErrorRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 服务器地址
                Text("服务器地址", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = uiState.serverUrl,
                    onValueChange = { settingsViewModel.updateServerUrl(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentCyan,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = DividerColor,
                        focusedContainerColor = NavyDark,
                        unfocusedContainerColor = NavyDark
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 保存按钮 —— 同步服务器地址到所有 ViewModel
                Button(
                    onClick = {
                        settingsViewModel.saveServerUrl()
                        // 同步到 Chat 和 Music ViewModel
                        chatViewModel.updateServerUrl(uiState.serverUrl.trimEnd('/'))
                        musicViewModel.updateServerUrl(uiState.serverUrl.trimEnd('/'))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存设置", fontWeight = FontWeight.Bold)
                }

                if (uiState.saveMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.saveMessage!!,
                        fontSize = 13.sp,
                        color = SuccessGreen
                    )
                    LaunchedEffect(uiState.saveMessage) {
                        kotlinx.coroutines.delay(2000)
                        settingsViewModel.clearSaveMessage()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 设备信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "设备信息",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 音箱名称
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Speaker,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("音箱", fontSize = 15.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = uiState.speakerName,
                        fontSize = 15.sp,
                        color = if (uiState.speakerName.contains("失败")) ErrorRed else TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = DividerColor)

                Spacer(modifier = Modifier.height(8.dp))

                // LLM 模型
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI 模型", fontSize = 15.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = uiState.llmModel,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 刷新按钮
                OutlinedButton(
                    onClick = { settingsViewModel.checkConnection() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AccentCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("刷新状态")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 关于卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "小管家 AI 助手",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "v1.0.0 — MVVM Architecture",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
