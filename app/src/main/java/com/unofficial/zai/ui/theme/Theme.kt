package com.unofficial.zai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Neutral "premium chat" palette: graphite surfaces with a single soft accent.
private val Accent = Color(0xFF10A37F)
private val AccentDark = Color(0xFF19C39B)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F4EC),
    onPrimaryContainer = Color(0xFF00382C),
    secondary = Color(0xFF4A5568),
    onSecondary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF14171A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14171A),
    surfaceVariant = Color(0xFFF1F2F4),
    onSurfaceVariant = Color(0xFF5B6470),
    outline = Color(0xFFD6DAE0),
    error = Color(0xFFD93025),
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF00281F),
    primaryContainer = Color(0xFF0B3B31),
    onPrimaryContainer = Color(0xFFB7F2E1),
    secondary = Color(0xFFA7B0BE),
    onSecondary = Color(0xFF1B1F24),
    background = Color(0xFF16181A),
    onBackground = Color(0xFFEDEFF2),
    surface = Color(0xFF1E2124),
    onSurface = Color(0xFFEDEFF2),
    surfaceVariant = Color(0xFF282C31),
    onSurfaceVariant = Color(0xFFAAB3BF),
    outline = Color(0xFF3A4048),
    error = Color(0xFFFF6B5E),
)

@Composable
fun ZaiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
