package com.kanwarai.voiceprofessionalmessage.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.runtime.Composable
import com.kanwarai.voiceprofessionalmessage.ui.components.EmptyState

@Composable
fun ResultScreen(onBack: () -> Unit) {
    ShellScreen(title = "Your message", onBack = onBack) {
        EmptyState(
            title = "No generated message yet.",
            supportingText = "Record a voice note to create a polished message.",
            icon = Icons.AutoMirrored.Outlined.Article,
        )
    }
}
