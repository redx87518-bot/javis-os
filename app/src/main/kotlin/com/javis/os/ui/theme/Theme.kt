package com.javis.os.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CyanPrimary = Color(0xFF00E5FF)
val CyanSecondary = Color(0xFF00B8D4)
val PurpleAccent = Color(0xFF7C4DFF)
val BackgroundDark = Color(0xFF050A14)
val SurfaceDark = Color(0xFF0D1B2A)
val SurfaceVariant = Color(0xFF1A2A3A)
val OnSurfaceLight = Color(0xFFE0F7FA)
val ErrorRed = Color(0xFFFF5252)
val SuccessGreen = Color(0xFF69F0AE)
val WarningAmber = Color(0xFFFFD740)

private val JavisDarkColors = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = BackgroundDark,
    primaryContainer = Color(0xFF003545),
    onPrimaryContainer = CyanPrimary,
    secondary = PurpleAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF21005D),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = SuccessGreen,
    background = BackgroundDark,
    onBackground = OnSurfaceLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Color(0xFF90A4AE),
    error = ErrorRed,
    outline = Color(0xFF00B8D4).copy(alpha = 0.4f),
)

@Composable
fun JavisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JavisDarkColors,
        typography = JavisTypography,
        content = content
    )
}
