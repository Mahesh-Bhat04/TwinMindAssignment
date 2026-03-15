package com.example.twinmindassignment.transcription.repository

import com.example.twinmindassignment.core.database.entity.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow

interface TranscriptionRepository {
    suspend fun transcribeChunk(chunkId: String): Boolean
    suspend fun retryFailedChunks(sessionId: String)
    fun getTranscriptForSession(sessionId: String): Flow<List<TranscriptSegmentEntity>>
    suspend fun getFullTranscriptText(sessionId: String): String?
}
