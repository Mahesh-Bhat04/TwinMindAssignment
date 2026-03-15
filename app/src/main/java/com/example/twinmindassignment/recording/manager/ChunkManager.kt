package com.example.twinmindassignment.recording.manager

import android.content.Context
import android.media.AudioRecord
import com.example.twinmindassignment.core.database.entity.AudioChunkEntity
import com.example.twinmindassignment.core.model.ChunkStatus
import com.example.twinmindassignment.core.model.SilenceResult
import com.example.twinmindassignment.core.util.AudioUtils
import com.example.twinmindassignment.core.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

class ChunkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var currentChunkIndex = 0
    private var currentOutputStream: FileOutputStream? = null
    private var currentChunkFile: File? = null
    private var bytesWrittenInCurrentChunk = 0
    private var currentChunkStartMs = 0L
    private var currentChunkId = ""

    // Ring buffer for overlap
    private val overlapBuffer = ByteArray(Constants.OVERLAP_BUFFER_SIZE)
    private var overlapBufferPos = 0
    private var overlapBufferFilled = false

    private val silenceDetector = SilenceDetector()

    val chunkIndex: Int get() = currentChunkIndex

    fun reset() {
        currentChunkIndex = 0
        bytesWrittenInCurrentChunk = 0
        overlapBufferPos = 0
        overlapBufferFilled = false
        silenceDetector.reset()
        closeCurrentChunk()
    }

    fun startNewChunk(sessionId: String, sessionStartTimeMs: Long, elapsedMs: Long): String {
        closeCurrentChunk()

        currentChunkId = UUID.randomUUID().toString()
        currentChunkFile = createChunkFile(sessionId, currentChunkIndex)
        currentOutputStream = FileOutputStream(currentChunkFile!!)
        AudioUtils.writeWavHeader(currentOutputStream!!)
        bytesWrittenInCurrentChunk = 0
        currentChunkStartMs = elapsedMs

        // Write overlap from previous chunk (except for the first chunk)
        if (currentChunkIndex > 0 && overlapBufferFilled) {
            // Write the entire overlap buffer
            if (overlapBufferPos < overlapBuffer.size) {
                // Buffer hasn't wrapped around yet, write what we have
                currentOutputStream!!.write(overlapBuffer, 0, overlapBufferPos)
                bytesWrittenInCurrentChunk += overlapBufferPos
            } else {
                // Buffer has been filled completely
                currentOutputStream!!.write(overlapBuffer)
                bytesWrittenInCurrentChunk += overlapBuffer.size
            }
        }

        return currentChunkId
    }

    data class WriteResult(
        val silenceResult: SilenceResult,
        val chunkComplete: Boolean,
        val completedChunk: AudioChunkEntity? = null
    )

    fun writeData(
        buffer: ByteArray,
        bytesRead: Int,
        sessionId: String,
        sessionStartTimeMs: Long,
        elapsedMs: Long
    ): WriteResult {
        // Write to current chunk file
        currentOutputStream?.write(buffer, 0, bytesRead)
        bytesWrittenInCurrentChunk += bytesRead

        // Update overlap ring buffer
        updateOverlapBuffer(buffer, bytesRead)

        // Analyze for silence
        val shortBuffer = AudioUtils.pcmToShortArray(buffer, bytesRead)
        val silenceResult = silenceDetector.analyze(
            shortBuffer,
            shortBuffer.size,
            System.currentTimeMillis()
        )

        // Check if chunk is complete (30 seconds of audio data)
        val bytesPerSecond = Constants.SAMPLE_RATE * Constants.BYTES_PER_SAMPLE * Constants.CHANNELS
        val chunkDurationBytes = (Constants.CHUNK_DURATION_MS * bytesPerSecond / 1000).toInt()

        if (bytesWrittenInCurrentChunk >= chunkDurationBytes) {
            val chunk = finalizeCurrentChunk(sessionId, elapsedMs)
            currentChunkIndex++
            return WriteResult(silenceResult, true, chunk)
        }

        return WriteResult(silenceResult, false)
    }

    private fun finalizeCurrentChunk(sessionId: String, elapsedMs: Long): AudioChunkEntity? {
        val file = currentChunkFile ?: return null
        closeCurrentChunk()

        // Update WAV header with actual data size
        AudioUtils.updateWavHeader(file, bytesWrittenInCurrentChunk)

        val overlapMs = if (currentChunkIndex == 0) 0L else Constants.OVERLAP_DURATION_MS

        return AudioChunkEntity(
            id = currentChunkId,
            sessionId = sessionId,
            chunkIndex = currentChunkIndex,
            filePath = file.absolutePath,
            startTimeMs = currentChunkStartMs,
            endTimeMs = elapsedMs,
            durationMs = elapsedMs - currentChunkStartMs,
            overlapMs = overlapMs,
            fileSizeBytes = file.length(),
            status = ChunkStatus.SAVED.name,
            retryCount = 0,
            isSilent = false,
            createdAt = System.currentTimeMillis()
        )
    }

    fun finalizeLastChunk(sessionId: String, elapsedMs: Long): AudioChunkEntity? {
        if (bytesWrittenInCurrentChunk == 0) {
            closeCurrentChunk()
            return null
        }
        val chunk = finalizeCurrentChunk(sessionId, elapsedMs)
        currentChunkIndex++
        return chunk
    }

    private fun updateOverlapBuffer(buffer: ByteArray, bytesRead: Int) {
        for (i in 0 until bytesRead) {
            overlapBuffer[overlapBufferPos % overlapBuffer.size] = buffer[i]
            overlapBufferPos++
            if (overlapBufferPos >= overlapBuffer.size) {
                overlapBufferFilled = true
                overlapBufferPos = 0
            }
        }
    }

    private fun closeCurrentChunk() {
        try {
            currentOutputStream?.flush()
            currentOutputStream?.close()
        } catch (_: Exception) {
        }
        currentOutputStream = null
    }

    private fun createChunkFile(sessionId: String, index: Int): File {
        val dir = File(context.filesDir, "recordings/$sessionId")
        dir.mkdirs()
        return File(dir, "chunk_${index.toString().padStart(4, '0')}.wav")
    }

    fun getRecordingDirectory(sessionId: String): File {
        return File(context.filesDir, "recordings/$sessionId")
    }
}
