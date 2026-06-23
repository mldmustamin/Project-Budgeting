package com.example.fundsmanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = NavyBlue,
    onPrimary = Color.White,
    primaryContainer = SoftBlue,
    onPrimaryContainer = NavyBlueDark,
    secondary = NavyBlueDark,
    tertiary = WarningOrange,
    background = AppBackground,
    onBackground = NavyBlueDark,
    surface = AppSurface,
    onSurface = NavyBlueDark,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = AppBorder,
    error = DangerRed
)

@Composable
fun FundsManagerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
