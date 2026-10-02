package com.kanwarai.voiceprofessionalmessage.presentation

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.kanwarai.voiceprofessionalmessage.ai.speech.SpeechToTextEngine
import com.kanwarai.voiceprofessionalmessage.ai.speech.TranscriptionError
import com.kanwarai.voiceprofessionalmessage.ai.speech.TranscriptionResult
import com.kanwarai.voiceprofessionalmessage.ai.speech.WhisperSpeechToTextEngine
import com.kanwarai.voiceprofessionalmessage.ai.rewrite.QwenRewriteEngine
import com.kanwarai.voiceprofessionalmessage.ai.rewrite.RewriteEngine
import com.kanwarai.voiceprofessionalmessage.ai.rewrite.RewriteError
import com.kanwarai.voiceprofessionalmessage.ai.rewrite.RewriteMetrics
import com.kanwarai.voiceprofessionalmessage.ai.rewrite.RewriteMessageType
import com.kanwarai.voiceprofessionalmessage.ai.rewrite.RewriteRequest
import com.kanwarai.voiceprofessionalmessage.ai.rewrite.RewriteResult
import com.kanwarai.voiceprofessionalmessage.ai.rewrite.RewriteTone
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorder
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorderError
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorderEvent
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorderResult
import com.kanwarai.voiceprofessionalmessage.audio.MAX_RECORDING_DURATION_MILLIS
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class HomeUiState(
    val messageType: MessageType = MessageType.Message,
    val tone: MessageTone = MessageTone.Professional,
    val recordingState: RecordingState = RecordingState.Idle,
    val notice: String? = null,
) {
    val configurationEnabled: Boolean
        get() = recordingState is RecordingState.Idle ||
            recordingState is RecordingState.Transcript ||
            recordingState is RecordingState.Rewritten ||
            recordingState is RecordingState.RewriteFailed ||
            recordingState is RecordingState.TranscriptionFailed ||
            recordingState is RecordingState.PermissionDenied ||
            recordingState is RecordingState.Error
}

sealed interface RecordingState {
    data object Idle : RecordingState
    data object RequestingPermission : RecordingState
    data object Starting : RecordingState
    data class Recording(val elapsedMillis: Long) : RecordingState
    data class Stopping(val elapsedMillis: Long) : RecordingState
    data class Transcribing(val durationMillis: Long) : RecordingState
    data class Transcript(
        val text: String,
        val audioDurationMillis: Long,
        val modelLoadMillis: Long,
        val transcriptionMillis: Long,
    ) : RecordingState
    data class Rewriting(val transcript: Transcript, val previous: Rewritten? = null) : RecordingState
    data class Rewritten(
        val transcript: Transcript,
        val message: String,
        val generatedMessage: String,
        val metrics: RewriteMetrics,
        val regenerateConfirmationRequired: Boolean = false,
    ) : RecordingState {
        val isEdited: Boolean get() = message != generatedMessage
    }
    data class RewriteFailed(val transcript: Transcript, val error: RewriteError) : RecordingState
    data class TranscriptionFailed(val error: TranscriptionError) : RecordingState
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
    private val speechToTextEngine: SpeechToTextEngine,
    private val rewriteEngine: RewriteEngine,
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
    private val inferenceMutex = Mutex()

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
        rewriteEngine.release()
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

