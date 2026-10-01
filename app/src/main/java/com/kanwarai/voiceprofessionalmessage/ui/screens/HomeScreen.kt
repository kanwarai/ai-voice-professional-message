package com.kanwarai.voiceprofessionalmessage.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kanwarai.voiceprofessionalmessage.VoiceMessageApplication
import com.kanwarai.voiceprofessionalmessage.presentation.HomeViewModel
import com.kanwarai.voiceprofessionalmessage.presentation.MessageTone
import com.kanwarai.voiceprofessionalmessage.presentation.MessageType
import com.kanwarai.voiceprofessionalmessage.presentation.RecordingState
import com.kanwarai.voiceprofessionalmessage.presentation.formatRecordingDuration
import com.kanwarai.voiceprofessionalmessage.ui.components.PrimaryVoiceAction
import com.kanwarai.voiceprofessionalmessage.ui.components.SegmentedSelector

@Composable
fun HomeScreen(
    onOpenResult: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val application = context.applicationContext as VoiceMessageApplication
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(application.audioRecorder, application),
    )
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val canAskAgain = granted || ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.RECORD_AUDIO,
        )
        viewModel.onPermissionResult(granted, canAskAgain)
    }

    fun requestRecording() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startRecording()
        } else {
            viewModel.onPermissionRequestStarted()
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.onPermissionAvailable()
            }
            if (event == Lifecycle.Event.ON_STOP && !activity.isChangingConfigurations) {
                viewModel.onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!activity.isChangingConfigurations) {
                viewModel.onWorkflowLeft()
            }
        }
    }

    LaunchedEffect(uiState.notice) {
        val notice = uiState.notice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(notice)
        viewModel.onNoticeShown()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            HomeNavigationBar(
                enabled = uiState.configurationEnabled,
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
                            recordingState = uiState.recordingState,
                            onRecord = ::requestRecording,
                            onStop = viewModel::stopRecording,
                            onCancel = { viewModel.cancelRecording() },
                            onCancelTranscription = { viewModel.cancelTranscription() },
                            onDeleteTranscript = viewModel::discardTranscript,
                            onTranscriptChanged = viewModel::updateTranscript,
                            onDismissError = viewModel::dismissError,
                            onOpenSettings = { context.openAppSettings() },
                            modifier = Modifier.weight(1f),
                        )
                        ConfigurationPanel(
                            selectedType = uiState.messageType,
                            selectedTone = uiState.tone,
                            enabled = uiState.configurationEnabled,
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
                        BrandAndRecord(
                            recordingState = uiState.recordingState,
                            onRecord = ::requestRecording,
                            onStop = viewModel::stopRecording,
                            onCancel = { viewModel.cancelRecording() },
                            onCancelTranscription = { viewModel.cancelTranscription() },
                            onDeleteTranscript = viewModel::discardTranscript,
                            onTranscriptChanged = viewModel::updateTranscript,
                            onDismissError = viewModel::dismissError,
                            onOpenSettings = { context.openAppSettings() },
                        )
                        ConfigurationPanel(
                            selectedType = uiState.messageType,
                            selectedTone = uiState.tone,
                            enabled = uiState.configurationEnabled,
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
    recordingState: RecordingState,
    onRecord: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onCancelTranscription: () -> Unit,
    onDeleteTranscript: () -> Unit,
    onTranscriptChanged: (String) -> Unit,
    onDismissError: () -> Unit,
    onOpenSettings: () -> Unit,
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
        RecordingControl(
            recordingState = recordingState,
            onRecord = onRecord,
            onStop = onStop,
            onCancel = onCancel,
            onCancelTranscription = onCancelTranscription,
            onDeleteTranscript = onDeleteTranscript,
            onTranscriptChanged = onTranscriptChanged,
            onDismissError = onDismissError,
            onOpenSettings = onOpenSettings,
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
private fun RecordingControl(
    recordingState: RecordingState,
    onRecord: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onCancelTranscription: () -> Unit,
    onDeleteTranscript: () -> Unit,
    onTranscriptChanged: (String) -> Unit,
    onDismissError: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (recordingState) {
            RecordingState.Idle -> PrimaryVoiceAction(onClick = onRecord)
            RecordingState.RequestingPermission -> ProgressState("Waiting for microphone permission…")
            RecordingState.Starting -> ProgressState("Preparing microphone…")
            is RecordingState.Recording -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "●", color = Color(0xFFB3261E))
                    Text(
                        text = "Recording",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = formatRecordingDuration(recordingState.elapsedMillis),
                    modifier = Modifier.semantics {
                        contentDescription = "Elapsed recording time ${formatRecordingDuration(recordingState.elapsedMillis)}"
                    },
                    style = MaterialTheme.typography.displaySmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onStop,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Stop")
                    }
                }
            }
            is RecordingState.Stopping -> ProgressState(
                "Finalizing ${formatRecordingDuration(recordingState.elapsedMillis)} voice note…",
            )
            is RecordingState.Transcribing -> {
                ProgressState("Transcribing voice note…")
                Text(
                    text = "Audio stays on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onCancelTranscription) { Text("Cancel transcription") }
            }
            is RecordingState.Transcript -> {
                Text(
                    text = "Transcript",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = recordingState.text,
                    onValueChange = onTranscriptChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Transcript text") },
                    minLines = 4,
                )
                Text(
                    text = "Message rewriting will be added in Phase 5.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDeleteTranscript,
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete") }
                    Button(onClick = onRecord, modifier = Modifier.weight(1f)) {
                        Text("Record again")
                    }
                }
            }
            is RecordingState.TranscriptionFailed -> {
                Icon(Icons.Outlined.Warning, contentDescription = null)
                Text(
                    text = if (recordingState.error == com.kanwarai.voiceprofessionalmessage.ai.speech.TranscriptionError.ModelMissing) {
                        "Speech model not installed"
                    } else {
                        "Transcription problem"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = recordingState.error.userMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDeleteTranscript) { Text("Dismiss") }
                    Button(onClick = onRecord) { Text("Record again") }
                }
            }
            is RecordingState.PermissionDenied -> {
                Icon(Icons.Outlined.Warning, contentDescription = null)
                Text(
                    text = "Microphone permission needed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (recordingState.permanently) {
                        "Allow microphone access in Android settings to record a voice note."
                    } else {
                        "Microphone access is required only while you record. You can try again."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = if (recordingState.permanently) onOpenSettings else onRecord) {
                    Text(if (recordingState.permanently) "Open app settings" else "Try again")
                }
                TextButton(onClick = onDismissError) { Text("Not now") }
            }
            is RecordingState.Error -> {
                Icon(Icons.Outlined.Warning, contentDescription = null)
                Text(
                    text = "Recording problem",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = recordingState.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onDismissError) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun ProgressState(message: String) {
    CircularProgressIndicator()
    Text(
        text = message,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ConfigurationPanel(
    selectedType: MessageType,
    selectedTone: MessageTone,
    enabled: Boolean,
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
            enabled = enabled,
        )
        SegmentedSelector(
            title = "Tone",
            options = MessageTone.entries,
            selectedOption = selectedTone,
            optionLabel = MessageTone::label,
            onOptionSelected = onToneSelected,
            enabled = enabled,
        )
        TextButton(
            onClick = onOpenResult,
            enabled = enabled,
            modifier = Modifier.align(Alignment.End),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Article, contentDescription = null)
            Text("Open message area", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun HomeNavigationBar(
    enabled: Boolean,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = {},
            enabled = enabled,
            icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenHistory,
            enabled = enabled,
            icon = { Icon(Icons.Outlined.History, contentDescription = null) },
            label = { Text("History") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenSettings,
            enabled = enabled,
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text("Settings") },
        )
    }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Home screen requires an Activity context")
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ),
    )
}
