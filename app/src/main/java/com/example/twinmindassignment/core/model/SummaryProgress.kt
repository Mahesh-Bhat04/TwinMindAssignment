package com.example.twinmindassignment.core.model

sealed class SummaryProgress {
    data object Started : SummaryProgress()
    data class Streaming(val partialText: String) : SummaryProgress()
    data class Completed(val parsed: ParsedSummary) : SummaryProgress()
    data class Error(val message: String) : SummaryProgress()
}
