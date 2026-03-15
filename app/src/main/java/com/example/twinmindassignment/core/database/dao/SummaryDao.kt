package com.example.twinmindassignment.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.twinmindassignment.core.database.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    fun getSummaryForSession(sessionId: String): Flow<SummaryEntity?>

    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    suspend fun getSummaryForSessionOnce(sessionId: String): SummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity)

    @Update
    suspend fun updateSummary(summary: SummaryEntity)

    @Query("UPDATE summaries SET status = :status, errorMessage = :error, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String?, now: Long)
}
