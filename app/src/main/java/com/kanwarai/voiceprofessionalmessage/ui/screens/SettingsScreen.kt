package com.kanwarai.voiceprofessionalmessage.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    ShellScreen(
        title = "Settings",
        emptyMessage = "Settings are coming in later phases.",
        supportingMessage = "These sections show what will be configurable.",
        onBack = onBack,
    ) {
        Column(modifier = Modifier.padding(top = 24.dp)) {
            SettingPreview("Appearance", "Theme controls arrive in Phase 2.")
            HorizontalDivider()
            SettingPreview("AI models", "Local model management arrives in Phase 8.")
            HorizontalDivider()
            SettingPreview("Privacy", "Privacy information and controls arrive in later phases.")
        }
    }
}

@Composable
private fun SettingPreview(title: String, detail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = detail,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
