package com.example.twinmindassignment.core.util

import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile

object AudioUtils {

    fun writeWavHeader(
        out: OutputStream,
        sampleRate: Int = Constants.SAMPLE_RATE,
        channels: Int = Constants.CHANNELS,
        bitsPerSample: Int = 16
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        // Write with placeholder data size (0), will be updated later
        val header = ByteArray(44)

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        // File size - 8 (placeholder, will be updated)
        writeInt(header, 4, 0)
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt sub-chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeInt(header, 16, 16) // Sub-chunk size
        writeShort(header, 20, 1) // Audio format (PCM)
        writeShort(header, 22, channels)
        writeInt(header, 24, sampleRate)
        writeInt(header, 28, byteRate)
        writeShort(header, 32, blockAlign)
        writeShort(header, 34, bitsPerSample)

        // data sub-chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeInt(header, 40, 0) // Data size placeholder

        out.write(header)
    }

    fun updateWavHeader(file: File, dataSize: Int) {
        val raf = RandomAccessFile(file, "rw")
        raf.use {
            // Update RIFF chunk size (file size - 8)
            it.seek(4)
            it.write(intToByteArray(dataSize + 36))

            // Update data sub-chunk size
            it.seek(40)
            it.write(intToByteArray(dataSize))
        }
    }

    fun pcmToShortArray(buffer: ByteArray, bytesRead: Int): ShortArray {
        val shorts = ShortArray(bytesRead / 2)
        for (i in shorts.indices) {
            shorts[i] = ((buffer[i * 2 + 1].toInt() shl 8) or (buffer[i * 2].toInt() and 0xFF)).toShort()
        }
        return shorts
    }

    private fun writeInt(header: ByteArray, offset: Int, value: Int) {
        header[offset] = (value and 0xFF).toByte()
        header[offset + 1] = ((value shr 8) and 0xFF).toByte()
        header[offset + 2] = ((value shr 16) and 0xFF).toByte()
        header[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShort(header: ByteArray, offset: Int, value: Int) {
        header[offset] = (value and 0xFF).toByte()
        header[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }
}
