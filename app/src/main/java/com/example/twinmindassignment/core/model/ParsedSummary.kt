package com.example.twinmindassignment.core.model

data class ParsedSummary(
    val title: String?,
    val summary: String?,
    val actionItems: List<String>,
    val keyPoints: List<String>
)
