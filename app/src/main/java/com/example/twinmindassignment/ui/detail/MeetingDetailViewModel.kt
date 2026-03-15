package com.example.twinmindassignment.ui.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.twinmindassignment.core.database.entity.MeetingSessionEntity
import com.example.twinmindassignment.core.database.entity.SummaryEntity
import com.example.twinmindassignment.core.database.entity.TranscriptSegmentEntity
import com.example.twinmindassignment.core.model.SummaryProgress
import com.example.twinmindassignment.recording.repository.RecordingRepository
import com.example.twinmindassignment.summary.repository.SummaryRepository
import com.example.twinmindassignment.summary.worker.SummaryWorker
import com.example.twinmindassignment.transcription.repository.TranscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeetingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingRepository: RecordingRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val summaryRepository: SummaryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val sessionId: String = savedStateHandle["sessionId"]!!

    val session: StateFlow<MeetingSessionEntity?> =
        recordingRepository.getSessionById(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transcript: StateFlow<List<TranscriptSegmentEntity>> =
        transcriptionRepository.getTranscriptForSession(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<SummaryEntity?> =
        summaryRepository.getSummaryForSession(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _summaryProgress = MutableStateFlow<SummaryProgress?>(null)
    val summaryProgress: StateFlow<SummaryProgress?> = _summaryProgress.asStateFlow()

    fun generateSummary() {
        viewModelScope.launch {
            summaryRepository.generateSummaryStream(sessionId)
                .collect { progress ->
                    _summaryProgress.value = progress
                }
        }
        // Enqueue worker as safety net for app-kill
        SummaryWorker.enqueue(context, sessionId)
    }

    fun retryFailedTranscriptions() {
        viewModelScope.launch {
            transcriptionRepository.retryFailedChunks(sessionId)
        }
    }
}
