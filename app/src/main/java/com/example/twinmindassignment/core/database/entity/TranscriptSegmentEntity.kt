package com.example.twinmindassignment.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcript_segments",
    foreignKeys = [
        ForeignKey(
            entity = MeetingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AudioChunkEntity::class,
            parentColumns = ["id"],
            childColumns = ["chunkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("chunkId")]
)
data class TranscriptSegmentEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val chunkId: String,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val orderIndex: Int,
    val confidence: Float? = null,
    val language: String? = null,
    val createdAt: Long
)
