package com.stem.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemCardShape
import com.stem.ui.theme.StemMonoBadge
import com.stem.ui.theme.StemPillShape
import com.stem.ui.theme.StemSharpShape

/**
 * Minimalist surface container matching Stem's warm-stone architectural aesthetic.
 * Encapsulates rounded geometry, subtle borders, and optional tactile interactions.
 */
@Composable
fun StemCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    role: Role? = null,
    enabled: Boolean = true,
    shape: Shape = StemCardShape,
    backgroundColor: Color = LocalStemColors.current.surface,
    borderColor: Color = LocalStemColors.current.borderSubtle,
    borderWidth: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(backgroundColor)
        .then(
            if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape)
            else Modifier
        )

    val clickableModifier = if (onClick != null) {
        baseModifier.clickable(
            enabled = enabled,
            role = role ?: Role.Button,
            onClick = onClick
        )
    } else {
        baseModifier
    }

    Column(
        modifier = clickableModifier.padding(contentPadding),
        content = content
    )
}

/**
 * Minimalist section header with mono uppercase title and optional action or count.
 */
@Composable
fun StemSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val stemTheme = LocalStemColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title.uppercase(),
                style = StemMonoBadge,
                color = stemTheme.inkFaint
            )
            if (action != null) {
                action()
            }
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = stemTheme.inkMuted
            )
        }
    }
}

/**
 * Minimalist status badge pill with colored live dot and optional pulsing micro-animation.
 */
@Composable
fun StemStatusPill(
    text: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = LocalStemColors.current.surface2,
    textColor: Color = LocalStemColors.current.ink
) {
    val stemTheme = LocalStemColors.current

    val pulseScale = if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "pillPulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        scale
    } else 1f

    val baseModifier = modifier
        .clip(StemPillShape)
        .background(backgroundColor)
        .border(1.dp, stemTheme.borderSubtle, StemPillShape)
        .then(
            if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick)
            else Modifier
        )
        .padding(horizontal = 10.dp, vertical = 4.dp)

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            style = StemMonoBadge.copy(fontWeight = FontWeight.Medium),
            color = textColor
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = StemMonoBadge,
                color = stemTheme.inkFaint
            )
        }
    }
}

/**
 * Minimalist interactive button matching Stem's warm-stone architectural aesthetic.
 */
@Composable
fun StemButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String? = null
) {
    val stemTheme = LocalStemColors.current

    val bg = when {
        isLoading -> stemTheme.surface2
        !enabled -> stemTheme.surface2
        isPrimary -> stemTheme.ink
        else -> stemTheme.surface2
    }

    val fg = when {
        isLoading -> stemTheme.inkMuted
        !enabled -> stemTheme.inkFaint
        isPrimary -> stemTheme.onInk
        else -> stemTheme.ink
    }

    val border = when {
        isPrimary && enabled && !isLoading -> stemTheme.ink
        else -> stemTheme.borderSubtle
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(StemSharpShape)
            .background(bg)
            .border(1.dp, border, StemSharpShape)
            .clickable(
                role = Role.Button,
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 1.5.dp,
                    color = fg
                )
                Text(
                    text = loadingText ?: text,
                    style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                    color = fg
                )
            }
        } else {
            Text(
                text = text,
                style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                color = fg
            )
        }
    }
}
