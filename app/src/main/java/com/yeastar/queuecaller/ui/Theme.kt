package com.yeastar.queuecaller.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF1565C0)
private val BlueDark = Color(0xFF0D47A1)
private val Green = Color(0xFF2E7D32)
private val Red = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = Blue,
    secondary = BlueDark,
    tertiary = Green,
    error = Red
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF64B5F6),
    secondary = Color(0xFF90CAF9),
    tertiary = Color(0xFF81C784),
    error = Color(0xFFEF9A9A)
)

@Composable
fun YeastarQueueTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
