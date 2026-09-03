package com.stem.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stem.R
import com.stem.engine.TransformHistory
import com.stem.ui.components.BeforeAfterDiffBlock
import com.stem.ui.components.StemButton
import com.stem.ui.components.StemCard
import com.stem.ui.components.StemSectionHeader
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemCardShape
import com.stem.ui.theme.StemMonoBadge

/**
 * Stem History Screen:
 * Clean, minimalist journal of recent transformations with:
 * - Expandable cards with Before/After visual comparison
 * - One-tap copy action
 * - Accidental data-loss prevention for clearing history
 */
@Composable
fun HistoryScreen(
    history: List<TransformHistory.Snapshot>,
    modifier: Modifier = Modifier,
    onCopy: (String) -> Unit = {},
    onClearHistory: () -> Unit = {}
) {
    val stemTheme = LocalStemColors.current
    val context = LocalContext.current
    val copiedToastMessage = stringResource(R.string.action_copied)
    var expandedId by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .background(stemTheme.bg)
    ) {
        item {
            StemSectionHeader(
                title = stringResource(R.string.nav_tab_history),
                subtitle = stringResource(R.string.history_subtitle),
                action = {
                    if (history.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.history_clear_button),
                            style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                            color = stemTheme.remove,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClick = { showClearDialog = true })
                                .padding(4.dp)
                        )
                    }
                }
            )
        }

        if (history.isEmpty()) {
            item {
                StemCard(contentPadding = PaddingValues(32.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.history_empty_title),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = stemTheme.inkMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.history_empty_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = stemTheme.inkFaint
                        )
                    }
                }
            }
        } else {
            items(history.asReversed(), key = { it.id }) { item ->
                val isExpanded = expandedId == item.id

                StemCard(
                    onClick = { expandedId = if (isExpanded) null else item.id },
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatHistoryCommand(item.presetName),
                            style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                            color = stemTheme.ink
                        )

                        Text(
                            text = formatTimeAgo(item.timestamp),
                            style = StemMonoBadge,
                            color = stemTheme.inkFaint
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isExpanded) {
                        Text(
                            text = item.originalText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = stemTheme.remove,
                                textDecoration = TextDecoration.LineThrough
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.replacedText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = stemTheme.add,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            BeforeAfterDiffBlock(
                                beforeText = item.originalText,
                                afterText = item.replacedText
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                StemButton(
                                    text = stringResource(R.string.history_copy_button),
                                    onClick = {
                                        onCopy(item.replacedText)
                                        Toast.makeText(context, copiedToastMessage, Toast.LENGTH_SHORT).show()
                                    },
                                    isPrimary = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Accidental data-loss prevention: confirmation dialog before wiping history
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.history_clear_button) + "?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = stemTheme.ink
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.history_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = stemTheme.inkMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.history_clear_button),
                        style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                        color = stemTheme.remove
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(
                        text = stringResource(R.string.action_close),
                        style = StemMonoBadge,
                        color = stemTheme.inkMuted
                    )
                }
            },
            containerColor = stemTheme.surface,
            shape = StemCardShape
        )
    }
}

fun formatHistoryCommand(presetName: String): String {
    val clean = presetName.trim()
    return when {
        clean.startsWith("?") || clean.startsWith("..") || clean.startsWith(".") || clean.startsWith(";;") -> clean
        clean.isBlank() -> "?enhance"
        else -> "?$clean"
    }
}

@Composable
private fun formatTimeAgo(timestamp: Long): String {
    val deltaMs = System.currentTimeMillis() - timestamp
    val deltaMins = deltaMs / (1000 * 60)
    return when {
        deltaMins < 1 -> stringResource(R.string.history_time_just_now)
        deltaMins < 60 -> stringResource(R.string.history_time_minutes_ago, deltaMins)
        deltaMins < 1440 -> stringResource(R.string.history_time_hours_ago, deltaMins / 60)
        else -> stringResource(R.string.history_time_days_ago, deltaMins / 1440)
    }
}
