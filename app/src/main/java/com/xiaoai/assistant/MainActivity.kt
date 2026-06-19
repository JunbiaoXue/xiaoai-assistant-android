package com.xiaoai.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoai.assistant.ui.screens.BottomNavApp
import com.xiaoai.assistant.ui.theme.*
import com.xiaoai.assistant.viewmodel.ChatViewModel
import com.xiaoai.assistant.viewmodel.MusicViewModel
import com.xiaoai.assistant.viewmodel.SettingsViewModel

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
// 主页面 —— 使用 ViewModel 驱动的底部导航架构
// =============================================
@Composable
fun XiaoaiApp() {
    // ---- ViewModels ----
    val settingsViewModel: SettingsViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val musicViewModel: MusicViewModel = viewModel()

    // 从 SettingsViewModel 同步 serverUrl 到其他 ViewModel
    val settingsState by settingsViewModel.uiState.collectAsState()
    LaunchedEffect(settingsState.serverUrl) {
        chatViewModel.updateServerUrl(settingsState.serverUrl)
        chatViewModel.refreshStatus()
        musicViewModel.updateServerUrl(settingsState.serverUrl)
    }

    // 从 ChatViewModel 同步音箱状态到 SettingsViewModel
    val chatState by chatViewModel.uiState.collectAsState()
    LaunchedEffect(chatState.speakerName) {
        // 更新 settings 中显示的音箱名称（通过 checkConnection 已实现）
    }

    // ---- 渲染 ----
    XiaoaiTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NavyDark
        ) {
            BottomNavApp(
                chatViewModel = chatViewModel,
                musicViewModel = musicViewModel,
                settingsViewModel = settingsViewModel
            )
        }
    }
}
