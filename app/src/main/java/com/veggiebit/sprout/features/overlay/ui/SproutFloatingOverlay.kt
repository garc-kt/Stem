package com.veggiebit.sprout.features.overlay.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veggiebit.sprout.app.theme.SproutCapsuleShape
import com.veggiebit.sprout.features.enhancement.data.engine.TransformHistory
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import com.veggiebit.sprout.features.enhancement.ui.components.DiffViewer
import com.veggiebit.sprout.features.enhancement.ui.components.PresetChipsRow
import com.veggiebit.sprout.features.overlay.ui.components.SproutPill

@Composable
fun SproutFloatingOverlay(
    payload: TextPayload?,
    transformResult: TransformResult?,
    isTransforming: Boolean = false,
    selectedPreset: TransformPreset,
    presets: List<TransformPreset> = TransformPreset.entries,
    isExpanded: Boolean,
    canUndo: Boolean,
    historyEntries: List<TransformHistory.Snapshot> = emptyList(),
    onExpandClick: () -> Unit,
    onCollapseClick: () -> Unit,
    onPresetSelected: (TransformPreset) -> Unit,
    onReplaceInline: (TransformResult) -> Unit,
    onCopyText: (String) -> Unit,
    onUndoClick: () -> Unit,
    onHistoryEntrySelected: (TransformHistory.Snapshot) -> Unit = {},
    onPillDrag: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    onPillDragEnd: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // MaterialTheme.motionScheme is internal in this project's resolved material3:1.4.0 (part
    // of the Expressive surface that isn't publicly accessible here), so this uses explicit
    // tween specs rather than the theme-driven motion spec.
    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)) { it / 4 })
                .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(180)) { it / 4 })
        },
        label = "overlayExpansion"
    ) { expanded ->
        if (!expanded) {
            SproutPill(
                activePreset = selectedPreset,
                hasSuggestions = transformResult?.hasChanges == true,
                isTransforming = isTransforming,
                onExpandClick = onExpandClick,
                onDrag = onPillDrag,
                onDragEnd = onPillDragEnd,
                modifier = modifier
            )
        } else {
            var showHistory by remember { mutableStateOf(false) }

            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .shadow(elevation = 16.dp, shape = SproutCapsuleShape)
                    .clip(SproutCapsuleShape)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                        shape = SproutCapsuleShape
                    ),
                shape = SproutCapsuleShape,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Spa,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Sprout Assistant",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isTransforming) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (payload?.packageName != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val appLabel = payload.packageName.substringAfterLast('.')
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = appLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onCollapseClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "Collapse",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Selection Chips (34dp)
                    PresetChipsRow(
                        selectedPreset = selectedPreset,
                        onPresetSelected = onPresetSelected,
                        presets = presets
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Diff / Comparison Area or Thinking State
                    if (isTransforming) {
                        com.veggiebit.sprout.features.enhancement.ui.components.SproutThinkingCard(
                            engineTitle = selectedPreset.title,
                            subtitle = "Generating ${selectedPreset.shortName} refinement..."
                        )
                    } else if (transformResult != null) {
                        if (transformResult.diffTokens.isNotEmpty()) {
                            DiffViewer(diffTokens = transformResult.diffTokens)
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = transformResult.transformedText.ifBlank { "No text detected." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = transformResult.summaryNote ?: "Transformation ready",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (transformResult.hasChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            if (transformResult.wordsSaved > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "-${transformResult.wordsSaved} words",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row — undo/history/copy as compact outlined icon buttons,
                    // primary Replace action taking the remaining width. (M3 Expressive's
                    // HorizontalFloatingToolbar was the original design here, but it and
                    // FloatingToolbarDefaults are internal — not usable from app code — in this
                    // project's resolved material3:1.4.0.)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { transformResult?.let(onReplaceInline) },
                            enabled = transformResult?.hasChanges == true,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Replace Inline", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        if (canUndo) {
                            OutlinedButton(
                                onClick = onUndoClick,
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                                    contentDescription = "Undo",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (historyEntries.size > 1) {
                            OutlinedButton(
                                onClick = { showHistory = !showHistory },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = "History",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { transformResult?.let { onCopyText(it.transformedText) } },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showHistory) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                items(historyEntries.asReversed(), key = { it.id }) { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = entry.originalText,
                                                style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = entry.replacedText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(onClick = { onHistoryEntrySelected(entry) }, modifier = Modifier.size(28.dp)) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                                contentDescription = "Restore",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
