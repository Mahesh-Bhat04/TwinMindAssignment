package com.example.twinmindassignment.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.twinmindassignment.core.database.entity.MeetingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingSessionDao {

    @Query("SELECT * FROM meeting_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<MeetingSessionEntity>>

    @Query("SELECT * FROM meeting_sessions WHERE id = :id")
    fun getSessionById(id: String): Flow<MeetingSessionEntity?>

    @Query("SELECT * FROM meeting_sessions WHERE id = :id")
    suspend fun getSessionByIdOnce(id: String): MeetingSessionEntity?

    @Query("SELECT * FROM meeting_sessions WHERE status = 'RECORDING'")
    suspend fun getActiveRecordingSession(): MeetingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MeetingSessionEntity)

    @Update
    suspend fun updateSession(session: MeetingSessionEntity)

    @Query("UPDATE meeting_sessions SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query("UPDATE meeting_sessions SET endTime = :endTime, totalDurationMs = :duration, status = :status, updatedAt = :now WHERE id = :id")
    suspend fun completeSession(id: String, endTime: Long, duration: Long, status: String, now: Long)

    @Query("UPDATE meeting_sessions SET chunkCount = chunkCount + 1, updatedAt = :now WHERE id = :id")
    suspend fun incrementChunkCount(id: String, now: Long)

    @Query("UPDATE meeting_sessions SET transcribedChunkCount = transcribedChunkCount + 1, updatedAt = :now WHERE id = :id")
    suspend fun incrementTranscribedCount(id: String, now: Long)

    @Query("UPDATE meeting_sessions SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, now: Long)

    @Delete
    suspend fun deleteSession(session: MeetingSessionEntity)
}
