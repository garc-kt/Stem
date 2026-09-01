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



/**
 * Stem Preset Chips — Minimalist sharp-geometry chips with custom geometric icons.
 * Matches Stem.dc.html design specification.
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
            val bg = if (isSelected) stemTheme.ink else stemTheme.surface
            val fg = if (isSelected) stemTheme.onInk else stemTheme.ink
            val border = if (isSelected) stemTheme.ink else stemTheme.border

            Row(
                modifier = Modifier
                    .clip(StemSharpShape)
                    .background(bg)
                    .border(1.dp, border, StemSharpShape)
                    .clickable(
                        role = Role.RadioButton,
                        onClick = { onPresetSelected(preset) }
                    )
                    .semantics { selected = isSelected }
                    .padding(
                        horizontal = if (compact) 10.dp else 14.dp,
                        vertical = if (compact) 6.dp else 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.shortName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = if (compact) 12.sp else 13.sp
                    ),
                    color = fg
                )
            }
        }
    }
}


