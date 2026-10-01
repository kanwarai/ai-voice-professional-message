package com.kanwarai.voiceprofessionalmessage.ai.speech

import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.math.sqrt

data class WavInspection(
    val durationMillis: Long,
    val rootMeanSquare: Double,
)

object WavInspector {
    private const val HEADER_SIZE = 44L
    private const val SAMPLE_RATE = 16_000
    private const val CHANNELS = 1
    private const val BITS_PER_SAMPLE = 16

    fun inspect(file: File): WavInspection? {
        if (!file.isFile || file.length() <= HEADER_SIZE) return null
        return try {
            RandomAccessFile(file, "r").use { input ->
            val header = ByteArray(44)
            input.readFully(header)
            if (header.ascii(0, 4) != "RIFF" || header.ascii(8, 4) != "WAVE" ||
                header.ascii(12, 4) != "fmt " || header.littleEndianInt(16) != 16L ||
                header.littleEndianShort(20) != 1 ||
                header.littleEndianShort(22) != CHANNELS ||
                header.littleEndianInt(24) != SAMPLE_RATE.toLong() ||
                header.littleEndianShort(34) != BITS_PER_SAMPLE ||
                header.ascii(36, 4) != "data"
            ) return null

            val dataLength = header.littleEndianInt(40)
            if (dataLength <= 0 || dataLength % 2 != 0L || dataLength + HEADER_SIZE != file.length()) {
                return null
            }
            var sumSquares = 0.0
            var samples = 0L
            while (samples * 2 < dataLength) {
                val low = input.read()
                val high = input.read()
                if (low < 0 || high < 0) throw EOFException()
                val sample = ((high shl 8) or low).toShort().toInt() / 32768.0
                sumSquares += sample * sample
                samples += 1
            }
            WavInspection(
                durationMillis = samples * 1_000 / SAMPLE_RATE,
                rootMeanSquare = sqrt(sumSquares / samples.coerceAtLeast(1)),
            )
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun ByteArray.ascii(offset: Int, length: Int) =
        copyOfRange(offset, offset + length).decodeToString()

    private fun ByteArray.littleEndianShort(offset: Int): Int =
        (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)

    private fun ByteArray.littleEndianInt(offset: Int): Long =
        (0 until 4).fold(0L) { value, index ->
            value or ((this[offset + index].toLong() and 0xff) shl (index * 8))
        }
}
