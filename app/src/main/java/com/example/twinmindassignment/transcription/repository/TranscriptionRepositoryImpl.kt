package com.example.twinmindassignment.transcription.repository

import android.util.Log
import com.example.twinmindassignment.core.database.dao.AudioChunkDao
import com.example.twinmindassignment.core.database.dao.MeetingSessionDao
import com.example.twinmindassignment.core.database.dao.TranscriptSegmentDao
import com.example.twinmindassignment.core.database.entity.TranscriptSegmentEntity
import com.example.twinmindassignment.core.di.IoDispatcher
import com.example.twinmindassignment.core.model.ChunkStatus
import com.example.twinmindassignment.core.network.api.TranscriptionApiService
import com.example.twinmindassignment.core.util.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class TranscriptionRepositoryImpl @Inject constructor(
    private val transcriptionApi: TranscriptionApiService,
    private val audioChunkDao: AudioChunkDao,
    private val transcriptSegmentDao: TranscriptSegmentDao,
    private val meetingSessionDao: MeetingSessionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TranscriptionRepository {

    companion object {
        private const val TAG = "TranscriptionRepo"
    }

    override suspend fun transcribeChunk(chunkId: String): Boolean = withContext(ioDispatcher) {
        val chunk = audioChunkDao.getChunkById(chunkId)
        if (chunk == null) {
            Log.e(TAG, "Chunk not found in DB: $chunkId")
            return@withContext false
        }

        try {
            Log.d(TAG, "Starting transcription for chunk $chunkId (file: ${chunk.filePath})")
            audioChunkDao.updateChunkStatus(chunkId, ChunkStatus.UPLOADING.name)

            val file = File(chunk.filePath)
            if (!file.exists()) {
                Log.e(TAG, "Audio file not found: ${chunk.filePath}")
                audioChunkDao.updateChunkStatus(chunkId, ChunkStatus.FAILED.name)
                return@withContext false
            }

            Log.d(TAG, "Uploading file: ${file.name} (${file.length()} bytes)")
            val requestFile = file.asRequestBody("audio/wav".toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = transcriptionApi.transcribeAudio(
                file = filePart,
                model = Constants.WHISPER_MODEL.toRequestBody("text/plain".toMediaType()),
                responseFormat = "verbose_json".toRequestBody("text/plain".toMediaType())
            )

            Log.d(TAG, "Transcription response received: text length=${response.text.length}, segments=${response.segments?.size ?: 0}")

            val maxOrder = transcriptSegmentDao.getMaxOrderIndex(chunk.sessionId) ?: -1

            // Delete any previous segments for this chunk (in case of retry)
            transcriptSegmentDao.deleteSegmentsForChunk(chunkId)

            val segments = if (!response.segments.isNullOrEmpty()) {
                response.segments.mapIndexed { index, seg ->
                    TranscriptSegmentEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = chunk.sessionId,
                        chunkId = chunk.id,
                        text = seg.text.trim(),
                        startTimeMs = chunk.startTimeMs + (seg.start * 1000).toLong(),
                        endTimeMs = chunk.startTimeMs + (seg.end * 1000).toLong(),
                        orderIndex = maxOrder + 1 + index,
                        confidence = null,
                        language = response.language,
                        createdAt = System.currentTimeMillis()
                    )
                }
            } else {
                listOf(
                    TranscriptSegmentEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = chunk.sessionId,
                        chunkId = chunk.id,
                        text = response.text.trim(),
                        startTimeMs = chunk.startTimeMs,
                        endTimeMs = chunk.endTimeMs,
                        orderIndex = maxOrder + 1,
                        confidence = null,
                        language = response.language,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            transcriptSegmentDao.insertSegments(segments)
            audioChunkDao.updateChunkStatus(chunkId, ChunkStatus.TRANSCRIBED.name)
            meetingSessionDao.incrementTranscribedCount(chunk.sessionId, System.currentTimeMillis())

            Log.d(TAG, "Chunk $chunkId transcribed successfully (${segments.size} segments)")
            true
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "HTTP ${e.code()} transcribing chunk $chunkId: $errorBody", e)
            audioChunkDao.updateChunkStatus(chunkId, ChunkStatus.FAILED.name)
            val updated = chunk.copy(retryCount = chunk.retryCount + 1)
            audioChunkDao.updateChunk(updated)
            false
        } catch (e: IOException) {
            Log.e(TAG, "Network error transcribing chunk $chunkId: ${e.message}", e)
            audioChunkDao.updateChunkStatus(chunkId, ChunkStatus.FAILED.name)
            val updated = chunk.copy(retryCount = chunk.retryCount + 1)
            audioChunkDao.updateChunk(updated)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error transcribing chunk $chunkId: ${e.message}", e)
            audioChunkDao.updateChunkStatus(chunkId, ChunkStatus.FAILED.name)
            val updated = chunk.copy(retryCount = chunk.retryCount + 1)
            audioChunkDao.updateChunk(updated)
            false
        }
    }

    override suspend fun retryFailedChunks(sessionId: String) = withContext(ioDispatcher) {
        val failed = audioChunkDao.getFailedChunks(sessionId)
        Log.d(TAG, "Retrying ${failed.size} failed chunks for session $sessionId")
        failed.forEach { chunk ->
            transcribeChunk(chunk.id)
        }
    }

    override fun getTranscriptForSession(sessionId: String): Flow<List<TranscriptSegmentEntity>> =
        transcriptSegmentDao.getTranscriptForSession(sessionId)

    override suspend fun getFullTranscriptText(sessionId: String): String? =
        withContext(ioDispatcher) { transcriptSegmentDao.getFullTranscriptText(sessionId) }
}
