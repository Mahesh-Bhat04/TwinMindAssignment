package com.example.twinmindassignment.core.model

sealed class SilenceResult {
    data class AudioDetected(val rms: Double) : SilenceResult()
    data class Silent(val durationMs: Long) : SilenceResult()
    data class SilenceWarning(val durationMs: Long) : SilenceResult()
}
