package com.stem.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color



// Stem Design System — Warm-stone neutrals, sharp geometry, Manrope + Space Mono,
// one desaturated accent pair reserved for diff states.
// Source: Stem.dc.html

// --- Light Theme Tokens -----------------------------------------------------------------

val StemLightBg = Color(0xFFFAF8F5)
val StemLightSurface = Color(0xFFFCFAF8)
val StemLightSurface2 = Color(0xFFF2EFE9)
val StemLightSurface3 = Color(0xFFE6E2DA)
val StemLightBorder = Color(0xFFD7D2C7)
val StemLightInk = Color(0xFF282521)
val StemLightInkMuted = Color(0xFF6E685F)
val StemLightInkFaint = Color(0xFF9E978C)
val StemLightOnInk = Color(0xFFFAF8F5)
val StemLightAdd = Color(0xFF2E7D47)
val StemLightAddBg = Color(0xFFEBF6EE)
val StemLightRemove = Color(0xFFA14B3B)
val StemLightRemoveBg = Color(0xFFF9EBE8)

// --- Dark Theme Tokens ------------------------------------------------------------------

val StemDarkBg = Color(0xFF23211E)
val StemDarkSurface = Color(0xFF2D2A26)
val StemDarkSurface2 = Color(0xFF37342F)
val StemDarkSurface3 = Color(0xFF44403B)
val StemDarkBorder = Color(0xFF4E4943)
val StemDarkInk = Color(0xFFF1EFEB)
val StemDarkInkMuted = Color(0xFFB4ADA3)
val StemDarkInkFaint = Color(0xFF847E74)
val StemDarkOnInk = Color(0xFF23211E)
val StemDarkAdd = Color(0xFF6ECB8E)
val StemDarkAddBg = Color(0xFF1A3824)
val StemDarkRemove = Color(0xFFE98C76)
val StemDarkRemoveBg = Color(0xFF422019)

// --- Extended & Semantic Colors ---------------------------------------------------------

data class StemColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val border: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val onInk: Color,
    val add: Color,
    val addBg: Color,
    val remove: Color,
    val removeBg: Color,
    val isDark: Boolean
)

val StemLightColors = StemColors(
    bg = StemLightBg,
    surface = StemLightSurface,
    surface2 = StemLightSurface2,
    surface3 = StemLightSurface3,
    border = StemLightBorder,
    ink = StemLightInk,
    inkMuted = StemLightInkMuted,
    inkFaint = StemLightInkFaint,
    onInk = StemLightOnInk,
    add = StemLightAdd,
    addBg = StemLightAddBg,
    remove = StemLightRemove,
    removeBg = StemLightRemoveBg,
    isDark = false
)

val StemDarkColors = StemColors(
    bg = StemDarkBg,
    surface = StemDarkSurface,
    surface2 = StemDarkSurface2,
    surface3 = StemDarkSurface3,
    border = StemDarkBorder,
    ink = StemDarkInk,
    inkMuted = StemDarkInkMuted,
    inkFaint = StemDarkInkFaint,
    onInk = StemDarkOnInk,
    add = StemDarkAdd,
    addBg = StemDarkAddBg,
    remove = StemDarkRemove,
    removeBg = StemDarkRemoveBg,
    isDark = true
)

val LocalStemColors = staticCompositionLocalOf { StemLightColors }


