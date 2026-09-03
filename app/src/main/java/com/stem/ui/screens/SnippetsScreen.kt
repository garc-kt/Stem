package com.stem.ui.screens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stem.R
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemCardShape
import com.stem.ui.theme.StemMonoBadge
import com.stem.ui.theme.StemSharpShape



/**
 * Stem Snippets & Commands Screen:
 * - Inline Commands list (?fix, ?concise, ?formal, ?punchy, etc.)
 * - Snippets Auto-expansions list (email -> address, shrug, etc.)
 * - New Snippet & Custom Command creators
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

    var snippetTriggerInput by remember { mutableStateOf("") }
    var snippetExpansionInput by remember { mutableStateOf("") }

    var commandTriggerInput by remember { mutableStateOf("") }
    var commandPromptInput by remember { mutableStateOf("") }

    val builtInCommands = listOf(
        Pair("?fix", stringResource(R.string.snippets_builtin_fix)),
        Pair("?concise", stringResource(R.string.snippets_builtin_concise)),
        Pair("?formal", stringResource(R.string.snippets_builtin_formal)),
        Pair("?punchy", stringResource(R.string.snippets_builtin_punchy)),
        Pair("?friendly", stringResource(R.string.snippets_builtin_friendly)),
        Pair("?bullets", stringResource(R.string.snippets_builtin_bullets)),
        Pair("?summarize", stringResource(R.string.snippets_builtin_summarize)),
        Pair("?expand", stringResource(R.string.snippets_builtin_expand)),
        Pair("?ai: <prompt>", stringResource(R.string.snippets_builtin_ai)),
        Pair("?calc: <math>", stringResource(R.string.snippets_builtin_calc)),
        Pair("?now / ?date", stringResource(R.string.snippets_builtin_now_date)),
        Pair("?undo", stringResource(R.string.snippets_builtin_undo)),
        Pair("..cmd:key:prompt", stringResource(R.string.snippets_builtin_cmd)),
        Pair("..save:key:text", stringResource(R.string.snippets_builtin_save))
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(stemTheme.bg)
    ) {
        // -------------------------------------------------------------
        // SECTION 1: INLINE COMMANDS
        // -------------------------------------------------------------
        item {
            Column {
                Text(
                    text = stringResource(R.string.snippets_inline_commands_header),
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.snippets_inline_commands_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = stemTheme.inkMuted
                )
            }
        }

        // Built-in Inline Commands List
        items(builtInCommands) { (cmd, description) ->
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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(StemSharpShape)
                                .background(stemTheme.surface2)
                                .border(1.dp, stemTheme.border, StemSharpShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = cmd,
                                style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                                color = stemTheme.ink
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = stemTheme.inkMuted
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 2: CUSTOM COMMANDS
        // -------------------------------------------------------------
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.snippets_custom_commands_header, customCommands.size),
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.snippets_custom_commands_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = stemTheme.inkMuted
                )
            }
        }

        // Add New Custom Command Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StemCardShape)
                    .background(stemTheme.surface)
                    .border(1.dp, stemTheme.border, StemCardShape)
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.snippets_add_custom_command_header),
                        style = StemMonoBadge,
                        color = stemTheme.inkFaint
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = commandTriggerInput,
                        onValueChange = { commandTriggerInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.snippets_command_trigger_placeholder), style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
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
                        value = commandPromptInput,
                        onValueChange = { commandPromptInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.snippets_command_prompt_placeholder), style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    val canSaveCommand = commandTriggerInput.isNotBlank() && commandPromptInput.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(StemSharpShape)
                            .background(if (canSaveCommand) stemTheme.ink else stemTheme.surface2)
                            .clickable(
                                enabled = canSaveCommand,
                                role = Role.Button,
                                onClick = {
                                    val cleanKey = commandTriggerInput.trim().removePrefix("?").removePrefix("..").removePrefix(".")
                                    val cleanPrompt = commandPromptInput.trim()
                                    if (cleanKey.isNotBlank() && cleanPrompt.isNotBlank()) {
                                        onSaveCustomCommand(cleanKey, cleanPrompt)
                                        commandTriggerInput = ""
                                        commandPromptInput = ""
                                    }
                                }
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.snippets_save_command_button),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (canSaveCommand) stemTheme.onInk else stemTheme.inkFaint
                        )
                    }
                }
            }
        }

        // Custom User Commands List
        items(customCommands.entries.toList(), key = { it.key }) { (trigger, prompt) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StemCardShape)
                    .background(stemTheme.surface)
                    .border(1.dp, stemTheme.border, StemCardShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(StemSharpShape)
                                .background(stemTheme.surface2)
                                .border(1.dp, stemTheme.border, StemSharpShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "?$trigger",
                                style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                                color = stemTheme.ink
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = stemTheme.ink
                        )
                    }

                    IconButton(
                        onClick = { onDeleteCustomCommand(trigger) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.snippets_delete_command_description),
                            tint = stemTheme.inkMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 3: SNIPPET EXPANSIONS
        // -------------------------------------------------------------
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.snippets_expansions_header, snippets.size),
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.snippets_expansions_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = stemTheme.inkMuted
                )
            }
        }

        // Add New Snippet Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StemCardShape)
                    .background(stemTheme.surface)
                    .border(1.dp, stemTheme.border, StemCardShape)
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.snippets_add_snippet_header),
                        style = StemMonoBadge,
                        color = stemTheme.inkFaint
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = snippetTriggerInput,
                        onValueChange = { snippetTriggerInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.snippets_trigger_placeholder), style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
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
                        value = snippetExpansionInput,
                        onValueChange = { snippetExpansionInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.snippets_expansion_placeholder), style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    val canSaveSnippet = snippetTriggerInput.isNotBlank() && snippetExpansionInput.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(StemSharpShape)
                            .background(if (canSaveSnippet) stemTheme.ink else stemTheme.surface2)
                            .clickable(
                                enabled = canSaveSnippet,
                                role = Role.Button,
                                onClick = {
                                    val cleanedTrigger = snippetTriggerInput.trim().removePrefix("..").removePrefix(".")
                                    val cleanedExpansion = snippetExpansionInput.trim()
                                    if (cleanedTrigger.isNotBlank() && cleanedExpansion.isNotBlank()) {
                                        onSaveSnippet(cleanedTrigger, cleanedExpansion)
                                        snippetTriggerInput = ""
                                        snippetExpansionInput = ""
                                    }
                                }
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.snippets_save_snippet_button),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (canSaveSnippet) stemTheme.onInk else stemTheme.inkFaint
                        )
                    }
                }
            }
        }

        // Active Snippets List
        items(snippets.entries.toList(), key = { it.key }) { (trigger, expansion) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StemCardShape)
                    .background(stemTheme.surface)
                    .border(1.dp, stemTheme.border, StemCardShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(StemSharpShape)
                                .background(stemTheme.surface2)
                                .border(1.dp, stemTheme.border, StemSharpShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "..$trigger",
                                style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                                color = stemTheme.ink
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = expansion,
                            style = MaterialTheme.typography.bodySmall,
                            color = stemTheme.ink
                        )
                    }

                    IconButton(
                        onClick = { onDeleteSnippet(trigger) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.snippets_delete_snippet_description),
                            tint = stemTheme.inkMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

