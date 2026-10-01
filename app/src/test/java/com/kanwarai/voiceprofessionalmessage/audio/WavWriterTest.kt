package com.kanwarai.voiceprofessionalmessage.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavWriterTest {
    @Test
    fun finalizedFileContainsValidPcmHeaderAndDataLength() {
        val file = temporaryFile()
        val samples = byteArrayOf(0, 0, -1, 127, 0, -128)
        val writer = WavWriter(file)

        writer.write(samples, 0, samples.size)
        writer.finish()

        val bytes = file.readBytes()
        assertEquals("RIFF", bytes.asAscii(0, 4))
        assertEquals(42, bytes.littleEndianInt(4))
        assertEquals("WAVE", bytes.asAscii(8, 4))
        assertEquals("fmt ", bytes.asAscii(12, 4))
        assertEquals(1, bytes.littleEndianShort(20))
        assertEquals(1, bytes.littleEndianShort(22))
        assertEquals(16_000, bytes.littleEndianInt(24))
        assertEquals(32_000, bytes.littleEndianInt(28))
        assertEquals(2, bytes.littleEndianShort(32))
        assertEquals(16, bytes.littleEndianShort(34))
        assertEquals("data", bytes.asAscii(36, 4))
        assertEquals(samples.size, bytes.littleEndianInt(40))
        assertArrayEquals(samples, bytes.copyOfRange(WavWriter.WAV_HEADER_SIZE, bytes.size))
        file.delete()
    }

    @Test
    fun abortDeletesIncompleteFile() {
        val file = temporaryFile()
        val writer = WavWriter(file)
        writer.write(byteArrayOf(1, 2), 0, 2)

        writer.abort()

        assertFalse(file.exists())
    }

    @Test
    fun headerFactoryUsesLittleEndianBoundaries() {
        val header = WavWriter.createHeader(dataLength = 32_000)

        assertEquals(32_036, header.littleEndianInt(4))
        assertEquals(32_000, header.littleEndianInt(40))
        assertTrue(header.size == WavWriter.WAV_HEADER_SIZE)
    }

    private fun temporaryFile(): File = File.createTempFile("wav_writer_test_", ".wav")

    private fun ByteArray.asAscii(offset: Int, length: Int): String =
        copyOfRange(offset, offset + length).decodeToString()

    private fun ByteArray.littleEndianInt(offset: Int): Int =
        ByteBuffer.wrap(this, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private fun ByteArray.littleEndianShort(offset: Int): Int =
        ByteBuffer.wrap(this, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
}
