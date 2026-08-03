package com.customalert.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF1B6B5A)
private val TealDark = Color(0xFF0F3F35)
private val Sand = Color(0xFFF3F0E8)
private val Ink = Color(0xFF1C1B1A)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7E4D5),
    onPrimaryContainer = TealDark,
    secondary = Color(0xFF4A635C),
    background = Sand,
    onBackground = Ink,
    surface = Color(0xFFFFFCF7),
    onSurface = Ink
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DD4C0),
    onPrimary = TealDark,
    primaryContainer = Teal,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFB0CCC3),
    background = Color(0xFF121413),
    onBackground = Color(0xFFE4E2DE),
    surface = Color(0xFF1C1F1E),
    onSurface = Color(0xFFE4E2DE)
)

@Composable
fun CustomAlertTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
