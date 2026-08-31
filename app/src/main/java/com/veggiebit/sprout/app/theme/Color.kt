package com.veggiebit.sprout.app.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Sprout brand seed — Citrus/Amber primary with Sprout Green secondary, per plan.md §3.1 and
// ARCHITECTURE_AND_PROCESS.md §2 Phase 5. Used as the static fallback scheme on API < 31 and
// whenever dynamic color is disabled; Android 12+ devices use dynamicLight/DarkColorScheme()
// (wallpaper-derived Monet) as the primary path — see Theme.kt.

// --- Light scheme -----------------------------------------------------------------------

val SproutLightPrimary = Color(0xFF8A4B00)
val SproutLightOnPrimary = Color(0xFFFFFFFF)
val SproutLightPrimaryContainer = Color(0xFFFFDCBE)
val SproutLightOnPrimaryContainer = Color(0xFF2C1600)

val SproutLightSecondary = Color(0xFF3A6A1F)
val SproutLightOnSecondary = Color(0xFFFFFFFF)
val SproutLightSecondaryContainer = Color(0xFFBAF397)
val SproutLightOnSecondaryContainer = Color(0xFF0B2100)

val SproutLightTertiary = Color(0xFF00696B)
val SproutLightOnTertiary = Color(0xFFFFFFFF)
val SproutLightTertiaryContainer = Color(0xFF9CF1F2)
val SproutLightOnTertiaryContainer = Color(0xFF002020)

val SproutLightError = Color(0xFFBA1A1A)
val SproutLightOnError = Color(0xFFFFFFFF)
val SproutLightErrorContainer = Color(0xFFFFDAD6)
val SproutLightOnErrorContainer = Color(0xFF410002)

val SproutLightBackground = Color(0xFFFFFBFF)
val SproutLightOnBackground = Color(0xFF201B13)
val SproutLightSurface = Color(0xFFFFFBFF)
val SproutLightOnSurface = Color(0xFF201B13)
val SproutLightSurfaceVariant = Color(0xFFF0E0D0)
val SproutLightOnSurfaceVariant = Color(0xFF504536)

val SproutLightSurfaceContainerLow = Color(0xFFFBF1E7)
val SproutLightSurfaceContainer = Color(0xFFF5EBE1)
val SproutLightSurfaceContainerHigh = Color(0xFFEFE5DB)
val SproutLightSurfaceContainerHighest = Color(0xFFE9E0D5)

val SproutLightOutline = Color(0xFF827567)
val SproutLightOutlineVariant = Color(0xFFD4C4B4)

// --- Dark scheme -------------------------------------------------------------------------

val SproutDarkPrimary = Color(0xFFFFB876)
val SproutDarkOnPrimary = Color(0xFF4A2800)
val SproutDarkPrimaryContainer = Color(0xFF693A00)
val SproutDarkOnPrimaryContainer = Color(0xFFFFDCBE)

val SproutDarkSecondary = Color(0xFF9FD67F)
val SproutDarkOnSecondary = Color(0xFF123900)
val SproutDarkSecondaryContainer = Color(0xFF255100)
val SproutDarkOnSecondaryContainer = Color(0xFFBAF397)

val SproutDarkTertiary = Color(0xFF4DD9DB)
val SproutDarkOnTertiary = Color(0xFF003738)
val SproutDarkTertiaryContainer = Color(0xFF004F51)
val SproutDarkOnTertiaryContainer = Color(0xFF9CF1F2)

val SproutDarkError = Color(0xFFFFB4AB)
val SproutDarkOnError = Color(0xFF690005)
val SproutDarkErrorContainer = Color(0xFF93000A)
val SproutDarkOnErrorContainer = Color(0xFFFFDAD6)

val SproutDarkBackground = Color(0xFF17130D)
val SproutDarkOnBackground = Color(0xFFEAE1D6)
val SproutDarkSurface = Color(0xFF17130D)
val SproutDarkOnSurface = Color(0xFFEAE1D6)
val SproutDarkSurfaceVariant = Color(0xFF504536)
val SproutDarkOnSurfaceVariant = Color(0xFFD4C4B4)

val SproutDarkSurfaceContainerLow = Color(0xFF201B13)
val SproutDarkSurfaceContainer = Color(0xFF241F17)
val SproutDarkSurfaceContainerHigh = Color(0xFF2F2921)
val SproutDarkSurfaceContainerHighest = Color(0xFF3A342B)

val SproutDarkOutline = Color(0xFF9C8F7F)
val SproutDarkOutlineVariant = Color(0xFF504536)

/**
 * Diff-viewer accent colors. Kept separate from the M3 color scheme (rather than reused as
 * literal light-mode hexes as before) because they need distinct light/dark pairs to stay
 * legible — the old hardcoded values were unreadable once dark mode shipped.
 */
data class SproutExtendedColors(
    val diffAdded: Color,
    val diffAddedBackground: Color,
    val diffDeleted: Color,
    val diffDeletedBackground: Color
)

val SproutLightExtendedColors = SproutExtendedColors(
    diffAdded = Color(0xFF1E8E3E),
    diffAddedBackground = Color(0xFFE6F4EA),
    diffDeleted = Color(0xFFD93025),
    diffDeletedBackground = Color(0xFFFCE8E6)
)

val SproutDarkExtendedColors = SproutExtendedColors(
    diffAdded = Color(0xFF7DDA8F),
    diffAddedBackground = Color(0xFF0F3D1A),
    diffDeleted = Color(0xFFFFB4A9),
    diffDeletedBackground = Color(0xFF4B120C)
)

val LocalSproutExtendedColors = staticCompositionLocalOf { SproutLightExtendedColors }
