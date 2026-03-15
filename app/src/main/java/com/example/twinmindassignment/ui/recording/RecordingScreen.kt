package com.example.twinmindassignment.ui.recording

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.twinmindassignment.core.model.ServiceState
import com.example.twinmindassignment.ui.components.ErrorBanner
import com.example.twinmindassignment.ui.components.RecordingTimer
import com.example.twinmindassignment.ui.components.StatusIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (sessionId: String) -> Unit
) {
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val silenceWarning by viewModel.silenceWarning.collectAsStateWithLifecycle()

    // Navigate back when recording stops
    LaunchedEffect(recordingState) {
        if (recordingState is ServiceState.Idle) {
            onNavigateBack()
        }
    }

    val state = recordingState as? ServiceState.Recording

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status indicator
            StatusIndicator(
                isRecording = state != null,
                isPaused = state?.isPaused == true,
                pauseReason = state?.pauseReason
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Timer
            RecordingTimer(elapsedMs = state?.elapsedMs ?: 0)

            Spacer(modifier = Modifier.height(16.dp))

            // Chunk info
            if (state != null) {
                Text(
                    text = "Chunks: ${state.chunkIndex}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Silence warning
            if (silenceWarning) {
                ErrorBanner(
                    message = "No audio detected - Check microphone",
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause/Resume
                OutlinedButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (state?.isPaused == true) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (state?.isPaused == true) "Resume" else "Pause",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Stop
                Button(
                    onClick = {
                        val sessionId = state?.sessionId
                        viewModel.stopRecording()
                        if (sessionId != null) {
                            onNavigateToDetail(sessionId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop recording",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
