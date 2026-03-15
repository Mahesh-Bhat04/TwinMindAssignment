package com.example.twinmindassignment.recording.repository

import com.example.twinmindassignment.core.database.dao.AudioChunkDao
import com.example.twinmindassignment.core.database.dao.MeetingSessionDao
import com.example.twinmindassignment.core.database.entity.AudioChunkEntity
import com.example.twinmindassignment.core.database.entity.MeetingSessionEntity
import com.example.twinmindassignment.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RecordingRepositoryImpl @Inject constructor(
    private val meetingSessionDao: MeetingSessionDao,
    private val audioChunkDao: AudioChunkDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : RecordingRepository {

    override fun getAllSessions(): Flow<List<MeetingSessionEntity>> =
        meetingSessionDao.getAllSessions()

    override fun getSessionById(id: String): Flow<MeetingSessionEntity?> =
        meetingSessionDao.getSessionById(id)

    override suspend fun getSessionByIdOnce(id: String): MeetingSessionEntity? =
        withContext(ioDispatcher) { meetingSessionDao.getSessionByIdOnce(id) }

    override suspend fun getActiveRecordingSession(): MeetingSessionEntity? =
        withContext(ioDispatcher) { meetingSessionDao.getActiveRecordingSession() }

    override suspend fun createSession(session: MeetingSessionEntity) =
        withContext(ioDispatcher) { meetingSessionDao.insertSession(session) }

    override suspend fun updateSession(session: MeetingSessionEntity) =
        withContext(ioDispatcher) { meetingSessionDao.updateSession(session) }

    override suspend fun completeSession(id: String, endTime: Long, duration: Long, status: String) =
        withContext(ioDispatcher) {
            meetingSessionDao.completeSession(id, endTime, duration, status, System.currentTimeMillis())
        }

    override suspend fun updateSessionStatus(id: String, status: String) =
        withContext(ioDispatcher) {
            meetingSessionDao.updateStatus(id, status, System.currentTimeMillis())
        }

    override suspend fun saveChunk(chunk: AudioChunkEntity) =
        withContext(ioDispatcher) { audioChunkDao.insertChunk(chunk) }

    override suspend fun incrementChunkCount(sessionId: String) =
        withContext(ioDispatcher) {
            meetingSessionDao.incrementChunkCount(sessionId, System.currentTimeMillis())
        }

    override suspend fun deleteSession(session: MeetingSessionEntity) =
        withContext(ioDispatcher) { meetingSessionDao.deleteSession(session) }

    override fun getChunksForSession(sessionId: String): Flow<List<AudioChunkEntity>> =
        audioChunkDao.getChunksForSession(sessionId)
}
