package com.veggiebit.sprout.features.overlay.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veggiebit.sprout.app.theme.SproutPillShape
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset

/**
 * 36dp height Collapsed Floating Pill. The icon badge gently pulses only while a suggestion is
 * available — the [rememberInfiniteTransition]/frame-callback animation is only ever composed
 * (and therefore only ever running) inside the `hasSuggestions` branch, so it's fully disposed
 * rather than idling forever the way the original always-present infinite transition did.
 *
 * (M3 Expressive's [androidx.compose.material3.MaterialShapes] polygon-morph badge was the
 * original design here, but `MaterialShapes`/`RoundedPolygon.toShape()` are `internal` — not
 * usable from app code — in this project's resolved material3:1.4.0.)
 *
 * Draggable: [onDrag] reports raw pointer deltas (in px) while dragging so the caller can
 * reposition the WindowManager layout live; [onDragEnd] fires once the gesture finishes so the
 * caller can snap-to-edge and persist the new anchor.
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
    val stateDescription = when {
        isTransforming -> "Thinking"
        hasSuggestions -> "Suggestion ready: ${activePreset.shortName}"
        else -> "No suggestion yet"
    }

    Box(
        // The pill's own chrome stays a slim 36dp (plan.md §3.2), but its tappable/draggable
        // region is padded out to the 48dp accessibility touch-target minimum so it isn't the
        // smallest target in the app despite being the single most-tapped element in it.
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(SproutPillShape)
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
                onClickLabel = "Expand Sprout suggestions",
                role = Role.Button,
                onClick = onExpandClick
            )
            .semantics {
                this.stateDescription = stateDescription
                // Announces to TalkBack when a suggestion becomes ready / thinking starts while
                // the user isn't actively exploring the pill — otherwise only the sighted pulse
                // animation / "Thinking..." text conveys that anything changed.
                liveRegion = LiveRegionMode.Polite
                customActions = listOf(
                    // Reuses the exact same drag-delta + snap-to-edge/persist path a real drag
                    // gesture uses (via the caller's onDrag/onDragEnd) — a huge delta lands
                    // comfortably past either edge, and the existing snap logic clamps and
                    // animates it back on-screen. This is the only way a TalkBack or
                    // switch-access user can relocate the pill; touch dragging has no
                    // equivalent for them.
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
                .height(36.dp)
                .shadow(elevation = 10.dp, shape = SproutPillShape)
                .clip(SproutPillShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                    shape = SproutPillShape
                ),
            shape = SproutPillShape,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isTransforming) {
                    com.veggiebit.sprout.features.enhancement.ui.components.SproutThinkingBadge()
                } else if (hasSuggestions) {
                    PulsingBadge()
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Spa,
                            contentDescription = "Sprout",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(7.dp))

                Text(
                    text = "Sprout",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isTransforming) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Thinking...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                } else if (hasSuggestions) {
                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activePreset.emoji,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = activePreset.shortName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Spa,
            contentDescription = "Sprout",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(15.dp)
        )
    }
}
