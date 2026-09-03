package com.stem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.R
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemMonoBadge
import com.stem.ui.theme.StemSharpShape
import com.stem.core.models.DiffToken
import com.stem.core.models.DiffType



/**
 * Stem Visual Diff Viewer rendering inline additions and deletions.
 * Matches Stem.dc.html wordDiff styling.
 */
@Composable
fun DiffViewer(
    diffTokens: List<DiffToken>,
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current
    val addedAnnouncement = stringResource(R.string.diff_added_announcement)
    val removedAnnouncement = stringResource(R.string.diff_removed_announcement)

    val annotatedString = remember(diffTokens, stemTheme) {
        buildAnnotatedString {
            diffTokens.forEach { token ->
                when (token.type) {
                    DiffType.UNMODIFIED -> {
                        pushStyle(
                            SpanStyle(
                                color = stemTheme.ink,
                                fontWeight = FontWeight.Normal
                            )
                        )
                        append(token.text)
                        pop()
                    }
                    DiffType.ADDED -> {
                        pushStyle(
                            SpanStyle(
                                color = stemTheme.add,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        append(token.text)
                        pop()
                    }
                    DiffType.DELETED -> {
                        pushStyle(
                            SpanStyle(
                                color = stemTheme.remove,
                                textDecoration = TextDecoration.LineThrough,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        append(token.text)
                        pop()
                    }
                }
            }
        }
    }

    val accessibilitySummary = remember(diffTokens, addedAnnouncement, removedAnnouncement) {
        buildString {
            diffTokens.forEach { token ->
                when (token.type) {
                    DiffType.UNMODIFIED -> append(token.text)
                    DiffType.ADDED -> append(String.format(addedAnnouncement, token.text))
                    DiffType.DELETED -> append(String.format(removedAnnouncement, token.text))
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(StemSharpShape)
            .background(stemTheme.surface)
            .border(1.dp, stemTheme.border, StemSharpShape)
            .padding(12.dp)
            .clearAndSetSemantics { contentDescription = accessibilitySummary }
    ) {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = stemTheme.ink
        )
    }
}

/**
 * Before/After comparison block for presets that transform entire sentence structure
 * (Summarize, Bulletize, Expand) where word-level diffing is not appropriate.
 */
@Composable
fun BeforeAfterDiffBlock(
    beforeText: String,
    afterText: String,
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(StemSharpShape)
            .background(stemTheme.surface)
            .border(1.dp, stemTheme.border, StemSharpShape)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.diff_before),
                style = StemMonoBadge,
                color = stemTheme.inkFaint
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = beforeText,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = stemTheme.inkMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.diff_after),
                style = StemMonoBadge,
                color = stemTheme.inkFaint
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = afterText,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = stemTheme.ink
            )
        }
    }
}


