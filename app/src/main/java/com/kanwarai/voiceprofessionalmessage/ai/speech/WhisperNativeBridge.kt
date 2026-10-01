package com.kanwarai.voiceprofessionalmessage.ai.speech

internal object WhisperNativeBridge {
    init {
        System.loadLibrary("voice_whisper")
    }

    external fun create(modelPath: String): Long
    external fun resetAbort(handle: Long)
    external fun transcribe(handle: Long, wavPath: String, threadCount: Int): String?
    external fun requestAbort(handle: Long)
    external fun destroy(handle: Long)
}
