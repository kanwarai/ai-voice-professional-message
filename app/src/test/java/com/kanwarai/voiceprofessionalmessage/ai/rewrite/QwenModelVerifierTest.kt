package com.kanwarai.voiceprofessionalmessage.ai.rewrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class QwenModelVerifierTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun reportsMissingModel() {
        assertEquals(
            RewriteError.ModelMissing,
            QwenModelVerifier.verify(temporaryFolder.root.resolve("missing.gguf"), 1, "00"),
        )
    }

    @Test fun rejectsWrongSizeAndHash() {
        val file = temporaryFolder.newFile("model.gguf").apply { writeText("model") }
        assertEquals(RewriteError.ModelInvalid, QwenModelVerifier.verify(file, 6, "00"))
        assertEquals(RewriteError.ModelInvalid, QwenModelVerifier.verify(file, 5, "00"))
    }

    @Test fun acceptsExactSizeAndHash() {
        val file = temporaryFolder.newFile("model.gguf").apply { writeText("model") }
        val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertNull(QwenModelVerifier.verify(file, file.length(), hash))
    }
}
