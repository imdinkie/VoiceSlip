package com.example.voiceslip.net

import org.junit.Assert.assertEquals
import org.junit.Test

class MistralTranscriptionClientTest {
    @Test
    fun mistralContextBiasTermsSplitsWhitespaceAndCommas() {
        assertEquals(
            listOf("Justin", "Dankert", "foo-bar", "v2.5"),
            mistralContextBiasTerms(listOf("Justin Dankert", "foo-bar, v2.5"))
        )
    }

    @Test
    fun mistralContextBiasTermsDropsEmptyAndDeduplicates() {
        assertEquals(
            listOf("Justin", "Dankert"),
            mistralContextBiasTerms(listOf("Justin  Dankert", "Justin", "", " Dankert "))
        )
    }
}
