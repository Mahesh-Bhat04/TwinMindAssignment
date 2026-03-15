package com.example.twinmindassignment.core.network.interceptor

import com.example.twinmindassignment.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
            .header("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            .build()
        return chain.proceed(request)
    }
}
