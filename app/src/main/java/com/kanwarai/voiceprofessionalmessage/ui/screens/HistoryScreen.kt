package com.kanwarai.voiceprofessionalmessage.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import com.kanwarai.voiceprofessionalmessage.ui.components.EmptyState

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    ShellScreen(title = "History", onBack = onBack) {
        EmptyState(
            title = "No messages yet.",
            supportingText = "Messages you create will appear here.",
            icon = Icons.Outlined.History,
        )
    }
}
