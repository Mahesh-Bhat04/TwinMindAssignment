package com.example.twinmindassignment.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.twinmindassignment.core.database.entity.SummaryEntity
import com.example.twinmindassignment.core.database.entity.TranscriptSegmentEntity
import com.example.twinmindassignment.core.model.SummaryProgress
import com.example.twinmindassignment.core.model.SummaryStatus
import com.example.twinmindassignment.core.util.DateTimeUtils
import com.example.twinmindassignment.ui.components.ErrorBanner
import com.example.twinmindassignment.ui.components.SectionCard
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    viewModel: MeetingDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val transcript by viewModel.transcript.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val summaryProgress by viewModel.summaryProgress.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = session?.title ?: "Meeting",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (summary == null && summaryProgress !is SummaryProgress.Started &&
                        summaryProgress !is SummaryProgress.Streaming && transcript.isNotEmpty()
                    ) {
                        TextButton(onClick = { viewModel.generateSummary() }) {
                            Text("Summarize")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Transcript") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Summary") }
                )
            }

            when (selectedTab) {
                0 -> TranscriptTab(
                    segments = transcript,
                    onRetryFailed = { viewModel.retryFailedTranscriptions() }
                )
                1 -> SummaryTab(
                    summary = summary,
                    progress = summaryProgress,
                    onGenerate = { viewModel.generateSummary() },
                    hasTranscript = transcript.isNotEmpty()
                )
            }
        }
    }
}

@Composable
private fun TranscriptTab(
    segments: List<TranscriptSegmentEntity>,
    onRetryFailed: () -> Unit
) {
    if (segments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No transcript yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Transcript will appear as audio is processed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(segments, key = { it.id }) { segment ->
                Column {
                    Text(
                        text = DateTimeUtils.formatElapsedTime(segment.startTimeMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = segment.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTab(
    summary: SummaryEntity?,
    progress: SummaryProgress?,
    onGenerate: () -> Unit,
    hasTranscript: Boolean
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Generating state
        if (progress is SummaryProgress.Started) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Generating summary...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Streaming text
        if (progress is SummaryProgress.Streaming) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = "Generating...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = progress.partialText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Completed summary in 4 sections
        if (summary?.status == SummaryStatus.COMPLETED.name || progress is SummaryProgress.Completed) {
            val displaySummary = summary
            if (displaySummary != null) {
                if (!displaySummary.title.isNullOrBlank()) {
                    item {
                        SectionCard(title = "Title", content = displaySummary.title)
                    }
                }
                if (!displaySummary.summary.isNullOrBlank()) {
                    item {
                        SectionCard(title = "Summary", content = displaySummary.summary)
                    }
                }
                val actionItems = parseJsonList(displaySummary.actionItems)
                if (actionItems.isNotEmpty()) {
                    item {
                        SectionCard(title = "Action Items", items = actionItems)
                    }
                }
                val keyPoints = parseJsonList(displaySummary.keyPoints)
                if (keyPoints.isNotEmpty()) {
                    item {
                        SectionCard(title = "Key Points", items = keyPoints)
                    }
                }
            }
        }

        // Error state
        if (progress is SummaryProgress.Error || summary?.status == SummaryStatus.FAILED.name) {
            item {
                ErrorBanner(
                    message = (progress as? SummaryProgress.Error)?.message
                        ?: summary?.errorMessage
                        ?: "Failed to generate summary",
                    onRetry = onGenerate
                )
            }
        }

        // No summary yet, show generate button
        if (summary == null && progress == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (hasTranscript) "Ready to generate summary"
                            else "Waiting for transcript...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hasTranscript) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onGenerate) {
                                Text("Generate Summary")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseJsonList(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val type = object : TypeToken<List<String>>() {}.type
        Gson().fromJson(json, type)
    } catch (_: Exception) {
        emptyList()
    }
}
