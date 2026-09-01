package com.veggiebit.sprout.features.overlay.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veggiebit.sprout.app.theme.LocalStemColors
import com.veggiebit.sprout.app.theme.StemMonoBadge
import com.veggiebit.sprout.app.theme.StemOverlayShape
import com.veggiebit.sprout.app.theme.StemSharpShape
import com.veggiebit.sprout.features.enhancement.data.engine.TransformHistory
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import com.veggiebit.sprout.features.enhancement.ui.components.BeforeAfterDiffBlock
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
    val stemTheme = LocalStemColors.current

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
                    .shadow(elevation = 16.dp, shape = StemOverlayShape)
                    .clip(StemOverlayShape)
                    .border(
                        width = 1.dp,
                        color = stemTheme.border,
                        shape = StemOverlayShape
                    ),
                shape = StemOverlayShape,
                color = stemTheme.surface
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
                            Text(
                                text = "STEM",
                                style = StemMonoBadge,
                                color = stemTheme.inkMuted
                            )

                            if (isTransforming) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = stemTheme.ink
                                )
                            }

                            if (payload?.packageName != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val appLabel = payload.packageName.substringAfterLast('.')
                                Box(
                                    modifier = Modifier
                                        .clip(StemSharpShape)
                                        .background(stemTheme.surface2)
                                        .border(1.dp, stemTheme.border, StemSharpShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = appLabel,
                                        style = StemMonoBadge,
                                        color = stemTheme.inkMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (canUndo) {
                                IconButton(onClick = onUndoClick, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Undo,
                                        contentDescription = "Undo",
                                        tint = stemTheme.inkMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (historyEntries.size > 1) {
                                IconButton(onClick = { showHistory = !showHistory }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = "History",
                                        tint = stemTheme.inkMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { transformResult?.let { onCopyText(it.transformedText) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = stemTheme.inkMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Selection Chips (Compact)
                    PresetChipsRow(
                        selectedPreset = selectedPreset,
                        onPresetSelected = onPresetSelected,
                        presets = presets,
                        compact = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Diff / Comparison Area
                    if (isTransforming) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(StemSharpShape)
                                .background(stemTheme.bg)
                                .border(1.dp, stemTheme.border, StemSharpShape)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Applying ${selectedPreset.title}...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = stemTheme.inkMuted
                            )
                        }
                    } else if (transformResult != null) {
                        if (selectedPreset.useDiff && transformResult.diffTokens.isNotEmpty()) {
                            DiffViewer(diffTokens = transformResult.diffTokens)
                        } else {
                            BeforeAfterDiffBlock(
                                beforeText = transformResult.originalText,
                                afterText = transformResult.transformedText
                            )
                        }

                        if (transformResult.wordsSaved > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "-${transformResult.wordsSaved} words",
                                style = StemMonoBadge,
                                color = stemTheme.add
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons: Dismiss & Replace
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(StemSharpShape)
                                .border(1.dp, stemTheme.border, StemSharpShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = onDismiss
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = stemTheme.ink
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(StemSharpShape)
                                .background(if (transformResult?.hasChanges == true) stemTheme.ink else stemTheme.surface2)
                                .clickable(
                                    enabled = transformResult?.hasChanges == true,
                                    role = Role.Button,
                                    onClick = { transformResult?.let(onReplaceInline) }
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Replace",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (transformResult?.hasChanges == true) stemTheme.onInk else stemTheme.inkFaint
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
                                    .clip(StemSharpShape)
                                    .background(stemTheme.bg)
                                    .border(1.dp, stemTheme.border, StemSharpShape)
                            ) {
                                items(historyEntries.asReversed(), key = { it.id }) { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = entry.originalText,
                                                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                                                color = stemTheme.inkMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = entry.replacedText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = stemTheme.ink,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { onHistoryEntrySelected(entry) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                                contentDescription = "Restore",
                                                tint = stemTheme.ink,
                                                modifier = Modifier.size(14.dp)
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

