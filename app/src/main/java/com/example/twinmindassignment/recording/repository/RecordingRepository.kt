package com.example.twinmindassignment.recording.repository

import com.example.twinmindassignment.core.database.entity.AudioChunkEntity
import com.example.twinmindassignment.core.database.entity.MeetingSessionEntity
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun getAllSessions(): Flow<List<MeetingSessionEntity>>
    fun getSessionById(id: String): Flow<MeetingSessionEntity?>
    suspend fun getSessionByIdOnce(id: String): MeetingSessionEntity?
    suspend fun getActiveRecordingSession(): MeetingSessionEntity?
    suspend fun createSession(session: MeetingSessionEntity)
    suspend fun updateSession(session: MeetingSessionEntity)
    suspend fun completeSession(id: String, endTime: Long, duration: Long, status: String)
    suspend fun updateSessionStatus(id: String, status: String)
    suspend fun saveChunk(chunk: AudioChunkEntity)
    suspend fun incrementChunkCount(sessionId: String)
    suspend fun deleteSession(session: MeetingSessionEntity)
    fun getChunksForSession(sessionId: String): Flow<List<AudioChunkEntity>>
}
