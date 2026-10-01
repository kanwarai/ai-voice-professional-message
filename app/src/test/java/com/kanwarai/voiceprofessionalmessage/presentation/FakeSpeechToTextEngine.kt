package com.kanwarai.voiceprofessionalmessage.presentation

import com.kanwarai.voiceprofessionalmessage.ai.speech.SpeechToTextEngine
import com.kanwarai.voiceprofessionalmessage.ai.speech.TranscriptionResult
import com.kanwarai.voiceprofessionalmessage.audio.RecordedAudio

class FakeSpeechToTextEngine : SpeechToTextEngine {
    var result: TranscriptionResult = TranscriptionResult.Success(
        transcript = "Test transcript",
        audioDurationMillis = 1_500,
        modelLoadMillis = 100,
        transcriptionMillis = 300,
    )
    var transcribeCalls = 0
    var cancelCalls = 0
    var releaseCalls = 0

    override suspend fun transcribe(audio: RecordedAudio): TranscriptionResult {
        transcribeCalls += 1
        return result
    }

    override fun cancel() {
        cancelCalls += 1
    }

    override fun release() {
        releaseCalls += 1
    }
}
