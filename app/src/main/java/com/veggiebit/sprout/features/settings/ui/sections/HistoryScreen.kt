package com.veggiebit.sprout.features.settings.ui.sections

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veggiebit.sprout.app.theme.LocalStemColors
import com.veggiebit.sprout.app.theme.StemCardShape
import com.veggiebit.sprout.app.theme.StemMonoBadge
import com.veggiebit.sprout.app.theme.StemSharpShape
import com.veggiebit.sprout.features.enhancement.data.engine.TransformHistory
import com.veggiebit.sprout.features.enhancement.ui.components.BeforeAfterDiffBlock

/**
 * Stem History Screen:
 * - Subtitle & expandable history snapshot cards
 * - Inline diff display with before/after blocks
 * - Copy and clear actions
 * Matches Stem.dc.html design specification.
 */
@Composable
fun HistoryScreen(
    history: List<TransformHistory.Snapshot>,
    onCopy: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current
    val context = LocalContext.current
    var expandedId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .background(stemTheme.bg)
    ) {
        // Subtitle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Transformations from the last 7 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = stemTheme.inkMuted
                )

                if (history.isNotEmpty()) {
                    Text(
                        text = "CLEAR",
                        style = StemMonoBadge,
                        color = stemTheme.remove,
                        modifier = Modifier
                            .clickable(role = Role.Button, onClick = onClearHistory)
                            .padding(4.dp)
                    )
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(StemCardShape)
                        .background(stemTheme.surface)
                        .border(1.dp, stemTheme.border, StemCardShape)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No transformation history yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = stemTheme.inkMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rewrites applied via Stem will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = stemTheme.inkFaint
                        )
                    }
                }
            }
        } else {
            items(history.asReversed(), key = { it.id }) { item ->
                val isExpanded = expandedId == item.id

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(StemCardShape)
                        .background(stemTheme.surface)
                        .border(1.dp, stemTheme.border, StemCardShape)
                        .clickable { expandedId = if (isExpanded) null else item.id }
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.presetName.uppercase(),
                                style = StemMonoBadge,
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
                        } else {
                            BeforeAfterDiffBlock(
                                beforeText = item.originalText,
                                afterText = item.replacedText
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(StemSharpShape)
                                        .background(stemTheme.surface2)
                                        .border(1.dp, stemTheme.border, StemSharpShape)
                                        .clickable(
                                            role = Role.Button,
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Stem", item.replacedText))
                                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Copy",
                                        style = StemMonoBadge,
                                        color = stemTheme.ink
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

private fun formatTimeAgo(timestamp: Long): String {
    val deltaMs = System.currentTimeMillis() - timestamp
    val deltaMins = deltaMs / (1000 * 60)
    return when {
        deltaMins < 1 -> "just now"
        deltaMins < 60 -> "m ago"
        deltaMins < 1440 -> "h ago"
        else -> "d ago"
    }
}
