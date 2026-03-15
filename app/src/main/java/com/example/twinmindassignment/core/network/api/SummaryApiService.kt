package com.example.twinmindassignment.core.network.api

import com.example.twinmindassignment.core.network.dto.ChatCompletionResponse
import com.example.twinmindassignment.core.network.dto.SummaryRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface SummaryApiService {

    @Streaming
    @POST("v1/chat/completions")
    suspend fun generateSummaryStream(
        @Body request: SummaryRequest
    ): ResponseBody

    @POST("v1/chat/completions")
    suspend fun generateSummary(
        @Body request: SummaryRequest
    ): ChatCompletionResponse
}
