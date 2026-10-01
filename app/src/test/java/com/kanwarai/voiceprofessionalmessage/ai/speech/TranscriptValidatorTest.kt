package com.kanwarai.voiceprofessionalmessage.ai.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranscriptValidatorTest {
    @Test
    fun trimsAndCollapsesWhitespace() {
        assertEquals("Please send the report.", TranscriptValidator.normalize("  Please  send\n the report. "))
    }

    @Test
    fun rejectsEmptyAndKnownNoSpeechMarkers() {
        assertNull(TranscriptValidator.normalize("   \n"))
        assertNull(TranscriptValidator.normalize("[BLANK AUDIO]"))
        assertNull(TranscriptValidator.normalize("(silence)"))
    }
}
