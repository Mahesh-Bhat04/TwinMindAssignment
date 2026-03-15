package com.example.twinmindassignment.core.network.dto

import com.google.gson.annotations.SerializedName

data class SummaryRequest(
    @SerializedName("model") val model: String = "gpt-4o-mini",
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("stream") val stream: Boolean = true,
    @SerializedName("temperature") val temperature: Double = 0.3
) {
    data class Message(
        @SerializedName("role") val role: String,
        @SerializedName("content") val content: String
    )
}
