package com.example.fin_ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FinAIDarkColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = OnSurfaceWhite,
    primaryContainer = VioletLight,
    onPrimaryContainer = OnSurfaceWhite,

    secondary = SurfaceContainer,
    onSecondary = OnSurfaceWhite,
    secondaryContainer = SurfaceContainerHigh,
    onSecondaryContainer = OnSurfaceWhite,

    tertiary = SoftRed,
    onTertiary = OnSurfaceWhite,
    tertiaryContainer = ErrorContainerDark,
    onTertiaryContainer = SoftRed,

    error = ErrorRed,
    onError = OnSurfaceWhite,
    errorContainer = ErrorContainerDark,
    onErrorContainer = SoftRed,

    background = BgDark,
    onBackground = OnSurfaceWhite,

    surface = BgDark,
    onSurface = OnSurfaceWhite,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = OnSurfaceVariantZinc,

    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,

    outline = OutlineViolet,
    outlineVariant = OutlineFaint,
)

// FinAI is a fixed dark-brand experience (violet/cyan), so the theme does not
// follow system light/dark or dynamic wallpaper colors — it always renders the
// same look as the web app's dark "Quantexa Modern" scheme.
@Composable
fun FinAITheme(content: @Composable () -> Unit) {
    // Responsive type: text follows the screen width (designed for ~400dp phones) and the
    // user's font-size setting is honoured but capped, so very large system fonts or
    // "display size" settings can't make amounts and labels wrap or overflow.
    val density = androidx.compose.ui.platform.LocalDensity.current
    val widthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val widthFactor = (widthDp / 400f).coerceIn(0.85f, 1.1f)
    val fontScale = density.fontScale.coerceIn(0.85f, 1.15f) * widthFactor
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, fontScale)
    ) {
        MaterialTheme(
            colorScheme = FinAIDarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
