package com.kanwarai.voiceprofessionalmessage.audio

import kotlinx.coroutines.flow.Flow

const val AUDIO_SAMPLE_RATE_HZ = 16_000
const val AUDIO_CHANNEL_COUNT = 1
const val AUDIO_BITS_PER_SAMPLE = 16
const val MAX_RECORDING_DURATION_MILLIS = 120_000L

interface AudioRecorder {
    val events: Flow<AudioRecorderEvent>

    suspend fun cleanupStaleFiles(): Int
    suspend fun start(): AudioRecorderResult<Unit>
    suspend fun stop(): AudioRecorderResult<RecordedAudio>
    suspend fun cancel(): AudioRecorderResult<Unit>
    suspend fun discardCompleted(): AudioRecorderResult<Unit>
    fun release()
}

data class RecordedAudio(
    val durationMillis: Long,
    val byteCount: Long,
)

sealed interface AudioRecorderEvent {
    data class CaptureFailed(val error: AudioRecorderError) : AudioRecorderEvent
}

sealed interface AudioRecorderResult<out T> {
    data class Success<T>(val value: T) : AudioRecorderResult<T>
    data class Failure(val error: AudioRecorderError) : AudioRecorderResult<Nothing>
}

enum class AudioRecorderError(val userMessage: String) {
    PermissionDenied("Microphone access is required to record a voice note."),
    AlreadyRecording("A recording is already in progress."),
    NotRecording("There is no active recording."),
    MicrophoneUnavailable("The microphone is unavailable. Close other recording apps and try again."),
    InitializationFailed("The microphone could not be prepared. Please try again."),
    FileCreationFailed("A temporary audio file could not be created."),
    CaptureFailed("Recording stopped unexpectedly. Please try again."),
    FinalizationFailed("The voice note could not be finalized. Please record it again."),
    EmptyRecording("No audio was captured. Please try again."),
    CleanupFailed("The temporary voice note could not be removed."),
}