    fun cancelTranscription(message: String? = null) {
        if (mutableState.value.recordingState !is RecordingState.Transcribing) return
        speechToTextEngine.cancel()
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            audioRecorder.discardCompleted()
            mutableState.value = mutableState.value.copy(
                recordingState = RecordingState.Idle,
                notice = message,
            )
        }
    }

    fun discardTranscript() {
        if (mutableState.value.recordingState is RecordingState.Transcript ||
            mutableState.value.recordingState is RecordingState.TranscriptionFailed ||
            mutableState.value.recordingState is RecordingState.Rewritten ||
            mutableState.value.recordingState is RecordingState.RewriteFailed
        ) {
            rewriteEngine.release()
            mutableState.value = mutableState.value.copy(recordingState = RecordingState.Idle)
        }
    }

    fun rewriteMessage() {
        if (operationJob?.isActive == true) return
        val currentState = mutableState.value.recordingState
        val transcript = when (val current = currentState) {
            is RecordingState.Transcript -> current
            is RecordingState.Rewritten -> current.transcript
            is RecordingState.RewriteFailed -> current.transcript
            else -> return
        }
        val previous = currentState as? RecordingState.Rewritten
        mutableState.value = mutableState.value.copy(
            recordingState = RecordingState.Rewriting(transcript, previous),
            notice = null,
        )
        operationJob = viewModelScope.launch {
            inferenceMutex.withLock {
                // Release Whisper before Qwen can create its only native context.
                speechToTextEngine.release()
                val request = RewriteRequest(
                    transcript = transcript.text,
                    messageType = RewriteMessageType.valueOf(mutableState.value.messageType.name),
                    tone = RewriteTone.valueOf(mutableState.value.tone.name),
                )
                try {
                    when (val result = rewriteEngine.rewrite(request)) {
                        is RewriteResult.Success -> mutableState.value = mutableState.value.copy(
                            recordingState = RecordingState.Rewritten(
                                transcript = transcript,
                                message = result.message,
                                generatedMessage = result.message,
                                metrics = result.metrics,
                            ),
                        )
                        is RewriteResult.Failure -> if (result.error != RewriteError.Cancelled) {
                            mutableState.value = if (previous != null) {
                                mutableState.value.copy(
                                    recordingState = previous.copy(regenerateConfirmationRequired = false),
                                    notice = result.error.userMessage,
                                )
                            } else {
                                mutableState.value.copy(
                                    recordingState = RecordingState.RewriteFailed(transcript, result.error),
                                )
                            }
                        }
                    }
                } finally {
                    rewriteEngine.release()
                }
            }
        }
    }

    fun updateMessage(text: String) {
        val current = mutableState.value.recordingState as? RecordingState.Rewritten ?: return
        mutableState.value = mutableState.value.copy(
            recordingState = current.copy(
                message = text.take(MAX_MESSAGE_CHARACTERS),
                regenerateConfirmationRequired = false,
            ),
        )
    }

    fun requestRegenerate() {
        val current = mutableState.value.recordingState as? RecordingState.Rewritten ?: return
        if (current.isEdited) {
            mutableState.value = mutableState.value.copy(
                recordingState = current.copy(regenerateConfirmationRequired = true),
            )
        } else {
            rewriteMessage()
        }
    }

    fun dismissRegenerateConfirmation() {
        val current = mutableState.value.recordingState as? RecordingState.Rewritten ?: return
        mutableState.value = mutableState.value.copy(
            recordingState = current.copy(regenerateConfirmationRequired = false),
        )
    }

    fun confirmRegenerate() {
        val current = mutableState.value.recordingState as? RecordingState.Rewritten ?: return
        mutableState.value = mutableState.value.copy(
            recordingState = current.copy(regenerateConfirmationRequired = false),
        )
        rewriteMessage()
    }

    fun finalMessagePayload(): String? =
        (mutableState.value.recordingState as? RecordingState.Rewritten)?.message?.takeIf(String::isNotBlank)

    fun onMessageCopied() {
        if (finalMessagePayload() != null) {
            mutableState.value = mutableState.value.copy(notice = "Message copied")
        }
    }

    fun onCopyFailed() {
        mutableState.value = mutableState.value.copy(notice = "We couldn't copy the message.")
    }

    fun onShareFailed() {
        mutableState.value = mutableState.value.copy(notice = "We couldn't open the share sheet.")
    }

    fun startOver() {
        timerJob?.cancel()
        when (mutableState.value.recordingState) {
            is RecordingState.Recording,
            is RecordingState.Starting,
            is RecordingState.Stopping,
            -> Unit
            is RecordingState.Transcribing -> speechToTextEngine.cancel()
            is RecordingState.Rewriting -> rewriteEngine.cancel()
            else -> Unit
        }
        val activeOperation = operationJob
        activeOperation?.cancel()
        operationJob = viewModelScope.launch {
            activeOperation?.join()
            audioRecorder.cancel()
            audioRecorder.discardCompleted()
            speechToTextEngine.release()
            rewriteEngine.release()
        }
        mutableState.value = mutableState.value.copy(recordingState = RecordingState.Idle, notice = null)
    }

    fun cancelRewrite(message: String? = null) {
        val current = mutableState.value.recordingState as? RecordingState.Rewriting ?: return
        rewriteEngine.cancel()
        operationJob?.cancel()
        mutableState.value = mutableState.value.copy(
            recordingState = current.previous ?: current.transcript,
            notice = message,
        )
    }

    fun backToTranscript() {
        val transcript = when (val current = mutableState.value.recordingState) {
            is RecordingState.Rewritten -> current.transcript
            is RecordingState.RewriteFailed -> current.transcript
            else -> return
        }
        mutableState.value = mutableState.value.copy(recordingState = transcript)
    }

    fun updateTranscript(text: String) {
        val transcript = mutableState.value.recordingState as? RecordingState.Transcript ?: return
        mutableState.value = mutableState.value.copy(
            recordingState = transcript.copy(text = text.take(MAX_TRANSCRIPT_CHARACTERS)),
        )
    }

    fun onAppBackgrounded() {
        if (mutableState.value.recordingState is RecordingState.Recording ||
            mutableState.value.recordingState is RecordingState.Starting ||
            mutableState.value.recordingState is RecordingState.Transcribing
            || mutableState.value.recordingState is RecordingState.Rewriting
        ) {
            if (mutableState.value.recordingState is RecordingState.Rewriting) {
                cancelRewrite("Rewriting was cancelled when the app moved to the background.")
            } else if (mutableState.value.recordingState is RecordingState.Transcribing) {
                cancelTranscription("Transcription was cancelled when the app moved to the background.")
            } else {
                cancelRecording("Recording was cancelled when the app moved to the background.")
            }
        }
    }

    fun onWorkflowLeft() {
        when (mutableState.value.recordingState) {
            is RecordingState.Recording,
            is RecordingState.Starting,
            is RecordingState.Stopping,
            -> cancelRecording()
            is RecordingState.Transcribing -> cancelTranscription()
            is RecordingState.Rewriting -> cancelRewrite()
            is RecordingState.Transcript,
            is RecordingState.TranscriptionFailed,
            is RecordingState.Rewritten,
            is RecordingState.RewriteFailed,
            -> discardTranscript()
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
        runBlocking {
            speechToTextEngine.cancel()
            rewriteEngine.cancel()
            operationJob?.cancelAndJoin()
            audioRecorder.cancel()
            audioRecorder.discardCompleted()
        }
        speechToTextEngine.release()
        rewriteEngine.release()
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
                        recordingState = RecordingState.Transcribing(result.value.durationMillis),
                        notice = if (maximumReached) {
                            "The two-minute limit was reached. Your voice note was stopped safely."
                        } else {
                            null
                        },
                    )
                    try {
                        when (val transcription = inferenceMutex.withLock {
                            try {
                                speechToTextEngine.transcribe(result.value)
                            } finally {
                                speechToTextEngine.release()
                            }
                        }) {
                            is TranscriptionResult.Success -> {
                                mutableState.value = mutableState.value.copy(
                                    recordingState = RecordingState.Transcript(
                                        text = transcription.transcript,
                                        audioDurationMillis = transcription.audioDurationMillis,
                                        modelLoadMillis = transcription.modelLoadMillis,
                                        transcriptionMillis = transcription.transcriptionMillis,
                                    ),
                                )
                            }
                            is TranscriptionResult.Failure -> {
                                if (transcription.error != TranscriptionError.Cancelled) {
                                    mutableState.value = mutableState.value.copy(
                                        recordingState = RecordingState.TranscriptionFailed(
                                            transcription.error,
                                        ),
                                    )
                                }
                            }
                        }
                    } finally {
                        withContext(NonCancellable) { audioRecorder.discardCompleted() }
                    }
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
        private const val MAX_TRANSCRIPT_CHARACTERS = 12_000
        private const val MAX_MESSAGE_CHARACTERS = 12_000

        fun factory(audioRecorder: AudioRecorder, context: Context) = viewModelFactory {
            initializer {
                HomeViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    audioRecorder = audioRecorder,
                    speechToTextEngine = WhisperSpeechToTextEngine(context.applicationContext),
                    rewriteEngine = QwenRewriteEngine(context.applicationContext),
                )
            }
        }
    }
}
