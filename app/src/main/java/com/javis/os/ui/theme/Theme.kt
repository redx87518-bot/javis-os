package com.javis.os.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JavisDarkColorScheme = darkColorScheme(
    primary = JavisCyan,
    onPrimary = BackgroundDark,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = JavisCyan,
    secondary = JavisBlue,
    onSecondary = TextPrimary,
    secondaryContainer = CardDark,
    onSecondaryContainer = TextPrimary,
    tertiary = JavisGreen,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    error = JavisRed,
    onError = TextPrimary
)

@Composable
fun JavisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JavisDarkColorScheme,
        typography = JavisTypography,
        content = content
    )
}
