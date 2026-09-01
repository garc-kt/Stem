package com.stem.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider



// Stem Design System Material 3 Color Schemes
// Source: Stem.dc.html

private val StemLightColorScheme = lightColorScheme(
    primary = StemLightInk,
    onPrimary = StemLightOnInk,
    primaryContainer = StemLightSurface2,
    onPrimaryContainer = StemLightInk,
    secondary = StemLightInkMuted,
    onSecondary = StemLightBg,
    secondaryContainer = StemLightSurface3,
    onSecondaryContainer = StemLightInk,
    tertiary = StemLightAdd,
    onTertiary = StemLightBg,
    tertiaryContainer = StemLightAddBg,
    onTertiaryContainer = StemLightAdd,
    error = StemLightRemove,
    onError = StemLightBg,
    errorContainer = StemLightRemoveBg,
    onErrorContainer = StemLightRemove,
    background = StemLightBg,
    onBackground = StemLightInk,
    surface = StemLightSurface,
    onSurface = StemLightInk,
    surfaceVariant = StemLightSurface2,
    onSurfaceVariant = StemLightInkMuted,
    surfaceContainerLow = StemLightSurface,
    surfaceContainer = StemLightSurface2,
    surfaceContainerHigh = StemLightSurface3,
    surfaceContainerHighest = StemLightBorder,
    outline = StemLightBorder,
    outlineVariant = StemLightBorder
)

private val StemDarkColorScheme = darkColorScheme(
    primary = StemDarkInk,
    onPrimary = StemDarkOnInk,
    primaryContainer = StemDarkSurface2,
    onPrimaryContainer = StemDarkInk,
    secondary = StemDarkInkMuted,
    onSecondary = StemDarkBg,
    secondaryContainer = StemDarkSurface3,
    onSecondaryContainer = StemDarkInk,
    tertiary = StemDarkAdd,
    onTertiary = StemDarkBg,
    tertiaryContainer = StemDarkAddBg,
    onTertiaryContainer = StemDarkAdd,
    error = StemDarkRemove,
    onError = StemDarkBg,
    errorContainer = StemDarkRemoveBg,
    onErrorContainer = StemDarkRemove,
    background = StemDarkBg,
    onBackground = StemDarkInk,
    surface = StemDarkSurface,
    onSurface = StemDarkInk,
    surfaceVariant = StemDarkSurface2,
    onSurfaceVariant = StemDarkInkMuted,
    surfaceContainerLow = StemDarkSurface,
    surfaceContainer = StemDarkSurface2,
    surfaceContainerHigh = StemDarkSurface3,
    surfaceContainerHighest = StemDarkBorder,
    outline = StemDarkBorder,
    outlineVariant = StemDarkBorder
)

@Composable
fun StemTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false, // Stem uses custom Warm-Stone neutral palette by design
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) StemDarkColorScheme else StemLightColorScheme
    val stemColors = if (useDarkTheme) StemDarkColors else StemLightColors

    CompositionLocalProvider(
        LocalStemColors provides stemColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = StemShapes,
            typography = StemTypography,
            content = content
        )
    }
}


