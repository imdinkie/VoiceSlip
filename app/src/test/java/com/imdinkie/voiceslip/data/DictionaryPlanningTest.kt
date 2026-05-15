package com.imdinkie.voiceslip.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryPlanningTest {
    @Test
    fun openRouterAudioTranscriptionSendsAllDictionaryEntriesWithoutInternalCap() {
        val terms = (1..150).map { "Term$it" }

        val plan = openRouterAudioDictionaryPlan(terms, enabled = true)

        assertTrue(plan.sent)
        assertEquals("OpenRouter audio prompt spelling constraints", plan.mechanism)
        assertEquals(150, plan.includedTerms)
        assertEquals(150, plan.totalTerms)
        assertNull(plan.limit)
        assertFalse(plan.truncated)
        assertTrue(plan.prompt!!.contains("Term150"))
    }

    @Test
    fun mistralMultipartTranscriptionReportsBiasTokensFromDictionaryEntries() {
        val terms = listOf("VoiceSlip", "Voxtral")

        val plan = dictionaryPlanForBuiltInTranscription(
            TranscriptionEngineId.MISTRAL_VOXTRAL_MINI_TRANSCRIBE,
            terms,
            enabled = true
        )

        assertEquals("Mistral context_bias", plan.mechanism)
        assertEquals(2, plan.includedTerms)
        assertEquals(2, plan.totalTerms)
        assertEquals(100, plan.limit)
        assertTrue(plan.prompt!!.contains("VoiceSlip"))
    }

    @Test
    fun disabledDictionaryDuringTranscriptionKeepsCleanupDictionaryIndependent() {
        val plan = dictionaryPlanForBuiltInTranscription(
            TranscriptionEngineId.GROQ_WHISPER_LARGE_V3,
            listOf("VoiceSlip"),
            enabled = false
        )

        assertFalse(plan.sent)
        assertEquals("Off", plan.mechanism)
        assertEquals(0, plan.includedTerms)
        assertEquals(1, plan.totalTerms)
    }
}
