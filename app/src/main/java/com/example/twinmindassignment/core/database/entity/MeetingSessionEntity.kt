package com.example.twinmindassignment.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meeting_sessions")
data class MeetingSessionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val status: String,
    val totalDurationMs: Long = 0L,
    val chunkCount: Int = 0,
    val transcribedChunkCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
