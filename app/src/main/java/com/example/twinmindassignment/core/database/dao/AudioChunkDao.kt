package com.example.twinmindassignment.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.twinmindassignment.core.database.entity.AudioChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {

    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId ORDER BY chunkIndex ASC")
    fun getChunksForSession(sessionId: String): Flow<List<AudioChunkEntity>>

    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId AND status = 'SAVED' ORDER BY chunkIndex ASC")
    suspend fun getUntranscribedChunks(sessionId: String): List<AudioChunkEntity>

    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId AND status = 'FAILED' ORDER BY chunkIndex ASC")
    suspend fun getFailedChunks(sessionId: String): List<AudioChunkEntity>

    @Query("SELECT * FROM audio_chunks WHERE id = :id")
    suspend fun getChunkById(id: String): AudioChunkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: AudioChunkEntity)

    @Update
    suspend fun updateChunk(chunk: AudioChunkEntity)

    @Query("UPDATE audio_chunks SET status = :status WHERE id = :id")
    suspend fun updateChunkStatus(id: String, status: String)

    @Query("SELECT COUNT(*) FROM audio_chunks WHERE sessionId = :sessionId")
    suspend fun getChunkCount(sessionId: String): Int

    @Query("SELECT SUM(fileSizeBytes) FROM audio_chunks WHERE sessionId = :sessionId")
    suspend fun getTotalFileSize(sessionId: String): Long?
}
