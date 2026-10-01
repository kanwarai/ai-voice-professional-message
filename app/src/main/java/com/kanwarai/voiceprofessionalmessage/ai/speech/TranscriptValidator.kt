package com.kanwarai.voiceprofessionalmessage.ai.speech

object TranscriptValidator {
    private val noSpeechMarkers = setOf(
        "blank audio",
        "silence",
        "no speech",
        "music",
        "background noise",
        "inaudible",
    )

    fun normalize(raw: String): String? {
        val normalized = raw.trim().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) return null
        val marker = normalized
            .removeSurrounding("[", "]")
            .removeSurrounding("(", ")")
            .trim()
            .lowercase()
        return normalized.takeUnless { marker in noSpeechMarkers }
    }
}
