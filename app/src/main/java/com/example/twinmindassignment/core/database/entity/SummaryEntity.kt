package com.example.twinmindassignment.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    foreignKeys = [ForeignKey(
        entity = MeetingSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId", unique = true)]
)
data class SummaryEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val title: String? = null,
    val summary: String? = null,
    val actionItems: String? = null,
    val keyPoints: String? = null,
    val status: String,
    val errorMessage: String? = null,
    val rawResponse: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
