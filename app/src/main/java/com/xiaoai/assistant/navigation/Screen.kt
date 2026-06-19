package com.xiaoai.assistant.navigation

/**
 * 应用导航路由定义
 */
sealed class Screen(val route: String, val label: String, val icon: String) {
    data object Chat : Screen("chat", "对话", "💬")
    data object Music : Screen("music", "音乐", "🎵")
    data object Settings : Screen("settings", "设置", "⚙️")
}

val bottomNavScreens = listOf(
    Screen.Chat,
    Screen.Music,
    Screen.Settings
)
