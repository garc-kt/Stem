package com.veggiebit.sprout.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SproutShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val SproutPillShape = RoundedCornerShape(18.dp) // 36dp height pill
val SproutCapsuleShape = RoundedCornerShape(28.dp) // Expanded capsule
val SproutChipShape = RoundedCornerShape(16.dp) // 32dp height chip
