package com.example.twinmindassignment.recording.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.twinmindassignment.core.database.dao.AudioChunkDao
import com.example.twinmindassignment.core.database.dao.MeetingSessionDao
import com.example.twinmindassignment.core.model.SessionStatus
import com.example.twinmindassignment.transcription.worker.TranscriptionWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SessionRecoveryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val meetingSessionDao: MeetingSessionDao,
    private val audioChunkDao: AudioChunkDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val orphanedSession = meetingSessionDao.getActiveRecordingSession() ?: return Result.success()

        // Mark session as completed
        val now = System.currentTimeMillis()
        meetingSessionDao.completeSession(
            id = orphanedSession.id,
            endTime = now,
            duration = now - orphanedSession.startTime,
            status = SessionStatus.COMPLETED.name,
            now = now
        )

        // Enqueue transcription for untranscribed chunks
        val untranscribed = audioChunkDao.getUntranscribedChunks(orphanedSession.id)
        untranscribed.forEach { chunk ->
            TranscriptionWorker.enqueue(applicationContext, chunk.id, orphanedSession.id)
        }

        // Also retry failed chunks
        val failed = audioChunkDao.getFailedChunks(orphanedSession.id)
        failed.forEach { chunk ->
            TranscriptionWorker.enqueue(applicationContext, chunk.id, orphanedSession.id)
        }

        return Result.success()
    }
}
