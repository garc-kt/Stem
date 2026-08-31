package com.veggiebit.sprout.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val SproutLightColorScheme = lightColorScheme(
    primary = SproutLightPrimary,
    onPrimary = SproutLightOnPrimary,
    primaryContainer = SproutLightPrimaryContainer,
    onPrimaryContainer = SproutLightOnPrimaryContainer,
    secondary = SproutLightSecondary,
    onSecondary = SproutLightOnSecondary,
    secondaryContainer = SproutLightSecondaryContainer,
    onSecondaryContainer = SproutLightOnSecondaryContainer,
    tertiary = SproutLightTertiary,
    onTertiary = SproutLightOnTertiary,
    tertiaryContainer = SproutLightTertiaryContainer,
    onTertiaryContainer = SproutLightOnTertiaryContainer,
    error = SproutLightError,
    onError = SproutLightOnError,
    errorContainer = SproutLightErrorContainer,
    onErrorContainer = SproutLightOnErrorContainer,
    background = SproutLightBackground,
    onBackground = SproutLightOnBackground,
    surface = SproutLightSurface,
    onSurface = SproutLightOnSurface,
    surfaceVariant = SproutLightSurfaceVariant,
    onSurfaceVariant = SproutLightOnSurfaceVariant,
    surfaceContainerLow = SproutLightSurfaceContainerLow,
    surfaceContainer = SproutLightSurfaceContainer,
    surfaceContainerHigh = SproutLightSurfaceContainerHigh,
    surfaceContainerHighest = SproutLightSurfaceContainerHighest,
    outline = SproutLightOutline,
    outlineVariant = SproutLightOutlineVariant
)

private val SproutDarkColorScheme = darkColorScheme(
    primary = SproutDarkPrimary,
    onPrimary = SproutDarkOnPrimary,
    primaryContainer = SproutDarkPrimaryContainer,
    onPrimaryContainer = SproutDarkOnPrimaryContainer,
    secondary = SproutDarkSecondary,
    onSecondary = SproutDarkOnSecondary,
    secondaryContainer = SproutDarkSecondaryContainer,
    onSecondaryContainer = SproutDarkOnSecondaryContainer,
    tertiary = SproutDarkTertiary,
    onTertiary = SproutDarkOnTertiary,
    tertiaryContainer = SproutDarkTertiaryContainer,
    onTertiaryContainer = SproutDarkOnTertiaryContainer,
    error = SproutDarkError,
    onError = SproutDarkOnError,
    errorContainer = SproutDarkErrorContainer,
    onErrorContainer = SproutDarkOnErrorContainer,
    background = SproutDarkBackground,
    onBackground = SproutDarkOnBackground,
    surface = SproutDarkSurface,
    onSurface = SproutDarkOnSurface,
    surfaceVariant = SproutDarkSurfaceVariant,
    onSurfaceVariant = SproutDarkOnSurfaceVariant,
    surfaceContainerLow = SproutDarkSurfaceContainerLow,
    surfaceContainer = SproutDarkSurfaceContainer,
    surfaceContainerHigh = SproutDarkSurfaceContainerHigh,
    surfaceContainerHighest = SproutDarkSurfaceContainerHighest,
    outline = SproutDarkOutline,
    outlineVariant = SproutDarkOutlineVariant
)

/**
 * Sprout's Material 3 theme: Sprout's shape system, and dynamic (Monet) color on Android 12+ in
 * both light and dark, falling back to the hand-authored Citrus/Amber + Sprout Green brand
 * scheme (see Color.kt) below API 31 or when dynamic color is turned off.
 *
 * Note: `MaterialExpressiveTheme`/`MotionScheme` are not used here — in the resolved
 * material3:1.4.0 build of this project they (and `ExperimentalMaterial3ExpressiveApi` itself)
 * are `internal`, not publicly accessible, so this sticks to the stable `MaterialTheme` API.
 */
@Composable
fun SproutTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        dynamicColor && supportsDynamicColor && useDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && supportsDynamicColor && !useDarkTheme -> dynamicLightColorScheme(LocalContext.current)
        useDarkTheme -> SproutDarkColorScheme
        else -> SproutLightColorScheme
    }

    val extendedColors = if (useDarkTheme) SproutDarkExtendedColors else SproutLightExtendedColors

    CompositionLocalProvider(LocalSproutExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = SproutShapes,
            typography = SproutTypography,
            content = content
        )
    }
}
