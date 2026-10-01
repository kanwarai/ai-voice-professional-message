package com.kanwarai.voiceprofessionalmessage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kanwarai.voiceprofessionalmessage.presentation.HomeViewModel
import com.kanwarai.voiceprofessionalmessage.presentation.MessageTone
import com.kanwarai.voiceprofessionalmessage.presentation.MessageType
import com.kanwarai.voiceprofessionalmessage.ui.components.PrimaryVoiceAction
import com.kanwarai.voiceprofessionalmessage.ui.components.SegmentedSelector

@Composable
fun HomeScreen(
    onOpenResult: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notice) {
        val notice = uiState.notice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(notice)
        viewModel.onNoticeShown()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            HomeNavigationBar(
                onOpenHistory = onOpenHistory,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 900.dp),
            ) {
                if (maxWidth >= 700.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BrandAndRecord(
                            onRecord = viewModel::onRecordSelected,
                            modifier = Modifier.weight(1f),
                        )
                        ConfigurationPanel(
                            selectedType = uiState.messageType,
                            selectedTone = uiState.tone,
                            onTypeSelected = viewModel::selectMessageType,
                            onToneSelected = viewModel::selectTone,
                            onOpenResult = onOpenResult,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                    ) {
                        BrandAndRecord(onRecord = viewModel::onRecordSelected)
                        ConfigurationPanel(
                            selectedType = uiState.messageType,
                            selectedTone = uiState.tone,
                            onTypeSelected = viewModel::selectMessageType,
                            onToneSelected = viewModel::selectTone,
                            onOpenResult = onOpenResult,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandAndRecord(
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Voice to Message",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Turn voice notes into polished messages.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryVoiceAction(
            onClick = onRecord,
            modifier = Modifier.padding(top = 20.dp),
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Your voice and messages stay on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConfigurationPanel(
    selectedType: MessageType,
    selectedTone: MessageTone,
    onTypeSelected: (MessageType) -> Unit,
    onToneSelected: (MessageTone) -> Unit,
    onOpenResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SegmentedSelector(
            title = "Message type",
            options = MessageType.entries,
            selectedOption = selectedType,
            optionLabel = MessageType::label,
            onOptionSelected = onTypeSelected,
        )
        SegmentedSelector(
            title = "Tone",
            options = MessageTone.entries,
            selectedOption = selectedTone,
            optionLabel = MessageTone::label,
            onOptionSelected = onToneSelected,
        )
        TextButton(
            onClick = onOpenResult,
            modifier = Modifier.align(Alignment.End),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Article, contentDescription = null)
            Text("Open message area", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun HomeNavigationBar(
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenHistory,
            icon = { Icon(Icons.Outlined.History, contentDescription = null) },
            label = { Text("History") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenSettings,
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text("Settings") },
        )
    }
}
