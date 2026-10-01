package com.kanwarai.voiceprofessionalmessage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kanwarai.voiceprofessionalmessage.ui.components.SegmentedSelector
import com.kanwarai.voiceprofessionalmessage.ui.components.SettingsSection
import com.kanwarai.voiceprofessionalmessage.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    ShellScreen(title = "Settings", onBack = onBack) {
        SettingsSection(title = "Appearance", icon = Icons.Outlined.Palette) {
            Text(
                text = "Choose how the app looks for this session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedSelector(
                title = "Theme",
                options = ThemeMode.entries,
                selectedOption = themeMode,
                optionLabel = ThemeMode::label,
                onOptionSelected = onThemeModeSelected,
            )
            Text(
                text = "Theme selection is not saved after the app is fully closed yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsSection(title = "AI models", icon = Icons.Outlined.AutoAwesome) {
            ModelStatusRow(
                icon = Icons.Outlined.Mic,
                title = "Speech recognition",
                availability = "Checked when used",
                status = "Local tiny.en · manual development install",
            )
            ModelStatusRow(
                icon = Icons.Outlined.AutoAwesome,
                title = "Message rewriting",
                availability = "Not installed",
                status = "Coming in Phase 5",
            )
            Text(
                text = "Automatic model installation will be added later. Phase 4 verifies a manually supplied private speech model when transcription starts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsSection(title = "Privacy", icon = Icons.Outlined.Lock) {
            Text(
                text = "Voice notes and generated messages are designed to stay on your device.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Voice notes are recorded only on request, transcribed locally, then deleted. Transcripts stay in memory and are never sent to a service.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModelStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    availability: String,
    status: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = availability,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier.padding(top = 8.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
