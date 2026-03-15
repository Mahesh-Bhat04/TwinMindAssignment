package com.example.twinmindassignment.core.di

import com.example.twinmindassignment.recording.repository.RecordingRepository
import com.example.twinmindassignment.recording.repository.RecordingRepositoryImpl
import com.example.twinmindassignment.summary.repository.SummaryRepository
import com.example.twinmindassignment.summary.repository.SummaryRepositoryImpl
import com.example.twinmindassignment.transcription.repository.TranscriptionRepository
import com.example.twinmindassignment.transcription.repository.TranscriptionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(impl: RecordingRepositoryImpl): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindTranscriptionRepository(impl: TranscriptionRepositoryImpl): TranscriptionRepository

    @Binds
    @Singleton
    abstract fun bindSummaryRepository(impl: SummaryRepositoryImpl): SummaryRepository
}
