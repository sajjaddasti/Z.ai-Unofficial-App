package com.unofficial.zai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unofficial.zai.ui.theme.BrandAqua
import com.unofficial.zai.ui.theme.BrandGradient
import com.unofficial.zai.ui.theme.BrandIndigo
import com.unofficial.zai.ui.theme.BrandMint
import com.unofficial.zai.ui.theme.BrandViolet

/** Brand mark: a soft gradient tile with a crisp white "Z". */
@Composable
fun BrandMark(
    size: Dp = 34.dp,
    corner: Dp = 11.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(corner),
                ambientColor = BrandMint.copy(alpha = 0.30f),
                spotColor = BrandIndigo.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(BrandGradient)),
        contentAlignment = Alignment.Center
    ) {
        // Top-left specular highlight, the detail that makes flat gradients feel glassy.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = 90f
                    )
                )
        )
        Text(
            text = "Z",
            color = Color(0xFF04150F),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.52f).sp,
            letterSpacing = (-0.5).sp
        )
    }
}

/** Soft ambient light behind overlays and settings so the UI never looks flat. */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    val dark = background.red + background.green + background.blue < 0.9f
    val mintGlow = if (dark) BrandMint.copy(alpha = 0.14f) else BrandMint.copy(alpha = 0.20f)
    val indigoGlow = if (dark) BrandIndigo.copy(alpha = 0.16f) else BrandViolet.copy(alpha = 0.10f)
    Box(
        modifier
            .fillMaxSize()
            .background(background)
            .background(
                Brush.radialGradient(
                    colors = listOf(mintGlow, Color.Transparent),
                    center = Offset(120f, -60f),
                    radius = 900f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(indigoGlow, Color.Transparent),
                    center = Offset(950f, 1500f),
                    radius = 1100f
                )
            )
    )
}

/** Rounded glass panel with a hairline edge. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.28f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.98f), color.copy(alpha = 0.92f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

/** Small rounded icon tile used in settings rows and menu shortcuts. */
@Composable
fun IconOrb(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(
                Brush.linearGradient(
                    listOf(
                        tint.copy(alpha = 0.20f),
                        BrandIndigo.copy(alpha = 0.14f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.28f), Color.Transparent)
                ),
                RoundedCornerShape(size * 0.32f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size * 0.48f),
            tint = tint
        )
    }
}

/** Circular translucent button used in the header and settings header. */
@Composable
fun CircleGlassButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    size: Dp = 40.dp
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.surfaceVariant.copy(alpha = 0.72f))
            .border(1.dp, scheme.outline.copy(alpha = 0.55f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size * 0.48f),
            tint = scheme.onSurface
        )
    }
}

/** Primary call to action: gradient fill, dark ink, generous radius. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .height(54.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = BrandMint.copy(alpha = 0.35f),
                spotColor = BrandMint.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(BrandGradient))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon ?: Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFF04150F),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = Color(0xFF04150F),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Pill label, e.g. a version or status chip. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    )
}

/** Live status dot with a soft halo. */
@Composable
fun StatusDot(color: Color = BrandMint, size: Dp = 6.dp) {
    Box(
        modifier = Modifier
            .size(size + 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.25f))
        )
        Box(
            Modifier
                .size(size * 0.62f)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** Small aqua/indigo underline used as a section accent. */
@Composable
fun AccentRule(modifier: Modifier = Modifier, width: Dp = 42.dp) {
    Box(
        modifier
            .width(width)
            .height(3.dp)
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(listOf(BrandAqua, BrandMint, BrandIndigo))
            )
    )
}
