package com.kanwarai.voiceprofessionalmessage.presentation

import androidx.lifecycle.SavedStateHandle
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
        val viewModel = HomeViewModel(savedStateHandle, recorder)

        viewModel.selectMessageType(MessageType.Email)
        viewModel.selectTone(MessageTone.Friendly)
        val restoredViewModel = HomeViewModel(savedStateHandle, recorder)

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
    fun startAndStopProduceReadyState() = runTest(mainDispatcherRule.testDispatcher) {
        val recorder = FakeAudioRecorder()
        val viewModel = createViewModel(recorder)

        viewModel.startRecording()
        runCurrent()
        assertTrue(viewModel.state.value.recordingState is RecordingState.Recording)

        viewModel.stopRecording()
        runCurrent()

        assertEquals(1, recorder.startCalls)
        assertEquals(1, recorder.stopCalls)
        assertEquals(RecordingState.Ready(1_500, 48_044), viewModel.state.value.recordingState)
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
            MonotonicClock { now },
        )
        viewModel.startRecording()
        runCurrent()

        now += MAX_RECORDING_DURATION_MILLIS
        advanceTimeBy(250)
        runCurrent()

        assertEquals(1, recorder.stopCalls)
        assertTrue(viewModel.state.value.recordingState is RecordingState.Ready)
        assertEquals(
            "The two-minute limit was reached. Your voice note was stopped safely.",
            viewModel.state.value.notice,
        )
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
    ) = HomeViewModel(
        savedStateHandle = SavedStateHandle(),
        audioRecorder = recorder,
        clock = MonotonicClock { 1_000L },
    )
}
