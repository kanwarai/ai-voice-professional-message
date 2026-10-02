package com.kanwarai.voiceprofessionalmessage.ai.rewrite

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

internal class QwenRewriteEngine(
    context: Context,
    private val bridge: QwenNativeBridge = QwenNativeBridge(),
) : RewriteEngine {
    private val modelFile = File(context.noBackupFilesDir, "models/${QwenModelSpec.FILE_NAME}")
    private val mutex = Mutex()
    @Volatile private var handle = 0L

    override suspend fun rewrite(request: RewriteRequest): RewriteResult = mutex.withLock {
        val transcript = request.transcript
        if (transcript.isBlank()) return RewriteResult.Failure(RewriteError.InputEmpty)
        when (withContext(Dispatchers.IO) { QwenModelVerifier.verify(modelFile) }) {
            RewriteError.ModelMissing -> return RewriteResult.Failure(RewriteError.ModelMissing)
            RewriteError.ModelInvalid -> return RewriteResult.Failure(RewriteError.ModelInvalid)
            else -> Unit
        }
        try {
            var loadMillis = 0L
            if (handle == 0L) {
                val started = System.nanoTime()
                handle = withContext(Dispatchers.Default) { bridge.create(modelFile.absolutePath) }
                loadMillis = (System.nanoTime() - started) / 1_000_000
                if (handle == 0L) return RewriteResult.Failure(RewriteError.ModelLoadFailed)
            }
            val fields = withContext(Dispatchers.Default) {
                bridge.rewrite(handle, RewritePrompt.system(request.messageType, request.tone), RewritePrompt.user(transcript))
            } ?: return RewriteResult.Failure(RewriteError.NativeRuntimeFailure)
            if (fields.size < 5) return RewriteResult.Failure(RewriteError.NativeRuntimeFailure)
            when (fields[0]) {
                "TOO_LONG" -> RewriteResult.Failure(RewriteError.InputTooLong)
                "CANCELLED" -> RewriteResult.Failure(RewriteError.Cancelled)
                "LIMIT" -> RewriteResult.Failure(RewriteError.IncompleteOutput)
                "OK" -> {
                    val validated = RewriteOutputValidator.validate(transcript, fields[1])
                        ?: return RewriteResult.Failure(RewriteError.UnsafeOutput)
                    RewriteResult.Success(validated, RewriteMetrics(loadMillis, fields[2].toLong(), fields[3].toLong(), fields[4].toInt()))
                }
                else -> RewriteResult.Failure(RewriteError.NativeRuntimeFailure)
            }
        } catch (_: OutOfMemoryError) {
            RewriteResult.Failure(RewriteError.OutOfMemory)
        } catch (cancelled: CancellationException) {
            bridge.cancel(handle)
            throw cancelled
        } catch (_: Throwable) {
            RewriteResult.Failure(RewriteError.NativeRuntimeFailure)
        }
    }

    override fun cancel() { if (handle != 0L) bridge.cancel(handle) }
    override fun release() { val old = handle; handle = 0; if (old != 0L) bridge.destroy(old) }

}
