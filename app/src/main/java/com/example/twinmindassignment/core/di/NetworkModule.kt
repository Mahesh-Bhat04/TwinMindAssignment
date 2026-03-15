package com.example.twinmindassignment.core.di

import com.example.twinmindassignment.core.network.api.SummaryApiService
import com.example.twinmindassignment.core.network.api.TranscriptionApiService
import com.example.twinmindassignment.core.network.interceptor.ApiKeyInterceptor
import com.example.twinmindassignment.core.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiKeyInterceptor())
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.OPENAI_BASE_URL)
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideTranscriptionApi(retrofit: Retrofit): TranscriptionApiService =
        retrofit.create(TranscriptionApiService::class.java)

    @Provides
    @Singleton
    fun provideSummaryApi(retrofit: Retrofit): SummaryApiService =
        retrofit.create(SummaryApiService::class.java)
}
