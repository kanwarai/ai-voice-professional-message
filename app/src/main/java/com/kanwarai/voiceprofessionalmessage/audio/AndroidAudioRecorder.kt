package com.kanwarai.voiceprofessionalmessage.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class AndroidAudioRecorder(
    context: Context,
) : AudioRecorder {
    private val appContext = context.applicationContext
    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val operationMutex = Mutex()
    private val mutableEvents = MutableSharedFlow<AudioRecorderEvent>(extraBufferCapacity = 1)
    private val temporaryDirectory = File(appContext.cacheDir, TEMP_DIRECTORY_NAME)

    private var activeSession: CaptureSession? = null
    private var completedFile: File? = null

    override val events: Flow<AudioRecorderEvent> = mutableEvents

    override suspend fun cleanupStaleFiles(): Int = operationMutex.withLock {
        if (!temporaryDirectory.exists()) return@withLock 0
        var deletedCount = 0
        temporaryDirectory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(FILE_PREFIX) && file.delete()) {
                deletedCount += 1
            }
        }
        deletedCount
    }

    override suspend fun start(): AudioRecorderResult<Unit> = operationMutex.withLock {
        if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.PermissionDenied)
        }
        if (activeSession != null) {
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.AlreadyRecording)
        }
        if (!discardCompletedFile()) {
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.CleanupFailed)
        }
        if (!temporaryDirectory.exists() && !temporaryDirectory.mkdirs()) {
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.FileCreationFailed)
        }

        val minimumBufferSize = AudioRecord.getMinBufferSize(
            AUDIO_SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minimumBufferSize <= 0) {
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.MicrophoneUnavailable)
        }
        val bufferSize = maxOf(minimumBufferSize, AUDIO_SAMPLE_RATE_HZ / 5 * 2)
        val file = File(temporaryDirectory, "$FILE_PREFIX${UUID.randomUUID()}.wav")
        val writer = try {
            WavWriter(file)
        } catch (_: IOException) {
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.FileCreationFailed)
        }
        val audioRecord = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(AUDIO_SAMPLE_RATE_HZ)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
        } catch (_: SecurityException) {
            writer.abort()
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.PermissionDenied)
        } catch (_: IllegalArgumentException) {
            writer.abort()
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.InitializationFailed)
        } catch (_: UnsupportedOperationException) {
            writer.abort()
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.InitializationFailed)
        }
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            writer.abort()
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.InitializationFailed)
        }

        try {
            audioRecord.startRecording()
        } catch (_: SecurityException) {
            audioRecord.release()
            writer.abort()
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.PermissionDenied)
        } catch (_: IllegalStateException) {
            audioRecord.release()
            writer.abort()
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.MicrophoneUnavailable)
        }

        val session = CaptureSession(
            audioRecord = audioRecord,
            writer = writer,
            file = file,
            bufferSize = bufferSize,
            startedAtMillis = SystemClock.elapsedRealtime(),
        )
        session.captureJob = recorderScope.launch { capture(session) }
        activeSession = session
        AudioRecorderResult.Success(Unit)
    }

    override suspend fun stop(): AudioRecorderResult<RecordedAudio> = operationMutex.withLock {
        val session = activeSession
            ?: return@withLock AudioRecorderResult.Failure(AudioRecorderError.NotRecording)
        activeSession = null
        stopCapture(session)

        session.failure.get()?.let { error ->
            session.writer.abort()
            return@withLock AudioRecorderResult.Failure(error)
        }
        if (session.writer.dataLength <= 0) {
            session.writer.abort()
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.EmptyRecording)
        }
        try {
            session.writer.finish()
        } catch (_: IOException) {
            session.writer.abort()
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.FinalizationFailed)
        } catch (_: IllegalStateException) {
            session.writer.abort()
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.FinalizationFailed)
        }
        if (!session.file.isFile || session.file.length() <= WavWriter.WAV_HEADER_SIZE) {
            session.file.delete()
            return@withLock AudioRecorderResult.Failure(AudioRecorderError.FinalizationFailed)
        }
        completedFile = session.file
        AudioRecorderResult.Success(
            RecordedAudio(
                file = session.file,
                durationMillis = (SystemClock.elapsedRealtime() - session.startedAtMillis).coerceAtLeast(0),
                byteCount = session.file.length(),
            ),
        )
    }

    override suspend fun cancel(): AudioRecorderResult<Unit> = operationMutex.withLock {
        val session = activeSession
        activeSession = null
        if (session != null) {
            stopCapture(session)
            session.writer.abort()
        }
        if (!discardCompletedFile()) {
            AudioRecorderResult.Failure(AudioRecorderError.CleanupFailed)
        } else {
            AudioRecorderResult.Success(Unit)
        }
    }

    override suspend fun discardCompleted(): AudioRecorderResult<Unit> = operationMutex.withLock {
        if (discardCompletedFile()) {
            AudioRecorderResult.Success(Unit)
        } else {
            AudioRecorderResult.Failure(AudioRecorderError.CleanupFailed)
        }
    }

    override fun release() {
        runBlocking(Dispatchers.IO) {
            cancel()
            discardCompleted()
        }
        recorderScope.cancel()
    }

    private suspend fun capture(session: CaptureSession) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val buffer = ByteArray(session.bufferSize)
        try {
            while (session.capturing.get()) {
                val bytesRead = session.audioRecord.read(buffer, 0, buffer.size)
                when {
                    bytesRead > 0 -> session.writer.write(buffer, 0, bytesRead)
                    bytesRead == 0 -> Unit
                    else -> throw IOException("AudioRecord read failed with code $bytesRead")
                }
            }
        } catch (_: SecurityException) {
            session.failure.compareAndSet(null, AudioRecorderError.PermissionDenied)
        } catch (_: IOException) {
            session.failure.compareAndSet(null, AudioRecorderError.CaptureFailed)
        } catch (_: IllegalStateException) {
            session.failure.compareAndSet(null, AudioRecorderError.CaptureFailed)
        } finally {
            try {
                if (session.audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    session.audioRecord.stop()
                }
            } catch (_: IllegalStateException) {
                session.failure.compareAndSet(null, AudioRecorderError.CaptureFailed)
            }
            session.audioRecord.release()
            session.failure.get()?.let { error ->
                mutableEvents.tryEmit(AudioRecorderEvent.CaptureFailed(error))
            }
        }
    }

    private suspend fun stopCapture(session: CaptureSession) {
        session.capturing.set(false)
        try {
            if (session.audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                session.audioRecord.stop()
            }
        } catch (_: IllegalStateException) {
            session.failure.compareAndSet(null, AudioRecorderError.CaptureFailed)
        }
        listOf(session.captureJob).joinAll()
    }

    private fun discardCompletedFile(): Boolean {
        val file = completedFile ?: return true
        completedFile = null
        return !file.exists() || file.delete()
    }

    private class CaptureSession(
        val audioRecord: AudioRecord,
        val writer: WavWriter,
        val file: File,
        val bufferSize: Int,
        val startedAtMillis: Long,
        val capturing: AtomicBoolean = AtomicBoolean(true),
        val failure: AtomicReference<AudioRecorderError?> = AtomicReference(null),
    ) {
        lateinit var captureJob: Job
    }

    private companion object {
        const val TEMP_DIRECTORY_NAME = "voice_notes_temp"
        const val FILE_PREFIX = "voice_note_"
    }
}
