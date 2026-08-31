package com.veggiebit.sprout.features.enhancement.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset

/**
 * Preset selector using stable M3's [FilterChip] — replaces a hand-rolled
 * Box+clickable(indication=null) row that had no ripple and no selection semantics, so
 * TalkBack announced nothing (a real gap for an accessibility-service app). FilterChip carries
 * real `selected` state semantics and a ripple for free. (M3 Expressive's `ToggleButton` was
 * the original choice here, but it — like the rest of the Expressive surface — is `internal`
 * in this project's resolved material3:1.4.0, not usable from app code.) A scrolling LazyRow
 * since the preset list has grown past what comfortably fits one row on a phone width.
 */
@Composable
fun PresetChipsRow(
    selectedPreset: TransformPreset,
    onPresetSelected: (TransformPreset) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<TransformPreset> = TransformPreset.entries
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            val isSelected = preset == selectedPreset
            FilterChip(
                selected = isSelected,
                onClick = { onPresetSelected(preset) },
                label = {
                    Text(
                        text = "${preset.emoji} ${preset.title}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
