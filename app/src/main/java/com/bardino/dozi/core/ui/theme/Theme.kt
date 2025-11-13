package com.bardino.dozi.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Scheme artık DoziPrimary'yi kullanır
private val LightColorScheme = lightColorScheme(
    primary = DoziPrimary,
    onPrimary = Color.White,
    primaryContainer = DoziPrimaryLight,
    onPrimaryContainer = DoziPrimaryDark,

    secondary = DoziAccent,
    onSecondary = Color.White,
    secondaryContainer = DoziAccentLight,
    onSecondaryContainer = DoziAccentDark,

    error = ErrorRed,
    onError = Color.White,

    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,

    outline = DoziPrimaryLight,
    surfaceTint = DoziPrimary
)

// Dark Scheme
private val DarkColorScheme = darkColorScheme(
    primary = DoziPrimary,
    onPrimary = Color.White,
    secondary = DoziAccent,
    onSecondary = Color.White,
    error = ErrorRed,
    onError = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    outline = Color(0xFF383838)
)

@Composable
fun DoziAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = DoziTypography, // Typography ve Shapes'in tanımlı olduğu varsayılır
        shapes = DoziShapes,
        content = content
    )
}

// Ek özellikler
val ColorScheme.surfaceElevated: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.red > 0.5f) { // isLight() kontrolü
        Color.White
    } else {
        Color(0xFF2C2C2E)
    }