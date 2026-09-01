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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        Pair("?fix", "Fix grammar, spelling, and punctuation"),
        Pair("?concise", "Trim wordiness and remove fluff"),
        Pair("?formal", "Elevate to clear professional tone"),
        Pair("?punchy", "Direct, impactful, and active"),
        Pair("?friendly", "Warm, open, and approachable"),
        Pair("?bullets", "Convert paragraphs to clear bullet points"),
        Pair("?summarize", "Condense into key takeaways"),
        Pair("?expand", "Elaborate with details and depth"),
        Pair("?ai: <prompt>", "Run ad-hoc AI instruction (e.g. ?ai: translate to spanish)"),
        Pair("?calc: <math>", "Calculate math inline (e.g. ?calc: 25 * 4 + 10)"),
        Pair("?now / ?date", "Insert formatted current timestamp or date"),
        Pair("?undo", "Revert last transformation in active field"),
        Pair("..cmd:key:prompt", "Quick-save a custom AI command inline while typing"),
        Pair("..save:key:text", "Quick-save a new snippet expansion inline while typing")
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
                    text = "INLINE COMMANDS",
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Type any command at the end of your text in any app to transform or calculate instantly.",
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
                    text = "CUSTOM COMMANDS (${customCommands.size})",
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Define custom AI prompt shortcuts. Type ?<trigger> in any text field to invoke.",
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
                        text = "ADD NEW CUSTOM COMMAND",
                        style = StemMonoBadge,
                        color = stemTheme.inkFaint
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = commandTriggerInput,
                        onValueChange = { commandTriggerInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Trigger (e.g. roast, translate, reply)", style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
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
                        placeholder = { Text("AI instruction (e.g. Rewrite in a witty, humorous roast tone)", style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
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
                            text = "Save custom command",
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
                            contentDescription = "Delete command",
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
                    text = "SNIPPET EXPANSIONS (${snippets.size})",
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Expand short triggers into long text phrases. Type ..<trigger> or .<trigger> to expand.",
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
                        text = "ADD NEW SNIPPET",
                        style = StemMonoBadge,
                        color = stemTheme.inkFaint
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = snippetTriggerInput,
                        onValueChange = { snippetTriggerInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Trigger (e.g. email, addr, shrug)", style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
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
                        placeholder = { Text("Full phrase expansion", style = MaterialTheme.typography.bodySmall, color = stemTheme.inkFaint) },
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
                            text = "Save snippet",
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

