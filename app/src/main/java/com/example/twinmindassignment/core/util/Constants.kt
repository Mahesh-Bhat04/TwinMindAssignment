package com.example.twinmindassignment.core.util

object Constants {
    // Audio Recording
    const val SAMPLE_RATE = 16000
    const val CHUNK_DURATION_MS = 30_000L
    const val OVERLAP_DURATION_MS = 2_000L
    const val BYTES_PER_SAMPLE = 2
    const val CHANNELS = 1

    // Overlap buffer size: SAMPLE_RATE * BYTES_PER_SAMPLE * (OVERLAP_DURATION_MS / 1000)
    const val OVERLAP_BUFFER_SIZE = SAMPLE_RATE * BYTES_PER_SAMPLE * 2 // 64,000 bytes

    // Silence Detection
    const val SILENCE_THRESHOLD_RMS = 200.0
    const val SILENCE_WARNING_MS = 10_000L

    // Storage
    const val MIN_STORAGE_BYTES = 50L * 1024 * 1024      // 50 MB
    const val WARNING_STORAGE_BYTES = 100L * 1024 * 1024  // 100 MB

    // Notifications
    const val RECORDING_NOTIFICATION_ID = 1001
    const val RECORDING_CHANNEL_ID = "recording_channel"
    const val RECORDING_CHANNEL_NAME = "Recording"

    // API
    const val OPENAI_BASE_URL = "https://api.openai.com/"
    const val WHISPER_MODEL = "whisper-1"
    const val SUMMARY_MODEL = "gpt-4o-mini"

    // WorkManager
    const val MAX_TRANSCRIPTION_RETRIES = 3
    const val MAX_SUMMARY_RETRIES = 3
}
