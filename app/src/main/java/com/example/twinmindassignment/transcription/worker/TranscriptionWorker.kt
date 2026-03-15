package com.example.twinmindassignment.transcription.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.twinmindassignment.core.util.Constants
import com.example.twinmindassignment.transcription.repository.TranscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transcriptionRepository: TranscriptionRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "TranscriptionWorker"

        fun enqueue(context: Context, chunkId: String, sessionId: String) {
            Log.d(TAG, "Enqueuing transcription for chunk $chunkId (session $sessionId)")
            val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
                .setInputData(workDataOf("chunk_id" to chunkId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("transcription")
                .addTag("session_$sessionId")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "transcribe_$chunkId",
                    ExistingWorkPolicy.KEEP,
                    request
                )
        }
    }

    override suspend fun doWork(): Result {
        val chunkId = inputData.getString("chunk_id")
        if (chunkId == null) {
            Log.e(TAG, "No chunk_id in input data")
            return Result.failure()
        }

        Log.d(TAG, "Starting transcription work for chunk $chunkId (attempt ${runAttemptCount + 1}/${Constants.MAX_TRANSCRIPTION_RETRIES + 1})")

        val success = transcriptionRepository.transcribeChunk(chunkId)
        return if (success) {
            Log.d(TAG, "Transcription succeeded for chunk $chunkId")
            Result.success()
        } else {
            if (runAttemptCount < Constants.MAX_TRANSCRIPTION_RETRIES) {
                Log.w(TAG, "Transcription failed for chunk $chunkId, scheduling retry (attempt ${runAttemptCount + 1})")
                Result.retry()
            } else {
                Log.e(TAG, "Transcription permanently failed for chunk $chunkId after ${runAttemptCount + 1} attempts")
                Result.failure()
            }
        }
    }
}
