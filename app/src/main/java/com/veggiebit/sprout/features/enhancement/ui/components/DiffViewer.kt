package com.veggiebit.sprout.features.enhancement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veggiebit.sprout.app.theme.LocalSproutExtendedColors
import com.veggiebit.sprout.features.enhancement.data.models.DiffToken
import com.veggiebit.sprout.features.enhancement.data.models.DiffType

/**
 * High-craft Visual Diff Viewer rendering inline additions and deletions with M3 tokens.
 */
@Composable
fun DiffViewer(
    diffTokens: List<DiffToken>,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalSproutExtendedColors.current

    val annotatedString = buildAnnotatedString {
        diffTokens.forEach { token ->
            when (token.type) {
                DiffType.UNMODIFIED -> {
                    append(token.text)
                }
                DiffType.ADDED -> {
                    pushStyle(
                        SpanStyle(
                            color = extendedColors.diffAdded,
                            background = extendedColors.diffAddedBackground,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    append(token.text)
                    pop()
                }
                DiffType.DELETED -> {
                    pushStyle(
                        SpanStyle(
                            color = extendedColors.diffDeleted,
                            background = extendedColors.diffDeletedBackground,
                            textDecoration = TextDecoration.LineThrough,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    append(token.text)
                    pop()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 24.sp,
                fontSize = 14.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
