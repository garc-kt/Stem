package com.veggiebit.sprout.features.settings.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veggiebit.sprout.app.theme.ThemeMode
import com.veggiebit.sprout.features.enhancement.data.models.LanguagePreference
import com.veggiebit.sprout.features.settings.ui.components.SproutSegmentedGroup
import com.veggiebit.sprout.features.settings.ui.components.SproutSubScreen

/** Theme mode + language preference — the Part 2.1 dark-mode toggle the user asked for, and
 * the Part 3.4 offline-rule-engine language override. */
@Composable
fun AppearanceScreen(
    themeMode: ThemeMode,
    languagePreference: LanguagePreference,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectLanguagePreference: (LanguagePreference) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SproutSubScreen(title = "Appearance", onBack = onBack, modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(20.dp)) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Follows the device by default; pin light or dark if you prefer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 28.dp)
            ) {
                SproutSegmentedGroup(
                    options = ThemeMode.entries,
                    selected = themeMode,
                    onSelected = onSelectThemeMode,
                    label = { it.label },
                    modifier = Modifier.padding(16.dp)
                )
            }

            Text(
                text = "Offline Engine Language",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Which dictionary the on-device rule engine uses. AI engines detect language automatically and ignore this.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                SproutSegmentedGroup(
                    options = LanguagePreference.entries,
                    selected = languagePreference,
                    onSelected = onSelectLanguagePreference,
                    label = { it.label },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
