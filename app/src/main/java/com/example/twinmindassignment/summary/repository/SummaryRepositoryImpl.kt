package com.example.twinmindassignment.summary.repository

import com.example.twinmindassignment.core.database.dao.MeetingSessionDao
import com.example.twinmindassignment.core.database.dao.SummaryDao
import com.example.twinmindassignment.core.database.dao.TranscriptSegmentDao
import com.example.twinmindassignment.core.database.entity.SummaryEntity
import com.example.twinmindassignment.core.di.IoDispatcher
import com.example.twinmindassignment.core.model.SessionStatus
import com.example.twinmindassignment.core.model.SummaryProgress
import com.example.twinmindassignment.core.model.SummaryStatus
import com.example.twinmindassignment.core.network.api.SummaryApiService
import com.example.twinmindassignment.core.network.dto.StreamChunk
import com.example.twinmindassignment.core.network.dto.SummaryRequest
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject

class SummaryRepositoryImpl @Inject constructor(
    private val summaryApi: SummaryApiService,
    private val summaryDao: SummaryDao,
    private val transcriptSegmentDao: TranscriptSegmentDao,
    private val meetingSessionDao: MeetingSessionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val gson: Gson
) : SummaryRepository {

    override fun generateSummaryStream(sessionId: String): Flow<SummaryProgress> = flow {
        val transcript = transcriptSegmentDao.getFullTranscriptText(sessionId)
        if (transcript.isNullOrBlank()) {
            emit(SummaryProgress.Error("No transcript available"))
            return@flow
        }

        val summaryId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // Check if summary already exists and is completed
        val existing = summaryDao.getSummaryForSessionOnce(sessionId)
        if (existing?.status == SummaryStatus.COMPLETED.name) {
            val parsed = SummaryParser.parse(existing.rawResponse ?: "")
            emit(SummaryProgress.Completed(parsed))
            return@flow
        }

        // Create or update summary entity
        summaryDao.insertSummary(
            SummaryEntity(
                id = existing?.id ?: summaryId,
                sessionId = sessionId,
                status = SummaryStatus.GENERATING.name,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        val activeId = existing?.id ?: summaryId

        emit(SummaryProgress.Started)

        try {
            val request = SummaryRequest(
                messages = listOf(
                    SummaryRequest.Message("system", SYSTEM_PROMPT),
                    SummaryRequest.Message("user", "Transcript:\n\n$transcript")
                )
            )

            val responseBody = summaryApi.generateSummaryStream(request)
            val fullResponse = StringBuilder()

            val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
            reader.use {
                var line = it.readLine()
                while (line != null) {
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        try {
                            val chunk = gson.fromJson(data, StreamChunk::class.java)
                            val content = chunk.choices?.firstOrNull()?.delta?.content
                            if (content != null) {
                                fullResponse.append(content)
                                emit(SummaryProgress.Streaming(fullResponse.toString()))
                            }
                        } catch (_: Exception) {
                            // Skip malformed chunks
                        }
                    }
                    line = it.readLine()
                }
            }

            val parsed = SummaryParser.parse(fullResponse.toString())

            summaryDao.updateSummary(
                SummaryEntity(
                    id = activeId,
                    sessionId = sessionId,
                    title = parsed.title,
                    summary = parsed.summary,
                    actionItems = gson.toJson(parsed.actionItems),
                    keyPoints = gson.toJson(parsed.keyPoints),
                    status = SummaryStatus.COMPLETED.name,
                    rawResponse = fullResponse.toString(),
                    retryCount = 0,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Update session title and status
            parsed.title?.let {
                meetingSessionDao.updateTitle(sessionId, it, System.currentTimeMillis())
            }
            meetingSessionDao.updateStatus(sessionId, SessionStatus.DONE.name, System.currentTimeMillis())

            emit(SummaryProgress.Completed(parsed))
        } catch (e: Exception) {
            summaryDao.updateStatus(
                activeId,
                SummaryStatus.FAILED.name,
                e.message,
                System.currentTimeMillis()
            )
            emit(SummaryProgress.Error(e.message ?: "Failed to generate summary"))
        }
    }.flowOn(ioDispatcher)

    override suspend fun generateSummaryBlocking(sessionId: String) {
        val transcript = transcriptSegmentDao.getFullTranscriptText(sessionId)
        if (transcript.isNullOrBlank()) return

        val existing = summaryDao.getSummaryForSessionOnce(sessionId)
        if (existing?.status == SummaryStatus.COMPLETED.name) return

        val summaryId = existing?.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        summaryDao.insertSummary(
            SummaryEntity(
                id = summaryId,
                sessionId = sessionId,
                status = SummaryStatus.GENERATING.name,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )

        try {
            val request = SummaryRequest(
                messages = listOf(
                    SummaryRequest.Message("system", SYSTEM_PROMPT),
                    SummaryRequest.Message("user", "Transcript:\n\n$transcript")
                ),
                stream = false
            )

            val response = summaryApi.generateSummary(request)
            val content = response.choices.firstOrNull()?.message?.content ?: ""
            val parsed = SummaryParser.parse(content)

            summaryDao.updateSummary(
                SummaryEntity(
                    id = summaryId,
                    sessionId = sessionId,
                    title = parsed.title,
                    summary = parsed.summary,
                    actionItems = gson.toJson(parsed.actionItems),
                    keyPoints = gson.toJson(parsed.keyPoints),
                    status = SummaryStatus.COMPLETED.name,
                    rawResponse = content,
                    retryCount = 0,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = System.currentTimeMillis()
                )
            )

            parsed.title?.let {
                meetingSessionDao.updateTitle(sessionId, it, System.currentTimeMillis())
            }
            meetingSessionDao.updateStatus(sessionId, SessionStatus.DONE.name, System.currentTimeMillis())
        } catch (e: Exception) {
            summaryDao.updateStatus(
                summaryId,
                SummaryStatus.FAILED.name,
                e.message,
                System.currentTimeMillis()
            )
        }
    }

    override fun getSummaryForSession(sessionId: String): Flow<SummaryEntity?> =
        summaryDao.getSummaryForSession(sessionId)

    companion object {
        const val SYSTEM_PROMPT = """You are a meeting summarizer. Given a transcript, produce exactly these 4 sections in markdown:

## Title
A concise title for this meeting (one line).

## Summary
A 2-3 paragraph summary of the meeting content.

## Action Items
- Bulleted list of action items discussed

## Key Points
- Bulleted list of key points and decisions made

Be concise and accurate. Only include information present in the transcript."""
    }
}
