package com.kanwarai.voiceprofessionalmessage.ai.rewrite

internal class QwenNativeBridge {
    external fun create(modelPath: String): Long
    external fun rewrite(handle: Long, system: String, user: String): Array<String>?
    external fun cancel(handle: Long)
    external fun destroy(handle: Long)

    companion object { init { System.loadLibrary("qwen_rewrite") } }
}
