package com.mindhex.sweep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF00BFA5)
private val TealDark = Color(0xFF00897B)
private val Ink = Color(0xFF102027)

private val LightColors = lightColorScheme(
    primary = TealDark,
    secondary = Teal,
    background = Color(0xFFF6F8F9),
    surface = Color.White,
    onPrimary = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Teal,
    secondary = TealDark,
    background = Ink,
    surface = Color(0xFF1B2A31),
    onPrimary = Ink
)

@Composable
fun SweepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
