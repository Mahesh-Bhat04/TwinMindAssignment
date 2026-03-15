package com.example.twinmindassignment

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.twinmindassignment.recording.worker.SessionRecoveryWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TwinMindApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        enqueueSessionRecovery()
    }

    private fun enqueueSessionRecovery() {
        val request = OneTimeWorkRequestBuilder<SessionRecoveryWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "session_recovery",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
