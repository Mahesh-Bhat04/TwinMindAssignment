package com.example.twinmindassignment.ui.dashboard

import android.content.Context
import android.content.Intent
import com.example.twinmindassignment.core.database.entity.MeetingSessionEntity
import com.example.twinmindassignment.core.model.ServiceState
import com.example.twinmindassignment.recording.repository.RecordingRepository
import com.example.twinmindassignment.recording.service.AudioRecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val sessions: StateFlow<List<MeetingSessionEntity>> =
        recordingRepository.getAllSessions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordingState: StateFlow<ServiceState> = AudioRecordingService.serviceState

    fun startRecording() {
        val intent = Intent(context, AudioRecordingService::class.java).apply {
            action = AudioRecordingService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stopRecording() {
        val intent = Intent(context, AudioRecordingService::class.java).apply {
            action = AudioRecordingService.ACTION_STOP
        }
        context.startService(intent)
    }
}
