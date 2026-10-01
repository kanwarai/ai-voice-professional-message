package com.kanwarai.voiceprofessionalmessage.presentation

import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorder
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorderEvent
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorderResult
import com.kanwarai.voiceprofessionalmessage.audio.RecordedAudio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File

class FakeAudioRecorder : AudioRecorder {
    private val mutableEvents = MutableSharedFlow<AudioRecorderEvent>(extraBufferCapacity = 1)
    override val events: Flow<AudioRecorderEvent> = mutableEvents

    var startResult: AudioRecorderResult<Unit> = AudioRecorderResult.Success(Unit)
    var stopResult: AudioRecorderResult<RecordedAudio> = AudioRecorderResult.Success(
        RecordedAudio(file = File("test.wav"), durationMillis = 1_500, byteCount = 48_044),
    )
    var cancelResult: AudioRecorderResult<Unit> = AudioRecorderResult.Success(Unit)
    var discardResult: AudioRecorderResult<Unit> = AudioRecorderResult.Success(Unit)

    var startCalls = 0
    var stopCalls = 0
    var cancelCalls = 0
    var discardCalls = 0

    override suspend fun cleanupStaleFiles(): Int = 0

    override suspend fun start(): AudioRecorderResult<Unit> {
        startCalls += 1
        return startResult
    }

    override suspend fun stop(): AudioRecorderResult<RecordedAudio> {
        stopCalls += 1
        return stopResult
    }

    override suspend fun cancel(): AudioRecorderResult<Unit> {
        cancelCalls += 1
        return cancelResult
    }

    override suspend fun discardCompleted(): AudioRecorderResult<Unit> {
        discardCalls += 1
        return discardResult
    }

    override fun release() = Unit

    fun emit(event: AudioRecorderEvent) {
        mutableEvents.tryEmit(event)
    }
}
