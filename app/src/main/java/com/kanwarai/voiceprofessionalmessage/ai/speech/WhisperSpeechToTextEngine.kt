package com.kanwarai.voiceprofessionalmessage.ai.speech

import android.content.Context
import android.os.SystemClock
import com.kanwarai.voiceprofessionalmessage.audio.RecordedAudio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

class WhisperSpeechToTextEngine(context: Context) : SpeechToTextEngine {
    private val modelFile = WhisperModelSpec.privateFile(context.applicationContext)
    private val operationMutex = Mutex()
    private val cancellationRequested = AtomicBoolean(false)
    private var nativeHandle = 0L
    private var modelLoadMillis = 0L

    override suspend fun transcribe(audio: RecordedAudio): TranscriptionResult =
        operationMutex.withLock {
            cancellationRequested.set(false)
            if (nativeHandle != 0L) WhisperNativeBridge.resetAbort(nativeHandle)
            val inspection = WavInspector.inspect(audio.file)
                ?: return@withLock TranscriptionResult.Failure(TranscriptionError.AudioInvalid)
            if (inspection.rootMeanSquare < MINIMUM_RMS) {
                return@withLock TranscriptionResult.Failure(TranscriptionError.NoSpeechDetected)
            }
            when (val validationError = validateModel()) {
                null -> Unit
                else -> return@withLock TranscriptionResult.Failure(validationError)
            }

            try {
                if (nativeHandle == 0L) {
                    val loadStarted = SystemClock.elapsedRealtime()
                    nativeHandle = withContext(Dispatchers.Default + NonCancellable) {
                        WhisperNativeBridge.create(modelFile.absolutePath)
                    }
                    modelLoadMillis = SystemClock.elapsedRealtime() - loadStarted
                    if (nativeHandle == 0L) {
                        return@withLock TranscriptionResult.Failure(TranscriptionError.ModelLoadFailed)
                    }
                }
                coroutineContext.ensureActive()
                val started = SystemClock.elapsedRealtime()
                val rawTranscript = withContext(Dispatchers.Default) {
                    WhisperNativeBridge.transcribe(
                        nativeHandle,
                        audio.file.absolutePath,
                        Runtime.getRuntime().availableProcessors().coerceIn(1, 4),
                    )
                }
                val elapsed = SystemClock.elapsedRealtime() - started
                coroutineContext.ensureActive()
                if (cancellationRequested.get()) {
                    TranscriptionResult.Failure(TranscriptionError.Cancelled)
                } else if (rawTranscript == null) {
                    TranscriptionResult.Failure(TranscriptionError.TranscriptionFailed)
                } else {
                    val transcript = TranscriptValidator.normalize(rawTranscript)
                        ?: return@withLock TranscriptionResult.Failure(TranscriptionError.NoSpeechDetected)
                    TranscriptionResult.Success(
                        transcript = transcript,
                        audioDurationMillis = inspection.durationMillis,
                        modelLoadMillis = modelLoadMillis,
                        transcriptionMillis = elapsed,
                    )
                }
            } catch (_: OutOfMemoryError) {
                TranscriptionResult.Failure(TranscriptionError.OutOfMemory)
            } catch (_: UnsatisfiedLinkError) {
                TranscriptionResult.Failure(TranscriptionError.NativeInitializationFailed)
            } catch (_: CancellationException) {
                TranscriptionResult.Failure(TranscriptionError.Cancelled)
            } catch (_: RuntimeException) {
                TranscriptionResult.Failure(TranscriptionError.NativeRuntimeFailure)
            }
        }

    override fun cancel() {
        cancellationRequested.set(true)
        val handle = nativeHandle
        if (handle != 0L) WhisperNativeBridge.requestAbort(handle)
    }

    override fun release() {
        cancel()
        runBlocking(Dispatchers.Default) {
            operationMutex.withLock {
                if (nativeHandle != 0L) {
                    WhisperNativeBridge.destroy(nativeHandle)
                    nativeHandle = 0L
                }
            }
        }
    }

    private suspend fun validateModel(): TranscriptionError? = withContext(Dispatchers.IO) {
        if (!modelFile.isFile) return@withContext TranscriptionError.ModelMissing
        if (modelFile.length() != WhisperModelSpec.BYTE_COUNT) {
            return@withContext TranscriptionError.ModelInvalid
        }
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(modelFile).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual == WhisperModelSpec.SHA256) null else TranscriptionError.ModelInvalid
        } catch (_: IOException) {
            TranscriptionError.ModelInvalid
        }
    }

    private companion object {
        const val MINIMUM_RMS = 0.003
    }
}
