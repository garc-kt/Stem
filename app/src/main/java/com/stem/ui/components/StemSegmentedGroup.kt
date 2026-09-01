package com.stem.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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



/**
 * High-craft Stem Segmented Group with sharp geometric aesthetics.
 */
@Composable
fun <T> StemSegmentedGroup(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(StemSharpShape)
            .background(stemTheme.surface2)
            .border(1.dp, stemTheme.border, StemSharpShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected

            val targetBg = if (isSelected) stemTheme.ink else stemTheme.surface2
            val targetFg = if (isSelected) stemTheme.onInk else stemTheme.inkMuted

            val bg by animateColorAsState(targetValue = targetBg, animationSpec = tween(150), label = "segBg")
            val fg by animateColorAsState(targetValue = targetFg, animationSpec = tween(150), label = "segFg")

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(StemSharpShape)
                    .background(bg)
                    .clickable(
                        role = Role.RadioButton,
                        onClick = { onSelected(option) }
                    )
                    .semantics { this.selected = isSelected }
                    .padding(vertical = 7.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.5.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = fg,
                    maxLines = 1
                )
            }
        }
    }
}
