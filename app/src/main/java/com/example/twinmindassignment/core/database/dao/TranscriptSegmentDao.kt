package com.example.twinmindassignment.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.twinmindassignment.core.database.entity.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptSegmentDao {

    @Query("SELECT * FROM transcript_segments WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getTranscriptForSession(sessionId: String): Flow<List<TranscriptSegmentEntity>>

    @Query("SELECT * FROM transcript_segments WHERE chunkId = :chunkId ORDER BY orderIndex ASC")
    suspend fun getSegmentsForChunk(chunkId: String): List<TranscriptSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: TranscriptSegmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<TranscriptSegmentEntity>)

    @Query("DELETE FROM transcript_segments WHERE chunkId = :chunkId")
    suspend fun deleteSegmentsForChunk(chunkId: String)

    @Query("SELECT MAX(orderIndex) FROM transcript_segments WHERE sessionId = :sessionId")
    suspend fun getMaxOrderIndex(sessionId: String): Int?

    @Query("SELECT GROUP_CONCAT(text, ' ') FROM transcript_segments WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getFullTranscriptText(sessionId: String): String?
}
