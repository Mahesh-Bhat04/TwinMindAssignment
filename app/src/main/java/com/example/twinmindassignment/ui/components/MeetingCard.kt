package com.example.twinmindassignment.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.twinmindassignment.core.database.entity.MeetingSessionEntity
import com.example.twinmindassignment.core.model.SessionStatus
import com.example.twinmindassignment.core.util.DateTimeUtils

@Composable
fun MeetingCard(
    session: MeetingSessionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = session.status)
            }

            Text(
                text = DateTimeUtils.formatDateTime(session.startTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (session.totalDurationMs > 0) {
                Text(
                    text = "Duration: ${DateTimeUtils.formatDuration(session.totalDurationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (session.chunkCount > 0) {
                Text(
                    text = "Transcribed: ${session.transcribedChunkCount}/${session.chunkCount} chunks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, color) = when (status) {
        SessionStatus.RECORDING.name -> "Recording" to MaterialTheme.colorScheme.error
        SessionStatus.COMPLETED.name -> "Completed" to MaterialTheme.colorScheme.primary
        SessionStatus.TRANSCRIBING.name -> "Transcribing" to MaterialTheme.colorScheme.secondary
        SessionStatus.SUMMARIZING.name -> "Summarizing" to MaterialTheme.colorScheme.secondary
        SessionStatus.DONE.name -> "Done" to MaterialTheme.colorScheme.tertiary
        SessionStatus.ERROR.name -> "Error" to MaterialTheme.colorScheme.error
        else -> status to MaterialTheme.colorScheme.outline
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}
