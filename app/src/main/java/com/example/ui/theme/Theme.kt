package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HighContrastColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberOnContainer,
    secondary = RadarCyan,
    onSecondary = Color.Black,
    secondaryContainer = RadarCyanDark,
    onSecondaryContainer = RadarCyan,
    tertiary = DangerRed,
    onTertiary = Color.White,
    tertiaryContainer = DangerRedContainer,
    onTertiaryContainer = DangerRed,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force high contrast palette for accessibility
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HighContrastColorScheme,
        typography = Typography,
        content = content
    )
}
