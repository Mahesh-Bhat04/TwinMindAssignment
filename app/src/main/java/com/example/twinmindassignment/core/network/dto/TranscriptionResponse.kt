package com.example.twinmindassignment.core.network.dto

import com.google.gson.annotations.SerializedName

data class TranscriptionResponse(
    @SerializedName("text") val text: String,
    @SerializedName("language") val language: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("segments") val segments: List<TranscriptionSegment>? = null
)

data class TranscriptionSegment(
    @SerializedName("id") val id: Int,
    @SerializedName("start") val start: Double,
    @SerializedName("end") val end: Double,
    @SerializedName("text") val text: String
)
