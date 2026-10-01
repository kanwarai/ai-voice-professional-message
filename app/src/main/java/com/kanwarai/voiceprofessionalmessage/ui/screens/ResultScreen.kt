package com.kanwarai.voiceprofessionalmessage.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun ResultScreen(onBack: () -> Unit) {
    ShellScreen(
        title = "Result",
        emptyMessage = "No generated message yet.",
        supportingMessage = "Your polished message will appear here after generation is added in a later phase.",
        onBack = onBack,
    )
}
