package com.kanwarai.voiceprofessionalmessage.ai.rewrite

enum class RewriteError(val userMessage: String) {
    ModelMissing("The local writing model is not installed. Add the verified Qwen model to the app's private model folder."),
    ModelInvalid("The local writing model failed its integrity check."),
    InputEmpty("Enter a transcript before rewriting."),
    InputTooLong("This transcript is too long for the local writing model. Shorten it and try again."),
    ModelLoadFailed("The local writing model could not be loaded on this device."),
    OutOfMemory("There was not enough memory to rewrite this message locally."),
    IncompleteOutput("The local model reached its response limit before finishing. Try a shorter transcript."),
    UnsafeOutput("The local model produced an unreliable result. Review the transcript and try again."),
    Cancelled("Rewriting was cancelled."),
    NativeRuntimeFailure("The local writing model could not complete the rewrite."),
}

data class RewriteMetrics(
    val modelLoadMillis: Long,
    val promptEvaluationMillis: Long,
    val generationMillis: Long,
    val generatedTokens: Int,
)

sealed interface RewriteResult {
    data class Success(val message: String, val metrics: RewriteMetrics) : RewriteResult
    data class Failure(val error: RewriteError) : RewriteResult
}

interface RewriteEngine {
    suspend fun rewrite(request: RewriteRequest): RewriteResult
    fun cancel()
    fun release()
}

enum class RewriteMessageType(val label: String) { Message("Message"), Email("Email") }
enum class RewriteTone(val label: String) { Professional("Professional"), Friendly("Friendly"), Concise("Concise") }

data class RewriteRequest(
    val transcript: String,
    val messageType: RewriteMessageType,
    val tone: RewriteTone,
)
