package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val WateeraLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = AmberOnContainerLight,
    tertiary = DeepTealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TealContainerLight,
    onTertiaryContainer = TealOnContainerLight,
    background = SandBackground,
    onBackground = ForestInk,
    surface = IvorySurface,
    onSurface = ForestInk,
    surfaceVariant = LinenSurfaceVariant,
    onSurfaceVariant = SageSlate,
    surfaceContainerLowest = IvorySurface,
    surfaceContainerLow = Color(0xFFFAF9F5),
    surfaceContainer = Color(0xFFF2EFE7),
    surfaceContainerHigh = Color(0xFFEBE7DC),
    outline = SoftBorderLight,
    outlineVariant = Color(0xFFE7E2D6),
    error = CoralErrorLight,
    onError = Color.White
)

private val WateeraDarkColorScheme = darkColorScheme(
    primary = MintPrimaryDark,
    onPrimary = Color(0xFF043026),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = Color(0xFF3E2400),
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberOnContainerDark,
    tertiary = SkyTealTertiaryDark,
    onTertiary = Color(0xFF072834),
    tertiaryContainer = TealContainerDark,
    onTertiaryContainer = TealOnContainerDark,
    background = ObsidianPineBackground,
    onBackground = MistWhiteInk,
    surface = CharcoalPineSurface,
    onSurface = MistWhiteInk,
    surfaceVariant = MossSlateVariant,
    onSurfaceVariant = MutedSageInk,
    surfaceContainerLowest = Color(0xFF0B1210),
    surfaceContainerLow = Color(0xFF141E1B),
    surfaceContainer = CharcoalPineSurface,
    surfaceContainerHigh = MossSlateVariant,
    outline = SoftBorderDark,
    outlineVariant = Color(0xFF25342F),
    error = CoralErrorDark,
    onError = Color(0xFF690005)
)

val WateeraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun WateeraTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) WateeraDarkColorScheme else WateeraLightColorScheme,
        typography = WateeraTypography,
        shapes = WateeraShapes,
        content = content
    )
}
