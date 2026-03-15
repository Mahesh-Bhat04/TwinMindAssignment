package com.example.twinmindassignment.summary.worker

import android.content.Context
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
import com.example.twinmindassignment.summary.repository.SummaryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val summaryRepository: SummaryRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString("session_id") ?: return Result.failure()

        return try {
            summaryRepository.generateSummaryBlocking(sessionId)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < Constants.MAX_SUMMARY_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        fun enqueue(context: Context, sessionId: String) {
            val request = OneTimeWorkRequestBuilder<SummaryWorker>()
                .setInputData(workDataOf("session_id" to sessionId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("summary")
                .addTag("session_$sessionId")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "summary_$sessionId",
                    ExistingWorkPolicy.KEEP,
                    request
                )
        }
    }
}
