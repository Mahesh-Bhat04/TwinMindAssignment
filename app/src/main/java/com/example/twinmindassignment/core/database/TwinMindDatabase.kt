package com.example.twinmindassignment.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.twinmindassignment.core.database.converter.Converters
import com.example.twinmindassignment.core.database.dao.AudioChunkDao
import com.example.twinmindassignment.core.database.dao.MeetingSessionDao
import com.example.twinmindassignment.core.database.dao.SummaryDao
import com.example.twinmindassignment.core.database.dao.TranscriptSegmentDao
import com.example.twinmindassignment.core.database.entity.AudioChunkEntity
import com.example.twinmindassignment.core.database.entity.MeetingSessionEntity
import com.example.twinmindassignment.core.database.entity.SummaryEntity
import com.example.twinmindassignment.core.database.entity.TranscriptSegmentEntity

@Database(
    entities = [
        MeetingSessionEntity::class,
        AudioChunkEntity::class,
        TranscriptSegmentEntity::class,
        SummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TwinMindDatabase : RoomDatabase() {
    abstract fun meetingSessionDao(): MeetingSessionDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptSegmentDao(): TranscriptSegmentDao
    abstract fun summaryDao(): SummaryDao
}
