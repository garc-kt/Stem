package com.veggiebit.sprout.app.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val GoogleMonetLightColorScheme = lightColorScheme(
    primary = GoogleMonetPrimary,
    onPrimary = GoogleMonetOnPrimary,
    primaryContainer = GoogleMonetPrimaryContainer,
    onPrimaryContainer = GoogleMonetOnPrimaryContainer,
    secondary = GoogleMonetSecondary,
    onSecondary = GoogleMonetOnSecondary,
    secondaryContainer = GoogleMonetSecondaryContainer,
    onSecondaryContainer = GoogleMonetOnSecondaryContainer,
    tertiary = GoogleMonetTertiary,
    onTertiary = GoogleMonetOnTertiary,
    tertiaryContainer = GoogleMonetTertiaryContainer,
    onTertiaryContainer = GoogleMonetOnTertiaryContainer,
    background = GoogleMonetBackground,
    onBackground = GoogleMonetOnBackground,
    surface = GoogleMonetSurface,
    onSurface = GoogleMonetOnSurface,
    surfaceContainerLow = GoogleMonetSurfaceContainerLow,
    surfaceContainer = GoogleMonetSurfaceContainer,
    surfaceContainerHigh = GoogleMonetSurfaceContainerHigh,
    surfaceContainerHighest = GoogleMonetSurfaceContainerHighest,
    outline = GoogleMonetOutline,
    outlineVariant = GoogleMonetOutlineVariant
)

@Composable
fun SproutTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Google Material You Dynamic Colors (Monet) on Android 12+, Pixel Google palette on older versions
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> GoogleMonetLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SproutTypography,
        shapes = SproutShapes,
        content = content
    )
}
