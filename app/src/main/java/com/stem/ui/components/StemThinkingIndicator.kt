package com.stem.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemMonoBadge
import com.stem.ui.theme.StemSharpShape



/**
 * Word-level skeleton animation displayed when Stem AI is actively "thinking" and transforming text.
 * Represents the words that will change as animated shimmering skeleton pills matching the
 * sentence structure and word lengths.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThinkingWordSkeleton(
    text: String = "",
    label: String = "Thinking...",
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "thinkingWordSkeleton")

    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerSkeletonTranslate"
    )

    val shimmerBrush = Brush.horizontalGradient(
        colors = listOf(
            stemTheme.surface2.copy(alpha = 0.5f),
            stemTheme.border.copy(alpha = 0.95f),
            stemTheme.surface2.copy(alpha = 0.5f)
        ),
        startX = shimmerTranslate - 250f,
        endX = shimmerTranslate + 250f
    )

    val words = remember(text) {
        val extracted = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (extracted.isNotEmpty()) extracted else listOf("Thinking", "and", "crafting", "polished", "sentence", "phrasing", "instant", "preview")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(StemSharpShape)
            .background(stemTheme.surface)
            .border(1.dp, stemTheme.border, StemSharpShape)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StemThinkingBadge(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label.uppercase(),
                        style = StemMonoBadge,
                        color = stemTheme.inkMuted
                    )
                }

                Text(
                    text = "REWRITING",
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                words.forEach { word ->
                    val charCount = word.length.coerceIn(2, 16)
                    val widthDp = (charCount * 7.5f + 10f).dp.coerceIn(22.dp, 130.dp)

                    Box(
                        modifier = Modifier
                            .size(width = widthDp, height = 15.dp)
                            .clip(StemSharpShape)
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

/**
 * Compact Rotating AI Sparkles Badge for the collapsed floating pill.
 */
@Composable
fun StemThinkingBadge(
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "badgeThinking")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "badgeRotation"
    )

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(stemTheme.surface2),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = "Thinking",
            tint = stemTheme.ink,
            modifier = Modifier
                .size(13.dp)
                .rotate(rotation)
        )
    }
}

