package com.veggiebit.sprout.features.settings.ui.sections

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veggiebit.sprout.app.theme.LocalStemColors
import com.veggiebit.sprout.app.theme.StemCardShape
import com.veggiebit.sprout.app.theme.StemMonoBadge
import com.veggiebit.sprout.app.theme.StemSharpShape

/**
 * Stem Snippets Screen:
 * - Trigger / Expansion addition card
 * - Active Snippets list with trigger badges and quick deletion
 * Matches Stem.dc.html design specification.
 */
@Composable
fun SnippetsScreen(
    snippets: Map<String, String>,
    customCommands: Map<String, String> = emptyMap(),
    onSaveSnippet: (String, String) -> Unit,
    onDeleteSnippet: (String) -> Unit,
    onSaveCustomCommand: (String, String) -> Unit = { _, _ -> },
    onDeleteCustomCommand: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current

    var triggerInput by remember { mutableStateOf("") }
    var expansionInput by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
            .fillMaxSize()
            .background(stemTheme.bg)
    ) {
        // Subtitle / Intro
        item {
            Text(
                text = "Expand short abbreviations into full phrases as you type in any app.",
                style = MaterialTheme.typography.bodyMedium,
                color = stemTheme.inkMuted
            )
        }

        // New Snippet Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StemCardShape)
                    .background(stemTheme.surface)
                    .border(1.dp, stemTheme.border, StemCardShape)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "NEW SNIPPET",
                        style = StemMonoBadge,
                        color = stemTheme.inkFaint
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = triggerInput,
                        onValueChange = { triggerInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Trigger (e.g. ;email)", style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = stemTheme.ink),
                        shape = StemSharpShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = stemTheme.bg,
                            unfocusedContainerColor = stemTheme.bg,
                            focusedBorderColor = stemTheme.ink,
                            unfocusedBorderColor = stemTheme.border,
                            cursorColor = stemTheme.ink
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = expansionInput,
                        onValueChange = { expansionInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Expansion", style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = stemTheme.ink),
                        shape = StemSharpShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = stemTheme.bg,
                            unfocusedContainerColor = stemTheme.bg,
                            focusedBorderColor = stemTheme.ink,
                            unfocusedBorderColor = stemTheme.border,
                            cursorColor = stemTheme.ink
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(StemSharpShape)
                            .background(if (triggerInput.isNotBlank() && expansionInput.isNotBlank()) stemTheme.ink else stemTheme.surface2)
                            .clickable(
                                enabled = triggerInput.isNotBlank() && expansionInput.isNotBlank(),
                                role = Role.Button,
                                onClick = {
                                    val cleanedTrigger = triggerInput.trim()
                                    val cleanedExpansion = expansionInput.trim()
                                    if (cleanedTrigger.isNotBlank() && cleanedExpansion.isNotBlank()) {
                                        onSaveSnippet(cleanedTrigger, cleanedExpansion)
                                        triggerInput = ""
                                        expansionInput = ""
                                    }
                                }
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Add snippet",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (triggerInput.isNotBlank() && expansionInput.isNotBlank()) stemTheme.onInk else stemTheme.inkFaint
                        )
                    }
                }
            }
        }

        // Snippets Header
        item {
            Text(
                text = "SNIPPETS ()",
                style = StemMonoBadge,
                color = stemTheme.inkFaint
            )
        }

        if (snippets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(StemCardShape)
                        .background(stemTheme.surface)
                        .border(1.dp, stemTheme.border, StemCardShape)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "No snippets added yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = stemTheme.inkMuted
                    )
                }
            }
        } else {
            items(snippets.entries.toList(), key = { it.key }) { (trigger, expansion) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(StemCardShape)
                        .background(stemTheme.surface)
                        .border(1.dp, stemTheme.border, StemCardShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(StemSharpShape)
                                    .background(stemTheme.surface2)
                                    .border(1.dp, stemTheme.border, StemSharpShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = trigger,
                                    style = StemMonoBadge,
                                    color = stemTheme.ink
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = expansion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = stemTheme.ink,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        IconButton(
                            onClick = { onDeleteSnippet(trigger) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Delete snippet",
                                tint = stemTheme.inkMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
