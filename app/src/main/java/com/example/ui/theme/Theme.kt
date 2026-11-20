package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// PRD 7.1.5: Strictly Light UI (Warm, Friendly & Clean) — never default to dark theme.
private val WorthItLightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = SoftWhite,
    primaryContainer = PaleSage,
    onPrimaryContainer = DeepCharcoal,
    secondary = SoftApricot,
    onSecondary = DeepCharcoal,
    secondaryContainer = SoftApricot,
    onSecondaryContainer = DeepCharcoal,
    tertiary = DarkAmberText,
    onTertiary = SoftWhite,
    tertiaryContainer = SoftAmber,
    onTertiaryContainer = DarkAmberText,
    background = WarmCream,
    onBackground = DeepCharcoal,
    surface = SoftWhite,
    onSurface = DeepCharcoal,
    surfaceVariant = WarmCream,
    onSurfaceVariant = WarmGray,
    surfaceContainerLowest = SoftWhite,
    surfaceContainerLow = WarmCream,
    surfaceContainer = SoftWhite,
    surfaceContainerHigh = PaleSage,
    surfaceContainerHighest = SoftApricot,
    outline = BorderWarmGray,
    outlineVariant = BorderWarmGray,
    error = DarkCoralText,
    onError = SoftWhite,
    errorContainer = SoftCoral,
    onErrorContainer = DarkCoralText
)

val WorthItShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun WorthItTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WorthItLightColorScheme,
        typography = WorthItTypography,
        shapes = WorthItShapes,
        content = content
    )
}
