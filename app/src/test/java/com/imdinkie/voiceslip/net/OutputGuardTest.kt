package com.imdinkie.voiceslip.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutputGuardTest {
    @Test
    fun longSingleParagraphIsNotRejectedAsRepeatedLines() {
        val paragraph = (1..45).joinToString(" ") {
            "Sentence $it describes a normal dictated thought with enough distinct wording to avoid any loop."
        }

        assertTrue(paragraph.length > 800)
        assertNull(outputGuardRejection(paragraph, durationMillis = 212_000L))
    }

    @Test
    fun repeatedEligibleLinesAreRejected() {
        val line = "This exact generated line is repeated often enough to be pathological."
        val text = List(12) { line }.joinToString("\n")

        assertEquals(
            "Rejected model output because it repeated the same line excessively.",
            outputGuardRejection(text, durationMillis = 60_000L)
        )
    }

    @Test
    fun extremeConsecutivePhraseLoopIsRejected() {
        val phrase = "repeat this phrase now"
        val text = List(40) { phrase }.joinToString(" ")

        assertEquals(
            "Rejected model output because it contains extreme repeated phrasing.",
            outputGuardRejection(text, durationMillis = 60_000L)
        )
    }

    @Test
    fun wildlyTooLongOutputReportsDurationLengthMessage() {
        val text = (1..9_000).joinToString("") { ('a' + (it % 26)).toString() }

        assertEquals(
            "Rejected model output because 9000 characters is wildly too long for a 1s recording.",
            outputGuardRejection(text, durationMillis = 1_000L)
        )
    }
}
