package com.stem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemSharpShape
import com.stem.core.models.TransformPreset



import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.getValue
import com.stem.ui.theme.StemPillShape

/**
 * Stem Preset Chips — Minimalist chips with smooth animated selection and clean geometry.
 */
@Composable
fun PresetChipsRow(
    selectedPreset: TransformPreset,
    onPresetSelected: (TransformPreset) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<TransformPreset> = TransformPreset.entries,
    compact: Boolean = false
) {
    val stemTheme = LocalStemColors.current

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            val isSelected = preset == selectedPreset

            val targetBg = if (isSelected) stemTheme.ink else stemTheme.surface
            val targetFg = if (isSelected) stemTheme.onInk else stemTheme.ink
            val targetBorder = if (isSelected) stemTheme.ink else stemTheme.borderSubtle

            val bg by animateColorAsState(targetValue = targetBg, animationSpec = tween(150), label = "chipBg")
            val fg by animateColorAsState(targetValue = targetFg, animationSpec = tween(150), label = "chipFg")
            val border by animateColorAsState(targetValue = targetBorder, animationSpec = tween(150), label = "chipBorder")

            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = if (compact) 32.dp else 36.dp)
                    .clip(StemPillShape)
                    .background(bg)
                    .border(1.dp, border, StemPillShape)
                    .clickable(
                        role = Role.RadioButton,
                        onClick = { onPresetSelected(preset) }
                    )
                    .semantics { selected = isSelected }
                    .padding(
                        horizontal = if (compact) 12.dp else 16.dp,
                        vertical = if (compact) 6.dp else 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.shortName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (compact) 12.sp else 13.sp,
                        letterSpacing = 0.1.sp
                    ),
                    color = fg
                )
            }
        }
    }
}


