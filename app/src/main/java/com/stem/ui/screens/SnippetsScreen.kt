package com.stem.ui.screens

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stem.R
import com.stem.ui.components.StemButton
import com.stem.ui.components.StemCard
import com.stem.ui.components.StemSectionHeader
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemCardShape
import com.stem.ui.theme.StemMonoBadge
import com.stem.ui.theme.StemSharpShape

/**
 * Stem Snippets & Commands Screen:
 * Clean, minimalist view featuring:
 * - Grouped Inline Commands (Text Polish, AI & Tools, Shortcuts)
 * - Custom Commands Creator & Active List
 * - Snippet Expansions Creator & Active List
 */
@Composable
fun SnippetsScreen(
    snippets: Map<String, String>,
    onSaveSnippet: (String, String) -> Unit,
    onDeleteSnippet: (String) -> Unit,
    modifier: Modifier = Modifier,
    customCommands: Map<String, String> = emptyMap(),
    onSaveCustomCommand: (String, String) -> Unit = { _, _ -> },
    onDeleteCustomCommand: (String) -> Unit = {}
) {
    val stemTheme = LocalStemColors.current

    var snippetTriggerInput by remember { mutableStateOf("") }
    var snippetExpansionInput by remember { mutableStateOf("") }

    var commandTriggerInput by remember { mutableStateOf("") }
    var commandPromptInput by remember { mutableStateOf("") }

    var pendingDeleteSnippet by remember { mutableStateOf<String?>(null) }
    var pendingDeleteCommand by remember { mutableStateOf<String?>(null) }

    val textPolishCommands = listOf(
        Pair("?fix", stringResource(R.string.snippets_builtin_fix)),
        Pair("?concise", stringResource(R.string.snippets_builtin_concise)),
        Pair("?formal", stringResource(R.string.snippets_builtin_formal)),
        Pair("?punchy", stringResource(R.string.snippets_builtin_punchy)),
        Pair("?friendly", stringResource(R.string.snippets_builtin_friendly)),
        Pair("?bullets", stringResource(R.string.snippets_builtin_bullets)),
        Pair("?summarize", stringResource(R.string.snippets_builtin_summarize)),
        Pair("?expand", stringResource(R.string.snippets_builtin_expand))
    )

    val toolsAndAiCommands = listOf(
        Pair("?ai: <prompt>", stringResource(R.string.snippets_builtin_ai)),
        Pair("?calc: <math>", stringResource(R.string.snippets_builtin_calc)),
        Pair("?now / ?date", stringResource(R.string.snippets_builtin_now_date)),
        Pair("?undo", stringResource(R.string.snippets_builtin_undo)),
        Pair("..cmd:key:prompt", stringResource(R.string.snippets_builtin_cmd)),
        Pair("..save:key:text", stringResource(R.string.snippets_builtin_save))
    )

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = stemTheme.bg,
        unfocusedContainerColor = stemTheme.bg,
        focusedBorderColor = stemTheme.ink,
        unfocusedBorderColor = stemTheme.borderSubtle,
        cursorColor = stemTheme.ink
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(stemTheme.bg)
    ) {
        // -------------------------------------------------------------
        // SECTION 1: INLINE COMMANDS (Grouped Minimalist Card)
        // -------------------------------------------------------------
        item {
            StemSectionHeader(
                title = stringResource(R.string.snippets_inline_commands_header),
                subtitle = stringResource(R.string.snippets_inline_commands_description)
            )

            Spacer(modifier = Modifier.height(10.dp))

            StemCard {
                // Text Polish Commands
                textPolishCommands.forEachIndexed { index, (cmd, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(StemSharpShape)
                                .background(stemTheme.surface2)
                                .border(1.dp, stemTheme.borderSubtle, StemSharpShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = cmd,
                                style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                                color = stemTheme.ink
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = stemTheme.inkMuted
                        )
                    }
                    if (index < textPolishCommands.size - 1) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(stemTheme.borderSubtle))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI & Utilities Commands
            StemCard {
                toolsAndAiCommands.forEachIndexed { index, (cmd, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(StemSharpShape)
                                .background(stemTheme.surface2)
                                .border(1.dp, stemTheme.borderSubtle, StemSharpShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = cmd,
                                style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                                color = stemTheme.ink
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = stemTheme.inkMuted
                        )
                    }
                    if (index < toolsAndAiCommands.size - 1) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(stemTheme.borderSubtle))
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 2: CUSTOM COMMANDS
        // -------------------------------------------------------------
        item {
            StemSectionHeader(
                title = stringResource(R.string.snippets_custom_commands_header, customCommands.size),
                subtitle = stringResource(R.string.snippets_custom_commands_description)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Add New Custom Command Card
            StemCard {
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
                    colors = textFieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = commandPromptInput,
                    onValueChange = { commandPromptInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.snippets_command_prompt_placeholder), style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = stemTheme.ink),
                    shape = StemSharpShape,
                    colors = textFieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                val canSaveCommand = commandTriggerInput.isNotBlank() && commandPromptInput.isNotBlank()
                StemButton(
                    text = stringResource(R.string.snippets_save_command_button),
                    onClick = {
                        val cleanKey = sanitizeTriggerKey(commandTriggerInput)
                        val cleanPrompt = commandPromptInput.trim()
                        if (cleanKey.isNotBlank() && cleanPrompt.isNotBlank()) {
                            onSaveCustomCommand(cleanKey, cleanPrompt)
                            commandTriggerInput = ""
                            commandPromptInput = ""
                        }
                    },
                    enabled = canSaveCommand,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Custom User Commands List
        items(customCommands.entries.toList(), key = { it.key }) { (trigger, prompt) ->
            StemCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
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
                                .border(1.dp, stemTheme.borderSubtle, StemSharpShape)
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
                        onClick = { pendingDeleteCommand = trigger },
                        modifier = Modifier.size(36.dp)
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
            StemSectionHeader(
                title = stringResource(R.string.snippets_expansions_header, snippets.size),
                subtitle = stringResource(R.string.snippets_expansions_description)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Add New Snippet Card
            StemCard {
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
                    colors = textFieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = snippetExpansionInput,
                    onValueChange = { snippetExpansionInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.snippets_expansion_placeholder), style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = stemTheme.ink),
                    shape = StemSharpShape,
                    colors = textFieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                val canSaveSnippet = snippetTriggerInput.isNotBlank() && snippetExpansionInput.isNotBlank()
                StemButton(
                    text = stringResource(R.string.snippets_save_snippet_button),
                    onClick = {
                        val cleanedTrigger = sanitizeTriggerKey(snippetTriggerInput)
                        val cleanedExpansion = snippetExpansionInput.trim()
                        if (cleanedTrigger.isNotBlank() && cleanedExpansion.isNotBlank()) {
                            onSaveSnippet(cleanedTrigger, cleanedExpansion)
                            snippetTriggerInput = ""
                            snippetExpansionInput = ""
                        }
                    },
                    enabled = canSaveSnippet,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Active Snippets List
        items(snippets.entries.toList(), key = { it.key }) { (trigger, expansion) ->
            StemCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
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
                                .border(1.dp, stemTheme.borderSubtle, StemSharpShape)
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
                        onClick = { pendingDeleteSnippet = trigger },
                        modifier = Modifier.size(36.dp)
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

    pendingDeleteCommand?.let { cmdTrigger ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCommand = null },
            title = {
                Text(
                    text = stringResource(R.string.snippets_delete_command_confirm_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = stemTheme.ink
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.snippets_delete_command_confirm_message, cmdTrigger),
                    style = MaterialTheme.typography.bodySmall,
                    color = stemTheme.inkMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomCommand(cmdTrigger)
                        pendingDeleteCommand = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                        color = stemTheme.remove
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCommand = null }) {
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

    pendingDeleteSnippet?.let { snipTrigger ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSnippet = null },
            title = {
                Text(
                    text = stringResource(R.string.snippets_delete_confirm_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = stemTheme.ink
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.snippets_delete_confirm_message, snipTrigger),
                    style = MaterialTheme.typography.bodySmall,
                    color = stemTheme.inkMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSnippet(snipTrigger)
                        pendingDeleteSnippet = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                        color = stemTheme.remove
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSnippet = null }) {
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

private fun sanitizeTriggerKey(raw: String): String =
    raw.trim().removePrefix(";;").removePrefix("..").removePrefix("?").removePrefix(".")
