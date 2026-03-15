package com.example.twinmindassignment.summary.repository

import com.example.twinmindassignment.core.database.entity.SummaryEntity
import com.example.twinmindassignment.core.model.SummaryProgress
import kotlinx.coroutines.flow.Flow

interface SummaryRepository {
    fun generateSummaryStream(sessionId: String): Flow<SummaryProgress>
    suspend fun generateSummaryBlocking(sessionId: String)
    fun getSummaryForSession(sessionId: String): Flow<SummaryEntity?>
}
