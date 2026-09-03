package com.stem.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp



// Stem Design System — Clean architectural geometry (4dp controls/inputs, 8dp cards, 12dp sheets/overlays, pill for status/chips)
val StemShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

val StemSharpShape = RoundedCornerShape(4.dp)
val StemCardShape = RoundedCornerShape(8.dp)
val StemSheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
val StemOverlayShape = RoundedCornerShape(12.dp)
val StemPillShape = RoundedCornerShape(100.dp)
val StemIndicatorShape = RoundedCornerShape(2.dp)


