package com.veggiebit.sprout.features.settings.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veggiebit.sprout.features.enhancement.data.engine.TextEngineProvider
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import com.veggiebit.sprout.features.enhancement.ui.components.DiffViewer
import com.veggiebit.sprout.features.enhancement.ui.components.PresetChipsRow
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import com.veggiebit.sprout.features.settings.ui.components.SproutSubScreen
import kotlinx.coroutines.launch

/**
 * Interactive test sandbox — now explicit-run only. It previously re-ran the active engine on
 * every keystroke and every preset change via a LaunchedEffect keyed on the sample text; with
 * a cloud engine selected, typing a sample sentence fired one billed API request per
 * character. The "Enhance" button already existed; the auto-run path is simply gone.
 */
@Composable
fun SandboxScreen(
    userSettings: SproutUserSettings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var sandboxText by remember { mutableStateOf("I has an idea for the meting tomorow. It will be realy great.") }
    var sandboxPreset by remember { mutableStateOf(TransformPreset.FIX) }
    var sandboxResult by remember { mutableStateOf<TransformResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun runTransform() {
        isLoading = true
        scope.launch {
            val engine = TextEngineProvider.getEngine(userSettings)
            sandboxResult = engine.transform(TextPayload(text = sandboxText), sandboxPreset)
            isLoading = false
        }
    }

    SproutSubScreen(title = "Test Sandbox", onBack = onBack, modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Test your active engine (${userSettings.engineMode.title}) and presets — tap Enhance to run.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = sandboxText,
                        onValueChange = { sandboxText = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("Sample Text") },
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PresetChipsRow(selectedPreset = sandboxPreset, onPresetSelected = { sandboxPreset = it })
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { runTransform() },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && sandboxText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Enhance with ${userSettings.engineMode.title}")
                    }

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        com.veggiebit.sprout.features.enhancement.ui.components.SproutThinkingCard(
                            engineTitle = userSettings.engineMode.title,
                            subtitle = "Testing ${sandboxPreset.shortName} preset..."
                        )
                    } else {
                        sandboxResult?.let { res ->
                            Spacer(modifier = Modifier.height(12.dp))
                            if (res.diffTokens.isNotEmpty()) {
                                DiffViewer(diffTokens = res.diffTokens)
                            } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = res.transformedText.ifBlank { "No changes needed." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = res.summaryNote ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (res.wordsSaved > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "-${res.wordsSaved} words",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
