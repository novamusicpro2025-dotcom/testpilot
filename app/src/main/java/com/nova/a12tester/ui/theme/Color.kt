package com.nova.a12tester.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.nova.a12tester.domain.ThemeMode

// Vibrant Tech Brand Palette
val BrandIndigo = Color(0xFF4F46E5)       // Electric Indigo Primary
val BrandIndigoDark = Color(0xFF3730A3)
val BrandIndigoLight = Color(0xFF818CF8)

val BrandTeal = Color(0xFF0D9488)         // Vibrant Emerald / Teal
val BrandTealDark = Color(0xFF0F766E)
val BrandTealLight = Color(0xFF2DD4BF)

val BrandCyan = Color(0xFF06B6D4)         // Vivid Cyan
val BrandEmerald = Color(0xFF10B981)      // Crisp Emerald
val BrandAmber = Color(0xFFF59E0B)        // Sunset Gold / Amber
val BrandAmberDark = Color(0xFFD97706)
val BrandPink = Color(0xFFEC4899)         // Neon Pink
val BrandPurple = Color(0xFF8B5CF6)       // Royal Violet
val BrandBlue = Color(0xFF2563EB)         // Royal Blue
val BrandOrange = Color(0xFFF97316)       // Coral Orange

// Colorful Gradients
object AppGradients {
    val Aurora = Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF06B6D4), Color(0xFF10B981)))
    val Ocean = Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF06B6D4)))
    val Sunset = Brush.horizontalGradient(listOf(Color(0xFFFF512F), Color(0xFFF09819)))
    val PurplePink = Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
    val EmeraldTeal = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF0D9488)))
    val AmberGold = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
    val GoogleBlue = Brush.horizontalGradient(listOf(Color(0xFF1A73E8), Color(0xFF4285F4)))
    val PlayGreen = Brush.horizontalGradient(listOf(Color(0xFF0F9D58), Color(0xFF34A853)))
}

private val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = BrandIndigoDark,
    secondary = BrandTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = BrandTealDark,
    tertiary = BrandAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFEF4444),
    onError = Color.White,
    outline = Color(0xFFE2E8F0)
)

private val DarkColors = darkColorScheme(
    primary = BrandIndigoLight,
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFEEF2FF),
    secondary = BrandTealLight,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = BrandAmber,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0B0F17),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    outline = Color(0xFF334155)
)

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun isAppInDarkTheme(): Boolean = LocalIsDarkTheme.current || isSystemInDarkTheme() || MaterialTheme.colorScheme.surface == DarkColors.surface

@Composable
fun TestPilotTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalIsDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TestPilotTypography,
            shapes = TestPilotShapes,
            content = content
        )
    }
}
