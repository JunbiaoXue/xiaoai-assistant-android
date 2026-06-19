package com.xiaoai.assistant.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoai.assistant.ui.theme.*

@Composable
fun SettingsScreen(
    serverUrl: String,
    onServerUrlChange: (String) -> Unit
) {
    var editingUrl by remember { mutableStateOf(serverUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("设置", color = TextPrimary, fontSize = 22.sp)

        // 服务器地址
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NavyCard
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("服务器地址", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editingUrl,
                    onValueChange = { editingUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = AiBorder,
                        cursorColor = AccentCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { editingUrl = "http://192.168.1.15:8765" }) {
                        Text("局域网", color = AccentCyan, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { editingUrl = "http://43.136.231.92:5000" }) {
                        Text("外网", color = AccentCyan, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onServerUrlChange(editingUrl.trimEnd('/')) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan,
                            contentColor = NavyDark
                        )
                    ) {
                        Text("保存")
                    }
                }
            }
        }

        // 当前连接
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NavyCard
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("当前连接", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(serverUrl, color = AccentCyan, fontSize = 14.sp)
            }
        }

        // 关于
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NavyCard
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关于", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("小管家 v1.0", color = TextPrimary, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("AI 语音助手 · 小爱音箱控制", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}
