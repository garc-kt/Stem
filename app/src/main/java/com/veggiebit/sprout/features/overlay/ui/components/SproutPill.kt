package com.veggiebit.sprout.features.overlay.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.veggiebit.sprout.app.theme.LocalStemColors
import com.veggiebit.sprout.app.theme.StemLogoMark
import com.veggiebit.sprout.app.theme.StemPillShape
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset

/**
 * Stem Collapsed Floating Pill — 40x40dp sharp rounded square in ink with Stem mark in onInk.
 * Matches Stem.dc.html design specification.
 */
@Composable
fun SproutPill(
    activePreset: TransformPreset,
    hasSuggestions: Boolean,
    isTransforming: Boolean = false,
    onExpandClick: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current

    val stateDescription = when {
        isTransforming -> "Thinking"
        hasSuggestions -> "Suggestion ready: ${activePreset.shortName}"
        else -> "No suggestion yet"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasSuggestions || isTransforming) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(StemPillShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = onDragEnd
                )
            }
            .clickable(
                onClickLabel = "Expand Stem suggestions",
                role = Role.Button,
                onClick = onExpandClick
            )
            .semantics {
                this.stateDescription = stateDescription
                liveRegion = LiveRegionMode.Polite
                customActions = listOf(
                    CustomAccessibilityAction("Move to left edge") {
                        onDrag(-100000f, 0f)
                        onDragEnd()
                        true
                    },
                    CustomAccessibilityAction("Move to right edge") {
                        onDrag(100000f, 0f)
                        onDragEnd()
                        true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .scale(if (hasSuggestions || isTransforming) pulseScale else 1f)
                .shadow(elevation = 8.dp, shape = StemPillShape)
                .clip(StemPillShape),
            shape = StemPillShape,
            color = stemTheme.ink
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                StemLogoMark(
                    size = 20.dp,
                    tint = stemTheme.onInk
                )
            }
        }
    }
}

