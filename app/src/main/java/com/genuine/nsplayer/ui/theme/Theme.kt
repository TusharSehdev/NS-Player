package com.genuine.nsplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core Deep Void & Obsidian Palette
val NSBlack = Color(0xFF090A0F)
val NSSurface = Color(0xFF12151F)
val NSSurfaceVariant = Color(0xFF1B202E)
val NSSurfaceCard = Color(0xFF161B26)
val NSBorder = Color(0xFF262E42)
val NSBorderFocused = Color(0xFF3D7BFF)

// Text Colors
val NSTextPrimary = Color(0xFFF2F5FB)
val NSTextSecondary = Color(0xFF94A0B8)
val NSTextMuted = Color(0xFF64748B)

// Status & Accents
val NSLiveRed = Color(0xFFFF334B)
val NSEPGGreen = Color(0xFF00E676)
val NSError = Color(0xFFFF453A)
val NSWarning = Color(0xFFFF9F0A)
val NSGold = Color(0xFFFFD600)

// Theme Accents
val NSPrimary = Color(0xFF3D7BFF)
val NSPrimaryVariant = Color(0xFF1E56D0)
val NSAccent = Color(0xFF00D2FF)

val FrostyAuraPrimary = Color(0xFF00E5FF)
val RedRosePrimary = Color(0xFFFF2A6D)
val EmeraldPrimary = Color(0xFF05D585)
val AmberGlowPrimary = Color(0xFFFF9E00)
val CyberPurplePrimary = Color(0xFF9D4EDD)

fun getThemePrimary(style: String): Color {
    return when (style.lowercase()) {
        "frosty aura", "frosty cyan" -> FrostyAuraPrimary
        "red rose", "ruby glow" -> RedRosePrimary
        "emerald", "emerald stream" -> EmeraldPrimary
        "amber glow", "amber neon" -> AmberGlowPrimary
        "cyber purple" -> CyberPurplePrimary
        else -> NSPrimary
    }
}

// Gradients for Hero Cards & Overlays
val HeroGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0x331B2A4A),
        Color(0xEE12151F),
        Color(0xFF12151F)
    )
)

val PlayerScrimTop = Brush.verticalGradient(
    colors = listOf(
        Color(0xDD000000),
        Color(0x88000000),
        Color.Transparent
    )
)

val PlayerScrimBottom = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color(0x99000000),
        Color(0xEE000000)
    )
)

val CardGlowGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0x223D7BFF),
        Color(0x1100D2FF)
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = NSPrimary,
    onPrimary = Color.White,
    primaryContainer = NSPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = NSAccent,
    onSecondary = Color.Black,
    background = NSBlack,
    onBackground = NSTextPrimary,
    surface = NSSurface,
    onSurface = NSTextPrimary,
    surfaceVariant = NSSurfaceVariant,
    onSurfaceVariant = NSTextSecondary,
    outline = NSBorder,
    error = NSError
)

@Composable
fun NSPlayerTheme(
    themeStyle: String = "Obsidian",
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val primaryColor = getThemePrimary(themeStyle)
    val colorScheme = DarkColorScheme.copy(
        primary = primaryColor,
        primaryContainer = primaryColor.copy(alpha = 0.25f),
        secondary = if (themeStyle.equals("Obsidian", ignoreCase = true)) NSAccent else primaryColor
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
