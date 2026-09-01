package com.stem.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp



// Stem Design System — Sharp geometry (2dp–4dp radii for controls/cards, 10dp for sheets/overlays)
// Source: Stem.dc.html
val StemShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp)
)

val StemSharpShape = RoundedCornerShape(3.dp)
val StemCardShape = RoundedCornerShape(4.dp)
val StemSheetShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
val StemOverlayShape = RoundedCornerShape(10.dp)
val StemIndicatorShape = RoundedCornerShape(2.dp)


