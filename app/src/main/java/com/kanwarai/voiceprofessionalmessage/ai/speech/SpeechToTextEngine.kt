package com.kanwarai.voiceprofessionalmessage.ai.speech

import com.kanwarai.voiceprofessionalmessage.audio.RecordedAudio

interface SpeechToTextEngine {
    suspend fun transcribe(audio: RecordedAudio): TranscriptionResult
    fun cancel()
    fun release()
}

sealed interface TranscriptionResult {
    data class Success(
        val transcript: String,
        val audioDurationMillis: Long,
        val modelLoadMillis: Long,
        val transcriptionMillis: Long,
    ) : TranscriptionResult {
        val realTimeFactor: Double
            get() = if (audioDurationMillis > 0) {
                transcriptionMillis.toDouble() / audioDurationMillis
            } else {
                0.0
            }
    }

    data class Failure(val error: TranscriptionError) : TranscriptionResult
}

enum class TranscriptionError(val userMessage: String) {
    ModelMissing("Speech model not installed. Add the verified tiny.en model, then record again."),
    ModelInvalid("The installed speech model is incomplete or unsupported."),
    ModelLoadFailed("The speech model could not be loaded."),
    NativeInitializationFailed("Local speech recognition could not be initialized."),
    AudioInvalid("The voice note is not a valid 16 kHz mono PCM recording."),
    NoSpeechDetected("We couldn't detect clear speech. Try recording again."),
    TranscriptionFailed("The voice note could not be transcribed. Please record it again."),
    Cancelled("Transcription was cancelled."),
    NativeRuntimeFailure("Local speech recognition stopped unexpectedly."),
    OutOfMemory("There was not enough memory to transcribe this voice note."),
}
