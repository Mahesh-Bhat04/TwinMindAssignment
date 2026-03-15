package com.example.twinmindassignment.core.model

sealed class ServiceState {
    data object Idle : ServiceState()
    data class Recording(
        val sessionId: String,
        val elapsedMs: Long,
        val chunkIndex: Int,
        val isPaused: Boolean,
        val pauseReason: String? = null
    ) : ServiceState()
    data class Error(val message: String) : ServiceState()
}
