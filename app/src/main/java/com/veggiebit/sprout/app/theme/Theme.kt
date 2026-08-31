package com.veggiebit.sprout.app.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val SproutLightColorScheme = lightColorScheme(
    primary = SproutAmberPrimary,
    onPrimary = SproutAmberOnPrimary,
    primaryContainer = SproutAmberPrimaryContainer,
    onPrimaryContainer = SproutAmberOnPrimaryContainer,
    secondary = SproutSecondary,
    onSecondary = SproutOnSecondary,
    secondaryContainer = SproutSecondaryContainer,
    onSecondaryContainer = SproutOnSecondaryContainer,
    tertiary = SproutTertiary,
    onTertiary = SproutOnTertiary,
    tertiaryContainer = SproutTertiaryContainer,
    onTertiaryContainer = SproutOnTertiaryContainer,
    background = SproutBackground,
    onBackground = SproutOnBackground,
    surface = SproutSurface,
    onSurface = SproutOnSurface,
    surfaceContainerLow = SproutSurfaceContainerLow,
    surfaceContainer = SproutSurfaceContainer,
    surfaceContainerHigh = SproutSurfaceContainerHigh,
    surfaceContainerHighest = SproutSurfaceContainerHighest,
    outline = SproutOutline,
    outlineVariant = SproutOutlineVariant
)

@Composable
fun SproutTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Strictly Light Mode Expressive Theme
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> SproutLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SproutTypography,
        shapes = SproutShapes,
        content = content
    )
}
