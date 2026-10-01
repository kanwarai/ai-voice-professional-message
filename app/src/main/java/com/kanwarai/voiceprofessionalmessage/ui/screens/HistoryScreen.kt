package com.kanwarai.voiceprofessionalmessage.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    ShellScreen(
        title = "History",
        emptyMessage = "No messages yet.",
        supportingMessage = "Local message history will be added in Phase 7.",
        onBack = onBack,
    )
}
