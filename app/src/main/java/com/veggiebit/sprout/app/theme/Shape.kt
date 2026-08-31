package com.veggiebit.sprout.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// The "Increased" shape roles (extraSmallIncreased, mediumIncreased, etc.) that ship with M3
// Expressive aren't available in this project's resolved material3:1.4.0 build — the Shapes
// constructor here only accepts the five base roles. Sprout's rounder, more expressive corner
// treatment for cards/containers is applied directly via RoundedCornerShape literals instead.
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
val SproutLargeIncreasedShape = RoundedCornerShape(20.dp) // Rounder container treatment
