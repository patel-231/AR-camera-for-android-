package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZeroDarkColorScheme = darkColorScheme(
    primary = ZeroGold,
    onPrimary = Color(0xFF0F172A),
    secondary = ZeroCyan,
    onSecondary = Color(0xFF0F172A),
    background = ZeroDarkBg,
    onBackground = ZeroOnDark,
    surface = ZeroSurface,
    onSurface = ZeroOnDark
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZeroDarkColorScheme,
        typography = Typography,
        content = content
    )
}
