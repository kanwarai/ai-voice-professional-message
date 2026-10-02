package com.kanwarai.voiceprofessionalmessage.ai.rewrite

object RewriteOutputValidator {
    private val forbidden = listOf("<think", "</think", "<|", "|>", "[/inst]", "### assistant")
    private val numeric = Regex("(?i)(?:[$€£₹]\\s*)?\\b\\d[\\d,.:/-]*\\b")
    private val months = Regex("(?i)\\b(january|february|march|april|may|june|july|august|september|october|november|december)\\b")

    fun validate(transcript: String, output: String): String? {
        val result = output.trim()
        if (result.isEmpty() || result.length > 6_000) return null
        if (result.length > maxOf(500, transcript.length * 3)) return null
        val lower = result.lowercase()
        if (forbidden.any(lower::contains) || lower.startsWith("assistant:") || lower.startsWith("analysis:")) return null
        val sourceFacts = numeric.findAll(transcript).map { normalize(it.value) }.toSet()
        if (numeric.findAll(result).map { normalize(it.value) }.any { it !in sourceFacts }) return null
        val sourceMonths = months.findAll(transcript).map { it.value.lowercase() }.toSet()
        if (months.findAll(result).map { it.value.lowercase() }.any { it !in sourceMonths }) return null
        val lines = result.lines().map(String::trim).filter(String::isNotEmpty)
        if (lines.groupingBy { it.lowercase() }.eachCount().values.any { it >= 3 }) return null
        val words = lower.split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.size >= 12) {
            val repeats = words.windowed(4).groupingBy { it }.eachCount().values.maxOrNull() ?: 0
            if (repeats >= 4) return null
        }
        return result
    }

    private fun normalize(value: String) = value.lowercase().filterNot { it.isWhitespace() || it == ',' }
}
