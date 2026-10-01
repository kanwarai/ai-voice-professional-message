package com.kanwarai.voiceprofessionalmessage.audio

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

class WavWriter(
    private val file: File,
    private val sampleRateHz: Int = AUDIO_SAMPLE_RATE_HZ,
    private val channelCount: Int = AUDIO_CHANNEL_COUNT,
    private val bitsPerSample: Int = AUDIO_BITS_PER_SAMPLE,
) : Closeable {
    private val output = RandomAccessFile(file, "rw")
    private var finished = false

    var dataLength: Long = 0
        private set

    init {
        require(sampleRateHz > 0)
        require(channelCount > 0)
        require(bitsPerSample == 16)
        output.setLength(0)
        output.write(ByteArray(WAV_HEADER_SIZE))
    }

    fun write(buffer: ByteArray, offset: Int, length: Int) {
        check(!finished) { "WAV writer is already closed" }
        require(offset >= 0 && length >= 0 && offset + length <= buffer.size)
        output.write(buffer, offset, length)
        dataLength += length
    }

    fun finish() {
        check(!finished) { "WAV writer is already closed" }
        check(dataLength <= UINT32_MAX - WAV_HEADER_SIZE) { "WAV data is too large" }
        output.seek(0)
        output.write(createHeader(dataLength, sampleRateHz, channelCount, bitsPerSample))
        output.fd.sync()
        finished = true
        output.close()
    }

    fun abort() {
        if (!finished) {
            finished = true
            output.close()
        }
        file.delete()
    }

    override fun close() {
        if (!finished) abort()
    }

    companion object {
        const val WAV_HEADER_SIZE = 44
        private const val UINT32_MAX = 0xFFFF_FFFFL

        fun createHeader(
            dataLength: Long,
            sampleRateHz: Int = AUDIO_SAMPLE_RATE_HZ,
            channelCount: Int = AUDIO_CHANNEL_COUNT,
            bitsPerSample: Int = AUDIO_BITS_PER_SAMPLE,
        ): ByteArray {
            require(dataLength in 0..(UINT32_MAX - 36))
            val byteRate = sampleRateHz * channelCount * bitsPerSample / 8
            val blockAlign = channelCount * bitsPerSample / 8
            return ByteArray(WAV_HEADER_SIZE).also { header ->
                header.putAscii(0, "RIFF")
                header.putLittleEndianInt(4, dataLength + 36)
                header.putAscii(8, "WAVE")
                header.putAscii(12, "fmt ")
                header.putLittleEndianInt(16, 16)
                header.putLittleEndianShort(20, 1)
                header.putLittleEndianShort(22, channelCount)
                header.putLittleEndianInt(24, sampleRateHz.toLong())
                header.putLittleEndianInt(28, byteRate.toLong())
                header.putLittleEndianShort(32, blockAlign)
                header.putLittleEndianShort(34, bitsPerSample)
                header.putAscii(36, "data")
                header.putLittleEndianInt(40, dataLength)
            }
        }

        private fun ByteArray.putAscii(offset: Int, value: String) {
            value.encodeToByteArray().copyInto(this, offset)
        }

        private fun ByteArray.putLittleEndianShort(offset: Int, value: Int) {
            this[offset] = value.toByte()
            this[offset + 1] = (value ushr 8).toByte()
        }

        private fun ByteArray.putLittleEndianInt(offset: Int, value: Long) {
            repeat(4) { byteIndex ->
                this[offset + byteIndex] = (value ushr (byteIndex * 8)).toByte()
            }
        }
    }
}
