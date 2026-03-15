package com.example.twinmindassignment.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.twinmindassignment.core.util.DateTimeUtils

@Composable
fun RecordingTimer(
    elapsedMs: Long,
    modifier: Modifier = Modifier
) {
    Text(
        text = DateTimeUtils.formatElapsedTime(elapsedMs),
        fontSize = 56.sp,
        fontWeight = FontWeight.Light,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}
