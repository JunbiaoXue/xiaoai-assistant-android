package com.xiaoai.assistant

import android.content.Context
import android.os.Bundle
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xiaoai.assistant.ui.chat.ChatScreen
import com.xiaoai.assistant.ui.music.MusicScreen
import com.xiaoai.assistant.ui.navigation.BottomNavBar
import com.xiaoai.assistant.ui.navigation.bottomNavItems
import com.xiaoai.assistant.ui.settings.SettingsScreen
import com.xiaoai.assistant.ui.theme.XiaoaiTheme
import com.xiaoai.assistant.viewmodel.ChatViewModel
import com.xiaoai.assistant.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XiaoaiTheme {
                XiaoaiApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XiaoaiApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("xiaoai", Context.MODE_PRIVATE) }

    // 服务器地址从 SharedPreferences 读取
    val savedUrl = remember { prefs.getString("server_url", "http://192.168.1.15:8765") ?: "http://192.168.1.15:8765" }
    var serverUrl by remember { mutableStateOf(savedUrl) }

    // ViewModel 共享服务器地址
    val chatViewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val musicViewModel: MusicViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // 初始化服务器地址
    LaunchedEffect(serverUrl) {
        chatViewModel.updateServerUrl(serverUrl)
        musicViewModel.updateServerUrl(serverUrl)
        chatViewModel.refreshStatus()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "chat"

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("chat") {
                ChatScreen(
                    chatViewModel = chatViewModel,
                    onNeedSpeechRecognizer = {
                        if (SpeechRecognizer.isRecognitionAvailable(context)) {
                            SpeechRecognizer.createSpeechRecognizer(context)
                        } else null
                    }
                )
            }
            composable("music") {
                MusicScreen(musicViewModel = musicViewModel)
            }
            composable("settings") {
                SettingsScreen(
                    serverUrl = serverUrl,
                    onServerUrlChange = { newUrl ->
                        serverUrl = newUrl
                        prefs.edit().putString("server_url", newUrl).apply()
                        chatViewModel.updateServerUrl(newUrl)
                        musicViewModel.updateServerUrl(newUrl)
                        chatViewModel.refreshStatus()
                    }
                )
            }
        }
    }
}
