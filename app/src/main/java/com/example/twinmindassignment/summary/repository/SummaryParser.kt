package com.example.twinmindassignment.summary.repository

import com.example.twinmindassignment.core.model.ParsedSummary

object SummaryParser {

    fun parse(raw: String): ParsedSummary {
        val title = extractSection(raw, "Title")?.trim()
        val summary = extractSection(raw, "Summary")?.trim()
        val actionItems = extractBulletList(raw, "Action Items")
        val keyPoints = extractBulletList(raw, "Key Points")

        return ParsedSummary(
            title = title,
            summary = summary,
            actionItems = actionItems,
            keyPoints = keyPoints
        )
    }

    private fun extractSection(text: String, sectionName: String): String? {
        val pattern = Regex(
            """##\s*$sectionName\s*\n(.*?)(?=\n##\s|\z)""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        return pattern.find(text)?.groupValues?.get(1)?.trim()
    }

    private fun extractBulletList(text: String, sectionName: String): List<String> {
        val sectionContent = extractSection(text, sectionName) ?: return emptyList()
        return sectionContent
            .lines()
            .map { it.trim() }
            .filter { it.startsWith("-") || it.startsWith("*") || it.matches(Regex("""\d+\..*""")) }
            .map { it.removePrefix("-").removePrefix("*").replaceFirst(Regex("""\d+\.\s*"""), "").trim() }
            .filter { it.isNotBlank() }
    }
}
