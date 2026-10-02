package com.kanwarai.voiceprofessionalmessage.ai.rewrite

import java.io.File
import java.security.MessageDigest

internal object QwenModelVerifier {
    fun verify(
        file: File,
        expectedSize: Long = QwenModelSpec.SIZE_BYTES,
        expectedSha256: String = QwenModelSpec.SHA256,
    ): RewriteError? {
        if (!file.isFile) return RewriteError.ModelMissing
        if (file.length() != expectedSize) return RewriteError.ModelInvalid
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual == expectedSha256) null else RewriteError.ModelInvalid
        } catch (_: Exception) {
            RewriteError.ModelInvalid
        }
    }
}
