package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color.Black,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberPrimary,
    secondary = AmberSecondary,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    error = RecordingRed
)

private val OledColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF201600),
    onPrimaryContainer = AmberPrimary,
    secondary = AmberSecondary,
    onSecondary = Color.Black,
    background = OledBackground,
    onBackground = Color(0xFFEDEDED),
    surface = OledSurface,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = OledBorder,
    error = RecordingRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF92400E),
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF111827),
    surface = LightSurface,
    onSurface = Color(0xFF111827),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF4B5563),
    outline = LightBorder,
    error = RecordingRed
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        AppThemeMode.SYSTEM -> if (systemDark) DarkColorScheme else LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.OLED_BLACK -> OledColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
