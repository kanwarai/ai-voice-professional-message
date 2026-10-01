package com.kanwarai.voiceprofessionalmessage.presentation

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorder
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorderError
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorderEvent
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorderResult
import com.kanwarai.voiceprofessionalmessage.audio.MAX_RECORDING_DURATION_MILLIS
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class HomeUiState(
    val messageType: MessageType = MessageType.Message,
    val tone: MessageTone = MessageTone.Professional,
    val recordingState: RecordingState = RecordingState.Idle,
    val notice: String? = null,
) {
    val configurationEnabled: Boolean
        get() = recordingState is RecordingState.Idle ||
            recordingState is RecordingState.Ready ||
            recordingState is RecordingState.PermissionDenied ||
            recordingState is RecordingState.Error
}

sealed interface RecordingState {
    data object Idle : RecordingState
    data object RequestingPermission : RecordingState
    data object Starting : RecordingState
    data class Recording(val elapsedMillis: Long) : RecordingState
    data class Stopping(val elapsedMillis: Long) : RecordingState
    data class Ready(val durationMillis: Long, val byteCount: Long) : RecordingState
    data class PermissionDenied(val permanently: Boolean) : RecordingState
    data class Error(val message: String) : RecordingState
}

enum class MessageType(val label: String) {
    Message("Message"),
    Email("Email"),
}

enum class MessageTone(val label: String) {
    Professional("Professional"),
    Friendly("Friendly"),
    Concise("Concise"),
}

fun interface MonotonicClock {
    fun nowMillis(): Long
}

fun formatRecordingDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis.coerceAtLeast(0) / 1_000).coerceAtMost(99 * 60 + 59)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

class HomeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val audioRecorder: AudioRecorder,
    private val clock: MonotonicClock = MonotonicClock(SystemClock::elapsedRealtime),
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        HomeUiState(
            messageType = restoredEnum(MESSAGE_TYPE_KEY, MessageType.Message),
            tone = restoredEnum(TONE_KEY, MessageTone.Professional),
        ),
    )
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    private var recordingStartedAtMillis = 0L
    private var timerJob: Job? = null
    private var operationJob: Job? = null

    init {
        viewModelScope.launch {
            audioRecorder.events.collect { event ->
                if (event is AudioRecorderEvent.CaptureFailed &&
                    mutableState.value.recordingState is RecordingState.Recording
                ) {
                    timerJob?.cancel()
                    audioRecorder.cancel()
                    showError(event.error)
                }
            }
        }
    }

    fun selectMessageType(messageType: MessageType) {
        if (!mutableState.value.configurationEnabled) return
        mutableState.value = mutableState.value.copy(messageType = messageType)
        savedStateHandle[MESSAGE_TYPE_KEY] = messageType.name
    }

    fun selectTone(tone: MessageTone) {
        if (!mutableState.value.configurationEnabled) return
        mutableState.value = mutableState.value.copy(tone = tone)
        savedStateHandle[TONE_KEY] = tone.name
    }

    fun onPermissionRequestStarted() {
        if (mutableState.value.recordingState is RecordingState.Recording) return
        if (mutableState.value.recordingState is RecordingState.Ready) {
            viewModelScope.launch { audioRecorder.discardCompleted() }
        }
        savedStateHandle[PERMISSION_REQUESTED_KEY] = true
        mutableState.value = mutableState.value.copy(
            recordingState = RecordingState.RequestingPermission,
            notice = null,
        )
    }

    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        if (granted) {
            startRecording()
        } else {
            mutableState.value = mutableState.value.copy(
                recordingState = RecordingState.PermissionDenied(permanently = !canAskAgain),
            )
        }
    }

    fun startRecording() {
        if (operationJob?.isActive == true) return
        if (mutableState.value.recordingState is RecordingState.Recording) return
        mutableState.value = mutableState.value.copy(
            recordingState = RecordingState.Starting,
            notice = null,
        )
        operationJob = viewModelScope.launch {
            when (val result = audioRecorder.start()) {
                is AudioRecorderResult.Success -> {
                    recordingStartedAtMillis = clock.nowMillis()
                    mutableState.value = mutableState.value.copy(
                        recordingState = RecordingState.Recording(elapsedMillis = 0),
                    )
                    startTimer()
                }
                is AudioRecorderResult.Failure -> showError(result.error)
            }
        }
    }

    fun stopRecording() {
        val recording = mutableState.value.recordingState as? RecordingState.Recording ?: return
        stopRecordingInternal(recording.elapsedMillis, maximumReached = false)
    }

    fun cancelRecording(message: String? = null) {
        val currentState = mutableState.value.recordingState
        if (currentState !is RecordingState.Recording &&
            currentState !is RecordingState.Stopping &&
            currentState !is RecordingState.Starting
        ) return
        timerJob?.cancel()
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            val result = audioRecorder.cancel()
            mutableState.value = if (result is AudioRecorderResult.Success) {
                mutableState.value.copy(
                    recordingState = RecordingState.Idle,
                    notice = message,
                )
            } else {
                mutableState.value.copy(
                    recordingState = RecordingState.Error(AudioRecorderError.CleanupFailed.userMessage),
                )
            }
        }
    }

    fun discardRecording() {
        if (operationJob?.isActive == true) return
        operationJob = viewModelScope.launch {
            when (val result = audioRecorder.discardCompleted()) {
                is AudioRecorderResult.Success -> {
                    mutableState.value = mutableState.value.copy(recordingState = RecordingState.Idle)
                }
                is AudioRecorderResult.Failure -> showError(result.error)
            }
        }
    }

    fun onAppBackgrounded() {
        if (mutableState.value.recordingState is RecordingState.Recording ||
            mutableState.value.recordingState is RecordingState.Starting
        ) {
            cancelRecording("Recording was cancelled when the app moved to the background.")
        }
    }

    fun onWorkflowLeft() {
        when (mutableState.value.recordingState) {
            is RecordingState.Recording,
            is RecordingState.Starting,
            is RecordingState.Stopping,
            -> cancelRecording()
            is RecordingState.Ready -> discardRecording()
            else -> Unit
        }
    }

    fun dismissError() {
        if (mutableState.value.recordingState is RecordingState.Error ||
            mutableState.value.recordingState is RecordingState.PermissionDenied
        ) {
            mutableState.value = mutableState.value.copy(recordingState = RecordingState.Idle)
        }
    }

    fun onPermissionAvailable() {
        if (mutableState.value.recordingState is RecordingState.PermissionDenied) {
            mutableState.value = mutableState.value.copy(recordingState = RecordingState.Idle)
        }
    }

    fun onNoticeShown() {
        mutableState.value = mutableState.value.copy(notice = null)
    }

    override fun onCleared() {
        timerJob?.cancel()
        operationJob?.cancel()
        runBlocking {
            audioRecorder.cancel()
            audioRecorder.discardCompleted()
        }
        super.onCleared()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = (clock.nowMillis() - recordingStartedAtMillis)
                    .coerceIn(0, MAX_RECORDING_DURATION_MILLIS)
                mutableState.value = mutableState.value.copy(
                    recordingState = RecordingState.Recording(elapsed),
                )
                if (elapsed >= MAX_RECORDING_DURATION_MILLIS) {
                    viewModelScope.launch {
                        stopRecordingInternal(elapsed, maximumReached = true)
                    }
                    return@launch
                }
                delay(TIMER_UPDATE_MILLIS)
            }
        }
    }

    private fun stopRecordingInternal(elapsedMillis: Long, maximumReached: Boolean) {
        if (operationJob?.isActive == true) return
        timerJob?.cancel()
        mutableState.value = mutableState.value.copy(
            recordingState = RecordingState.Stopping(elapsedMillis),
        )
        operationJob = viewModelScope.launch {
            when (val result = audioRecorder.stop()) {
                is AudioRecorderResult.Success -> {
                    mutableState.value = mutableState.value.copy(
                        recordingState = RecordingState.Ready(
                            durationMillis = result.value.durationMillis,
                            byteCount = result.value.byteCount,
                        ),
                        notice = if (maximumReached) {
                            "The two-minute limit was reached. Your voice note was stopped safely."
                        } else {
                            null
                        },
                    )
                }
                is AudioRecorderResult.Failure -> showError(result.error)
            }
        }
    }

    private fun showError(error: AudioRecorderError) {
        mutableState.value = mutableState.value.copy(
            recordingState = RecordingState.Error(error.userMessage),
        )
    }

    private inline fun <reified T : Enum<T>> restoredEnum(key: String, default: T): T {
        val savedValue = savedStateHandle.get<String>(key) ?: return default
        return enumValues<T>().find { it.name == savedValue } ?: default
    }

    companion object {
        private const val MESSAGE_TYPE_KEY = "message_type"
        private const val TONE_KEY = "tone"
        private const val PERMISSION_REQUESTED_KEY = "microphone_permission_requested"
        private const val TIMER_UPDATE_MILLIS = 250L

        fun factory(audioRecorder: AudioRecorder) = viewModelFactory {
            initializer {
                HomeViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    audioRecorder = audioRecorder,
                )
            }
        }
    }
}
