package com.example.twinmindassignment.recording.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import com.example.twinmindassignment.core.database.entity.MeetingSessionEntity
import com.example.twinmindassignment.core.model.ChunkStatus
import com.example.twinmindassignment.core.model.ServiceState
import com.example.twinmindassignment.core.model.SessionStatus
import com.example.twinmindassignment.core.model.SilenceResult
import com.example.twinmindassignment.core.util.Constants
import com.example.twinmindassignment.core.util.DateTimeUtils
import com.example.twinmindassignment.recording.manager.AudioFocusManager
import com.example.twinmindassignment.recording.manager.ChunkManager
import com.example.twinmindassignment.recording.manager.StorageMonitor
import com.example.twinmindassignment.recording.receiver.HeadsetReceiver
import com.example.twinmindassignment.recording.receiver.PhoneCallReceiver
import com.example.twinmindassignment.recording.repository.RecordingRepository
import com.example.twinmindassignment.transcription.worker.TranscriptionWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class AudioRecordingService : Service() {

    @Inject lateinit var recordingRepository: RecordingRepository
    @Inject lateinit var audioFocusManager: AudioFocusManager
    @Inject lateinit var chunkManager: ChunkManager
    @Inject lateinit var storageMonitor: StorageMonitor
    @Inject lateinit var notificationManager: RecordingNotificationManager

    private var audioRecord: AudioRecord? = null
    private var currentSessionId: String? = null
    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var isPaused = false
    private var pauseReason: String? = null
    private var sessionStartTimeMs = 0L
    private var elapsedAtPauseMs = 0L
    private var pauseStartMs = 0L
    private var totalPausedMs = 0L
    private var silenceWarningShown = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val phoneCallReceiver = PhoneCallReceiver(
        onCallStarted = { pauseRecording("Phone call") },
        onCallEnded = { resumeRecording() }
    )

    private val headsetReceiver = HeadsetReceiver(
        onHeadsetStateChanged = { connected ->
            Log.d(TAG, "Headset ${if (connected) "connected" else "disconnected"}, continuing recording")
            updateNotification()
        }
    )

    companion object {
        const val TAG = "AudioRecordingService"
        const val ACTION_START = "ACTION_START_RECORDING"
        const val ACTION_STOP = "ACTION_STOP_RECORDING"
        const val ACTION_PAUSE = "ACTION_PAUSE_RECORDING"
        const val ACTION_RESUME = "ACTION_RESUME_RECORDING"

        private val _serviceState = MutableStateFlow<ServiceState>(ServiceState.Idle)
        val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

        private val _silenceWarning = MutableStateFlow(false)
        val silenceWarning: StateFlow<Boolean> = _silenceWarning.asStateFlow()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
            ACTION_PAUSE -> pauseRecording("Manual pause")
            ACTION_RESUME -> resumeRecording()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        if (currentSessionId != null) return

        // Check storage
        if (storageMonitor.isStorageLow()) {
            _serviceState.value = ServiceState.Error("Recording stopped - Low storage")
            stopSelf()
            return
        }

        // Create notification channel and start foreground
        notificationManager.createNotificationChannel()
        val notification = notificationManager.buildNotification("00:00:00", false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Constants.RECORDING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(Constants.RECORDING_NOTIFICATION_ID, notification)
        }

        // Create session
        val sessionId = UUID.randomUUID().toString()
        currentSessionId = sessionId
        val now = System.currentTimeMillis()
        val title = "Meeting ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(now))}"

        serviceScope.launch(Dispatchers.IO) {
            recordingRepository.createSession(
                MeetingSessionEntity(
                    id = sessionId,
                    title = title,
                    startTime = now,
                    status = SessionStatus.RECORDING.name,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        // Request audio focus
        audioFocusManager.requestFocus(
            onFocusLost = { pauseRecording("Audio focus lost") },
            onFocusGainedBack = { resumeRecording() }
        )

        // Register receivers
        registerReceiver(
            phoneCallReceiver,
            IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        )
        registerReceiver(
            headsetReceiver,
            IntentFilter(Intent.ACTION_HEADSET_PLUG)
        )

        // Init AudioRecord
        val bufferSize = AudioRecord.getMinBufferSize(
            Constants.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            Constants.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2
        )

        sessionStartTimeMs = System.currentTimeMillis()
        totalPausedMs = 0L
        isPaused = false
        chunkManager.reset()

        // Start recording
        audioRecord?.startRecording()
        chunkManager.startNewChunk(sessionId, sessionStartTimeMs, 0L)

        _serviceState.value = ServiceState.Recording(
            sessionId = sessionId,
            elapsedMs = 0,
            chunkIndex = 0,
            isPaused = false
        )

        startRecordingLoop(sessionId, bufferSize)
        startTimerLoop(sessionId)
    }

    private fun startRecordingLoop(sessionId: String, bufferSize: Int) {
        recordingJob = serviceScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(bufferSize)
            while (isActive) {
                if (isPaused) {
                    delay(100)
                    continue
                }

                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (bytesRead <= 0) {
                    delay(10)
                    continue
                }

                // Check storage before writing
                if (storageMonitor.isStorageLow()) {
                    launch(Dispatchers.Main) {
                        stopRecordingWithError("Recording stopped - Low storage")
                    }
                    break
                }

                val elapsed = getElapsedMs()
                val result = chunkManager.writeData(buffer, bytesRead, sessionId, sessionStartTimeMs, elapsed)

                // Handle silence
                when (result.silenceResult) {
                    is SilenceResult.SilenceWarning -> {
                        if (!silenceWarningShown) {
                            silenceWarningShown = true
                            _silenceWarning.value = true
                        }
                    }
                    is SilenceResult.AudioDetected -> {
                        if (silenceWarningShown) {
                            silenceWarningShown = false
                            _silenceWarning.value = false
                        }
                    }
                    else -> {}
                }

                // Handle completed chunk
                if (result.chunkComplete && result.completedChunk != null) {
                    val chunk = result.completedChunk
                    recordingRepository.saveChunk(chunk)
                    recordingRepository.incrementChunkCount(sessionId)

                    // Enqueue transcription
                    TranscriptionWorker.enqueue(this@AudioRecordingService, chunk.id, sessionId)

                    // Start new chunk
                    chunkManager.startNewChunk(sessionId, sessionStartTimeMs, elapsed)
                }
            }
        }
    }

    private fun startTimerLoop(sessionId: String) {
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = getElapsedMs()
                _serviceState.value = ServiceState.Recording(
                    sessionId = sessionId,
                    elapsedMs = elapsed,
                    chunkIndex = chunkManager.chunkIndex,
                    isPaused = isPaused,
                    pauseReason = if (isPaused) pauseReason else null
                )
                updateNotification()
            }
        }
    }

    private fun pauseRecording(reason: String) {
        if (isPaused) return
        isPaused = true
        pauseReason = reason
        pauseStartMs = System.currentTimeMillis()
        audioRecord?.stop()
        Log.d(TAG, "Recording paused: $reason")
        updateNotification()
        val sessionId = currentSessionId ?: return
        _serviceState.value = ServiceState.Recording(
            sessionId = sessionId,
            elapsedMs = getElapsedMs(),
            chunkIndex = chunkManager.chunkIndex,
            isPaused = true,
            pauseReason = reason
        )
    }

    private fun resumeRecording() {
        if (!isPaused) return
        totalPausedMs += System.currentTimeMillis() - pauseStartMs
        isPaused = false
        pauseReason = null
        audioRecord?.startRecording()
        Log.d(TAG, "Recording resumed")
        updateNotification()
        val sessionId = currentSessionId ?: return
        _serviceState.value = ServiceState.Recording(
            sessionId = sessionId,
            elapsedMs = getElapsedMs(),
            chunkIndex = chunkManager.chunkIndex,
            isPaused = false
        )
    }

    private fun stopRecording() {
        val sessionId = currentSessionId ?: return
        currentSessionId = null

        recordingJob?.cancel()
        timerJob?.cancel()

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        // Finalize last chunk
        val elapsed = getElapsedMs()
        val lastChunk = chunkManager.finalizeLastChunk(sessionId, elapsed)

        serviceScope.launch(Dispatchers.IO) {
            if (lastChunk != null) {
                recordingRepository.saveChunk(lastChunk)
                recordingRepository.incrementChunkCount(sessionId)
                TranscriptionWorker.enqueue(this@AudioRecordingService, lastChunk.id, sessionId)
            }

            recordingRepository.completeSession(
                id = sessionId,
                endTime = System.currentTimeMillis(),
                duration = elapsed,
                status = SessionStatus.COMPLETED.name
            )
        }

        // Cleanup
        audioFocusManager.abandonFocus()
        try { unregisterReceiver(phoneCallReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(headsetReceiver) } catch (_: Exception) {}

        _serviceState.value = ServiceState.Idle
        _silenceWarning.value = false

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopRecordingWithError(errorMessage: String) {
        val sessionId = currentSessionId ?: return
        _serviceState.value = ServiceState.Error(errorMessage)

        serviceScope.launch(Dispatchers.IO) {
            recordingRepository.updateSession(
                MeetingSessionEntity(
                    id = sessionId,
                    title = "",
                    startTime = sessionStartTimeMs,
                    endTime = System.currentTimeMillis(),
                    status = SessionStatus.ERROR.name,
                    errorMessage = errorMessage,
                    totalDurationMs = getElapsedMs(),
                    createdAt = sessionStartTimeMs,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        stopRecording()
    }

    private fun getElapsedMs(): Long {
        if (isPaused) {
            return System.currentTimeMillis() - sessionStartTimeMs - totalPausedMs - (System.currentTimeMillis() - pauseStartMs)
        }
        return System.currentTimeMillis() - sessionStartTimeMs - totalPausedMs
    }

    private fun updateNotification() {
        val elapsed = getElapsedMs()
        val notification = notificationManager.buildNotification(
            DateTimeUtils.formatElapsedTime(elapsed),
            isPaused,
            pauseReason
        )
        notificationManager.updateNotification(notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        recordingJob?.cancel()
        timerJob?.cancel()
        audioRecord?.release()
        try { unregisterReceiver(phoneCallReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(headsetReceiver) } catch (_: Exception) {}
        audioFocusManager.abandonFocus()
        _serviceState.value = ServiceState.Idle
    }
}
