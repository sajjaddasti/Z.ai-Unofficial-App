package com.unofficial.zai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Brand system for the z.ai client.
 *
 * A single mint/aqua accent paired with an indigo highlight. Surfaces are
 * near-black in dark mode so the WebView reads as the content, and paper-white
 * in light mode. Gradients are reserved for brand moments (logo, primary
 * actions) so the interface stays calm around the chat.
 */

val BrandMint = Color(0xFF4ADE9F)
val BrandAqua = Color(0xFF2DD4BF)
val BrandIndigo = Color(0xFF6366F1)
val BrandViolet = Color(0xFF8B5CF6)

/** Mint -> aqua, used for primary actions and the brand mark. */
val BrandGradient = listOf(BrandMint, BrandAqua)

/** Mint -> aqua -> indigo, used sparingly for large brand moments. */
val BrandGradientWide = listOf(BrandMint, BrandAqua, BrandIndigo)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E9F6E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6F5E8),
    onPrimaryContainer = Color(0xFF043B2A),
    secondary = BrandIndigo,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E7FF),
    onSecondaryContainer = Color(0xFF1B1D5E),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF0A0E13),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0E13),
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = Color(0xFF5C6673),
    outline = Color(0xFFDCE1E8),
    outlineVariant = Color(0xFFE9EDF2),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = BrandMint,
    onPrimary = Color(0xFF032015),
    primaryContainer = Color(0xFF0B3A2A),
    onPrimaryContainer = Color(0xFFBFF7DE),
    secondary = Color(0xFF9BA3FF),
    onSecondary = Color(0xFF12143F),
    secondaryContainer = Color(0xFF23265F),
    onSecondaryContainer = Color(0xFFE2E4FF),
    tertiary = BrandAqua,
    onTertiary = Color(0xFF00201C),
    background = Color(0xFF07090C),
    onBackground = Color(0xFFEDF1F5),
    surface = Color(0xFF0E1116),
    onSurface = Color(0xFFEDF1F5),
    surfaceVariant = Color(0xFF171C23),
    onSurfaceVariant = Color(0xFF98A3B1),
    outline = Color(0xFF242B34),
    outlineVariant = Color(0xFF1A2028),
    error = Color(0xFFFF7A6E),
    onError = Color(0xFF2A0603)
)

val ZaiShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ZaiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = ZaiShapes,
        content = content
    )
}
