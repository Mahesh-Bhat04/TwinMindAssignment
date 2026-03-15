package com.example.twinmindassignment.recording.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.example.twinmindassignment.MainActivity
import com.example.twinmindassignment.R
import com.example.twinmindassignment.core.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RecordingNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.RECORDING_CHANNEL_ID,
            Constants.RECORDING_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active recording notification"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(
        elapsedTime: String,
        isPaused: Boolean,
        pauseReason: String? = null
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            context, 0,
            Intent(context, AudioRecordingService::class.java).apply {
                action = AudioRecordingService.ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = PendingIntent.getService(
            context, 1,
            Intent(context, AudioRecordingService::class.java).apply {
                action = if (isPaused) AudioRecordingService.ACTION_RESUME
                else AudioRecordingService.ACTION_PAUSE
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when {
            isPaused && pauseReason != null -> "Paused - $pauseReason"
            isPaused -> "Paused"
            else -> "Recording..."
        }

        val builder = NotificationCompat.Builder(context, Constants.RECORDING_CHANNEL_ID)
            .setContentTitle(statusText)
            .setContentText(elapsedTime)
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                R.drawable.ic_stop,
                "Stop",
                stopIntent
            )
            .addAction(
                if (isPaused) R.drawable.ic_play else R.drawable.ic_pause,
                if (isPaused) "Resume" else "Pause",
                pauseResumeIntent
            )
            .setContentIntent(contentIntent)

        // Android 16+ Live Updates: promote notification to lock screen
        if (Build.VERSION.SDK_INT >= 36) {
            builder.extras.putBoolean("android.requestPromotedOngoing", true)
        }

        return builder.build()
    }

    fun updateNotification(notification: Notification) {
        notificationManager.notify(Constants.RECORDING_NOTIFICATION_ID, notification)
    }

    fun buildSilenceWarningNotification(): Notification {
        return NotificationCompat.Builder(context, Constants.RECORDING_CHANNEL_ID)
            .setContentTitle("No audio detected")
            .setContentText("Check your microphone - no audio detected for 10 seconds")
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
