package com.veggiebit.sprout.features.selection.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veggiebit.sprout.app.SproutApplication
import com.veggiebit.sprout.app.theme.LocalStemColors
import com.veggiebit.sprout.app.theme.StemIndicatorShape
import com.veggiebit.sprout.app.theme.StemMonoBadge
import com.veggiebit.sprout.app.theme.StemOverlayShape
import com.veggiebit.sprout.app.theme.StemSharpShape
import com.veggiebit.sprout.app.theme.SproutTheme
import com.veggiebit.sprout.features.enhancement.data.engine.TextEngineProvider
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import com.veggiebit.sprout.features.enhancement.ui.components.BeforeAfterDiffBlock
import com.veggiebit.sprout.features.enhancement.ui.components.DiffViewer
import com.veggiebit.sprout.features.enhancement.ui.components.PresetChipsRow
import com.veggiebit.sprout.features.settings.data.SproutUserSettings

class ProcessTextActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
        val isReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

        if (selectedText.isBlank()) {
            finish()
            return
        }

        setContent {
            val userSettings by SproutApplication.instance.preferencesRepository.settingsFlow
                .collectAsState(initial = SproutUserSettings())

            var activePreset by remember(userSettings.defaultPreset) { mutableStateOf(userSettings.defaultPreset) }
            var result by remember { mutableStateOf<TransformResult?>(null) }
            var isLoading by remember { mutableStateOf(false) }

            LaunchedEffect(selectedText, activePreset, userSettings.engineMode) {
                isLoading = true
                val payload = TextPayload(text = selectedText)
                val engine = TextEngineProvider.getEngine(userSettings)
                result = engine.transform(payload, activePreset)
                isLoading = false
            }

            SproutTheme(themeMode = userSettings.themeMode, dynamicColor = false) {
                val stemTheme = LocalStemColors.current

                Dialog(
                    onDismissRequest = { finish() },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .shadow(24.dp, StemOverlayShape)
                            .clip(StemOverlayShape)
                            .border(1.dp, stemTheme.border, StemOverlayShape),
                        shape = StemOverlayShape,
                        color = stemTheme.surface
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Drag Handle / Indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .size(width = 36.dp, height = 3.dp)
                                    .clip(StemIndicatorShape)
                                    .background(stemTheme.border)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "STEM SELECTION",
                                        style = StemMonoBadge,
                                        color = stemTheme.inkMuted
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = userSettings.engineMode.title,
                                        style = StemMonoBadge,
                                        color = stemTheme.inkFaint
                                    )
                                }

                                IconButton(
                                    onClick = { finish() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close",
                                        tint = stemTheme.inkMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Presets Row (Compact)
                            PresetChipsRow(
                                selectedPreset = activePreset,
                                onPresetSelected = { activePreset = it },
                                compact = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Loading state / Diff View
                            if (isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(StemSharpShape)
                                        .background(stemTheme.bg)
                                        .border(1.dp, stemTheme.border, StemSharpShape)
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = stemTheme.ink
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Applying ${activePreset.title}...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = stemTheme.inkMuted
                                        )
                                    }
                                }
                            } else {
                                result?.let { res ->
                                    if (activePreset.useDiff && res.diffTokens.isNotEmpty()) {
                                        DiffViewer(diffTokens = res.diffTokens)
                                    } else {
                                        BeforeAfterDiffBlock(
                                            beforeText = res.originalText,
                                            afterText = res.transformedText
                                        )
                                    }

                                    if (res.wordsSaved > 0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "-${res.wordsSaved} words",
                                            style = StemMonoBadge,
                                            color = stemTheme.add
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Actions: Copy & Replace
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
                                            onClick = {
                                                val textToCopy = result?.transformedText ?: selectedText
                                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Stem", textToCopy))
                                                Toast.makeText(this@ProcessTextActivity, "Copied", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Copy",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = stemTheme.ink
                                    )
                                }

                                if (!isReadOnly) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(StemSharpShape)
                                            .background(if (!isLoading && result?.hasChanges == true) stemTheme.ink else stemTheme.surface2)
                                            .clickable(
                                                enabled = !isLoading && result?.hasChanges == true,
                                                role = Role.Button,
                                                onClick = {
                                                    val output = result?.transformedText ?: selectedText
                                                    val resultIntent = Intent().apply {
                                                        putExtra(Intent.EXTRA_PROCESS_TEXT, output)
                                                    }
                                                    setResult(Activity.RESULT_OK, resultIntent)
                                                    finish()
                                                }
                                            )
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Replace",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (!isLoading && result?.hasChanges == true) stemTheme.onInk else stemTheme.inkFaint
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

