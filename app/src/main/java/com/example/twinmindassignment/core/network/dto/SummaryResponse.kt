package com.example.twinmindassignment.core.network.dto

import com.google.gson.annotations.SerializedName

data class StreamChunk(
    @SerializedName("id") val id: String? = null,
    @SerializedName("choices") val choices: List<StreamChoice>? = null
)

data class StreamChoice(
    @SerializedName("delta") val delta: StreamDelta? = null,
    @SerializedName("finish_reason") val finishReason: String? = null
)

data class StreamDelta(
    @SerializedName("content") val content: String? = null,
    @SerializedName("role") val role: String? = null
)

data class ChatCompletionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("choices") val choices: List<ChatChoice>
)

data class ChatChoice(
    @SerializedName("message") val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)
