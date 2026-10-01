package com.kanwarai.voiceprofessionalmessage.ai.speech

import com.kanwarai.voiceprofessionalmessage.audio.WavWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WavInspectorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun acceptsExpectedPcmAndMeasuresDuration() {
        val file = temporaryFolder.newFile("valid.wav")
        WavWriter(file).use { writer ->
            val pcm = ByteArray(32_000)
            repeat(16_000) { index ->
                val sample = if (index % 2 == 0) 4_000 else -4_000
                pcm[index * 2] = sample.toByte()
                pcm[index * 2 + 1] = (sample shr 8).toByte()
            }
            writer.write(pcm, 0, pcm.size)
            writer.finish()
        }

        val inspection = WavInspector.inspect(file)

        assertNotNull(inspection)
        assertEquals(1_000L, inspection?.durationMillis)
    }

    @Test
    fun rejectsNonWavInput() {
        val file = temporaryFolder.newFile("invalid.wav")
        file.writeText("not audio")
        assertNull(WavInspector.inspect(file))
    }
}
