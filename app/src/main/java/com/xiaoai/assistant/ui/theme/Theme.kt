package com.xiaoai.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val XiaoaiColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    background = NavyDark,
    onBackground = TextPrimary,
    surface = NavyCard,
    onSurface = TextPrimary,
    secondary = AccentPurple,
    onSecondary = TextPrimary,
    error = ErrorRed,
    onError = Color.White,
    surfaceVariant = NavyMid,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
)

@Composable
fun XiaoaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = XiaoaiColorScheme,
        content = content
    )
}
