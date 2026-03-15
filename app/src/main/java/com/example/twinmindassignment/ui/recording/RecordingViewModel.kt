package com.example.twinmindassignment.ui.recording

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.example.twinmindassignment.core.model.ServiceState
import com.example.twinmindassignment.recording.service.AudioRecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    val recordingState: StateFlow<ServiceState> = AudioRecordingService.serviceState

    val silenceWarning: StateFlow<Boolean> = AudioRecordingService.silenceWarning

    fun togglePause() {
        val state = recordingState.value
        val action = if (state is ServiceState.Recording && state.isPaused)
            AudioRecordingService.ACTION_RESUME
        else
            AudioRecordingService.ACTION_PAUSE

        context.startService(
            Intent(context, AudioRecordingService::class.java).apply {
                this.action = action
            }
        )
    }

    fun stopRecording() {
        context.startService(
            Intent(context, AudioRecordingService::class.java).apply {
                action = AudioRecordingService.ACTION_STOP
            }
        )
    }
}
