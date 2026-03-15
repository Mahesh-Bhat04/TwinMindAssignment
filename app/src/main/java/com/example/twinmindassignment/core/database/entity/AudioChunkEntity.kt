package com.example.twinmindassignment.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_chunks",
    foreignKeys = [ForeignKey(
        entity = MeetingSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class AudioChunkEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val chunkIndex: Int,
    val filePath: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val overlapMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val status: String,
    val retryCount: Int = 0,
    val isSilent: Boolean = false,
    val createdAt: Long
)
