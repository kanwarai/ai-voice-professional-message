package com.kanwarai.voiceprofessionalmessage.presentation

import androidx.lifecycle.SavedStateHandle
import com.kanwarai.voiceprofessionalmessage.ai.speech.TranscriptionError
import com.kanwarai.voiceprofessionalmessage.ai.speech.TranscriptionResult
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorderError
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorderResult
import com.kanwarai.voiceprofessionalmessage.audio.MAX_RECORDING_DURATION_MILLIS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun defaultsMatchProductSpecification() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        assertEquals(MessageType.Message, viewModel.state.value.messageType)
        assertEquals(MessageTone.Professional, viewModel.state.value.tone)
        assertEquals(RecordingState.Idle, viewModel.state.value.recordingState)
    }

    @Test
    fun selectionsUpdateAndRestoreFromSavedState() = runTest(mainDispatcherRule.testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val recorder = FakeAudioRecorder()
        val viewModel = HomeViewModel(savedStateHandle, recorder, FakeSpeechToTextEngine(), FakeRewriteEngine())

        viewModel.selectMessageType(MessageType.Email)
        viewModel.selectTone(MessageTone.Friendly)
        val restoredViewModel = HomeViewModel(savedStateHandle, recorder, FakeSpeechToTextEngine(), FakeRewriteEngine())

        assertEquals(MessageType.Email, restoredViewModel.state.value.messageType)
        assertEquals(MessageTone.Friendly, restoredViewModel.state.value.tone)
    }

    @Test
    fun permissionDenialDistinguishesRetryFromSettingsRecovery() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onPermissionRequestStarted()
        viewModel.onPermissionResult(granted = false, canAskAgain = true)
        assertEquals(RecordingState.PermissionDenied(false), viewModel.state.value.recordingState)

        viewModel.onPermissionRequestStarted()
        viewModel.onPermissionResult(granted = false, canAskAgain = false)
        assertEquals(RecordingState.PermissionDenied(true), viewModel.state.value.recordingState)

        viewModel.onPermissionAvailable()
        assertEquals(RecordingState.Idle, viewModel.state.value.recordingState)
    }

    @Test
    fun startStopAndTranscriptionProduceTranscriptState() = runTest(mainDispatcherRule.testDispatcher) {
        val recorder = FakeAudioRecorder()
        val speechEngine = FakeSpeechToTextEngine()
        val viewModel = createViewModel(recorder, speechEngine)

        viewModel.startRecording()
        runCurrent()
        assertTrue(viewModel.state.value.recordingState is RecordingState.Recording)

        viewModel.stopRecording()
        runCurrent()

        assertEquals(1, recorder.startCalls)
        assertEquals(1, recorder.stopCalls)
        assertEquals(1, speechEngine.transcribeCalls)
        assertEquals(1, recorder.discardCalls)
        assertEquals(
            RecordingState.Transcript("Test transcript", 1_500, 100, 300),
            viewModel.state.value.recordingState,
        )
    }

    @Test
    fun selectorsCannotChangeDuringRecording() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.startRecording()
        runCurrent()

        viewModel.selectMessageType(MessageType.Email)
        viewModel.selectTone(MessageTone.Concise)

        assertEquals(MessageType.Message, viewModel.state.value.messageType)
        assertEquals(MessageTone.Professional, viewModel.state.value.tone)
        assertFalse(viewModel.state.value.configurationEnabled)

        viewModel.cancelRecording()
        runCurrent()
    }

    @Test
    fun cancelDeletesActiveRecordingAndReturnsIdle() = runTest(mainDispatcherRule.testDispatcher) {
        val recorder = FakeAudioRecorder()
        val viewModel = createViewModel(recorder)
        viewModel.startRecording()
        runCurrent()

        viewModel.cancelRecording()
        runCurrent()

        assertEquals(1, recorder.cancelCalls)
        assertEquals(RecordingState.Idle, viewModel.state.value.recordingState)
    }

    @Test
    fun backgroundCancelsRecordingWithClearNotice() = runTest(mainDispatcherRule.testDispatcher) {
        val recorder = FakeAudioRecorder()
        val viewModel = createViewModel(recorder)
        viewModel.startRecording()
        runCurrent()

        viewModel.onAppBackgrounded()
        runCurrent()

        assertEquals(RecordingState.Idle, viewModel.state.value.recordingState)
        assertEquals(
            "Recording was cancelled when the app moved to the background.",
            viewModel.state.value.notice,
        )
    }

    @Test
    fun backgroundDuringStartupCancelsBeforeRecordingCanContinue() =
        runTest(mainDispatcherRule.testDispatcher) {
            val recorder = FakeAudioRecorder()
            val viewModel = createViewModel(recorder)

            viewModel.startRecording()
            viewModel.onAppBackgrounded()
            runCurrent()

            assertEquals(1, recorder.cancelCalls)
            assertEquals(RecordingState.Idle, viewModel.state.value.recordingState)
            assertEquals(
                "Recording was cancelled when the app moved to the background.",
                viewModel.state.value.notice,
            )
        }

    @Test
    fun maximumDurationStopsAndNotifies() = runTest(mainDispatcherRule.testDispatcher) {
        val recorder = FakeAudioRecorder()
        var now = 10_000L
        val viewModel = HomeViewModel(
            SavedStateHandle(),
            recorder,
            FakeSpeechToTextEngine(),
            FakeRewriteEngine(),
            MonotonicClock { now },
        )
        viewModel.startRecording()
        runCurrent()

        now += MAX_RECORDING_DURATION_MILLIS
        advanceTimeBy(250)
        runCurrent()

        assertEquals(1, recorder.stopCalls)
        assertTrue(viewModel.state.value.recordingState is RecordingState.Transcript)
        assertEquals(
            "The two-minute limit was reached. Your voice note was stopped safely.",
            viewModel.state.value.notice,
        )
    }

    @Test
    fun modelMissingIsHonestAndDeletesAudio() = runTest(mainDispatcherRule.testDispatcher) {
        val recorder = FakeAudioRecorder()
        val speechEngine = FakeSpeechToTextEngine().apply {
            result = TranscriptionResult.Failure(TranscriptionError.ModelMissing)
        }
        val viewModel = createViewModel(recorder, speechEngine)

        viewModel.startRecording()
        runCurrent()
        viewModel.stopRecording()
        runCurrent()

        assertEquals(
            RecordingState.TranscriptionFailed(TranscriptionError.ModelMissing),
            viewModel.state.value.recordingState,
        )
        assertEquals(1, recorder.discardCalls)
    }

    @Test
    fun transcriptCanBeCorrectedInMemory() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.startRecording()
        runCurrent()
        viewModel.stopRecording()
        runCurrent()

        viewModel.updateTranscript("Corrected transcript")

        val transcript = viewModel.state.value.recordingState as RecordingState.Transcript
        assertEquals("Corrected transcript", transcript.text)
    }

    @Test fun rewriteIsExplicitAndPreservesOriginalTranscript() = runTest(mainDispatcherRule.testDispatcher) {
        val speech = FakeSpeechToTextEngine()
        val rewrite = FakeRewriteEngine()
        val viewModel = HomeViewModel(SavedStateHandle(), FakeAudioRecorder(), speech, rewrite, MonotonicClock { 1_000 })
        viewModel.startRecording(); runCurrent(); viewModel.stopRecording(); runCurrent()
        assertEquals(0, rewrite.calls)
        viewModel.rewriteMessage(); runCurrent()
        val state = viewModel.state.value.recordingState as RecordingState.Rewritten
        assertEquals("Test transcript", state.transcript.text)
        assertEquals("Polished message", state.message)
        assertEquals(1, speech.releaseCalls)
    }

    @Test
    fun recorderFailureMapsToSafeUiError() = runTest(mainDispatcherRule.testDispatcher) {
        val recorder = FakeAudioRecorder().apply {
            startResult = AudioRecorderResult.Failure(AudioRecorderError.MicrophoneUnavailable)
        }
        val viewModel = createViewModel(recorder)

        viewModel.startRecording()
        runCurrent()

        assertEquals(
            RecordingState.Error(AudioRecorderError.MicrophoneUnavailable.userMessage),
            viewModel.state.value.recordingState,
        )
    }

    @Test
    fun durationFormattingIsStable() {
        assertEquals("00:00", formatRecordingDuration(-1))
        assertEquals("00:09", formatRecordingDuration(9_999))
        assertEquals("01:05", formatRecordingDuration(65_000))
        assertEquals("02:00", formatRecordingDuration(MAX_RECORDING_DURATION_MILLIS))
    }

    @Test
    fun noticeCanBeConsumed() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.startRecording()
        runCurrent()
        viewModel.onAppBackgrounded()
        runCurrent()

        viewModel.onNoticeShown()

        assertNull(viewModel.state.value.notice)
    }

    private fun createViewModel(
        recorder: FakeAudioRecorder = FakeAudioRecorder(),
        speechEngine: FakeSpeechToTextEngine = FakeSpeechToTextEngine(),
    ) = HomeViewModel(
        savedStateHandle = SavedStateHandle(),
        audioRecorder = recorder,
        speechToTextEngine = speechEngine,
        rewriteEngine = FakeRewriteEngine(),
        clock = MonotonicClock { 1_000L },
    )
}
