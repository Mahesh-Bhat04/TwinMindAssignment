package com.example.twinmindassignment.core.di

import android.content.Context
import androidx.room.Room
import com.example.twinmindassignment.core.database.TwinMindDatabase
import com.example.twinmindassignment.core.database.dao.AudioChunkDao
import com.example.twinmindassignment.core.database.dao.MeetingSessionDao
import com.example.twinmindassignment.core.database.dao.SummaryDao
import com.example.twinmindassignment.core.database.dao.TranscriptSegmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TwinMindDatabase =
        Room.databaseBuilder(context, TwinMindDatabase::class.java, "twinmind_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMeetingSessionDao(db: TwinMindDatabase): MeetingSessionDao = db.meetingSessionDao()

    @Provides
    fun provideAudioChunkDao(db: TwinMindDatabase): AudioChunkDao = db.audioChunkDao()

    @Provides
    fun provideTranscriptSegmentDao(db: TwinMindDatabase): TranscriptSegmentDao = db.transcriptSegmentDao()

    @Provides
    fun provideSummaryDao(db: TwinMindDatabase): SummaryDao = db.summaryDao()
}
