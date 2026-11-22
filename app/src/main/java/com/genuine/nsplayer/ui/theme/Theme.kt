package com.genuine.nsplayer.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ==========================================
// NEXORA CINEMATIC DESIGN SYSTEM PALETTE
// ==========================================
val NexoraBackground = Color(0xFF0B0D12)
val NexoraBackgroundSecondary = Color(0xFF121620)
val NexoraSurface = Color(0xFF191F2B)
val NexoraSurfaceElevated = Color(0xFF202938)
val NexoraBorder = Color(0xFF2B3547)
val NexoraBorderFocused = Color(0xFF78A9FF)

// Accents (Restrained & Sophisticated)
val NexoraPrimaryAccent = Color(0xFF78A9FF)
val NexoraSecondaryAccent = Color(0xFFA5B9DC)

// Typographic Scale Colors
val NexoraTextPrimary = Color(0xFFF4F6FA)
val NexoraTextSecondary = Color(0xFF9AA5B5)
val NexoraTextMuted = Color(0xFF687386)

// Semantic State Colors
val NexoraSuccess = Color(0xFF4CAF86)
val NexoraError = Color(0xFFE36D72)
val NexoraWarning = Color(0xFFE4B568)
val NexoraLiveBadge = Color(0xFFFF334B)

// Aliases for seamless backwards compatibility
val NSBlack = NexoraBackground
val NSSurface = NexoraSurface
val NSSurfaceVariant = NexoraSurfaceElevated
val NSSurfaceCard = NexoraSurface
val NSBorder = NexoraBorder
val NSBorderFocused = NexoraBorderFocused
val NSTextPrimary = NexoraTextPrimary
val NSTextSecondary = NexoraTextSecondary
val NSTextMuted = NexoraTextMuted
val NSLiveRed = NexoraLiveBadge
val NSEPGGreen = NexoraSuccess
val NSError = NexoraError
val NSWarning = NexoraWarning
val NSGold = NexoraWarning
val NSPrimary = NexoraPrimaryAccent
val NSPrimaryVariant = NexoraSurfaceElevated
val NSAccent = NexoraSecondaryAccent

// Cinematic Color Variations for User Themes
val MidnightBlueAccent = Color(0xFF5B93FF)
val SlateAccent = Color(0xFF94A3B8)
val EmeraldAccent = Color(0xFF4CAF86)
val AmberAccent = Color(0xFFE4B568)

fun getThemePrimary(style: String): Color {
    return when (style.lowercase()) {
        "midnight blue" -> MidnightBlueAccent
        "slate" -> SlateAccent
        "emerald" -> EmeraldAccent
        "amber" -> AmberAccent
        else -> NexoraPrimaryAccent
    }
}

// Sophisticated Cinematic Scrims & Overlays (No Cheap Heavy Gradients)
val HeroGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0x33121620),
        Color(0xCC0B0D12),
        Color(0xFF0B0D12)
    )
)

val PlayerScrimTop = Brush.verticalGradient(
    colors = listOf(
        Color(0xDD000000),
        Color(0x66000000),
        Color.Transparent
    )
)

val PlayerScrimBottom = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color(0x88000000),
        Color(0xEE000000)
    )
)

val CardGlowGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0x1A78A9FF),
        Color(0x0DA5B9DC)
    )
)

private val NexoraColorScheme = darkColorScheme(
    primary = NexoraPrimaryAccent,
    onPrimary = Color(0xFF0B0D12),
    primaryContainer = NexoraSurfaceElevated,
    onPrimaryContainer = NexoraTextPrimary,
    secondary = NexoraSecondaryAccent,
    onSecondary = Color(0xFF0B0D12),
    background = NexoraBackground,
    onBackground = NexoraTextPrimary,
    surface = NexoraSurface,
    onSurface = NexoraTextPrimary,
    surfaceVariant = NexoraSurfaceElevated,
    onSurfaceVariant = NexoraTextSecondary,
    outline = NexoraBorder,
    error = NexoraError,
    onError = Color.White
)

@Composable
fun NSPlayerTheme(
    themeStyle: String = "Obsidian",
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val primaryColor = getThemePrimary(themeStyle)
    val colorScheme = NexoraColorScheme.copy(
        primary = primaryColor,
        primaryContainer = primaryColor.copy(alpha = 0.20f)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(12.dp),
            extraLarge = RoundedCornerShape(16.dp)
        ),
        content = content
    )
}
