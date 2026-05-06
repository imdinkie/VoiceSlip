package com.example.voiceslip

import com.example.voiceslip.data.DictionaryPromptPlan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryDisplayTextTest {
    @Test
    fun displayDistinguishesMistralBiasTokensFromDictionaryEntries() {
        val text = dictionaryDuringTranscriptionDetail(
            DictionaryPromptPlan(
                sent = true,
                mechanism = "Mistral context_bias",
                prompt = "VoiceSlip",
                includedTerms = 1,
                totalTerms = 2,
                limit = 100
            )
        )

        assertTrue(text.contains("Mistral Bias Tokens"))
        assertTrue(text.contains("Dictionary Entries"))
        assertFalse(text.contains("Terms included"))
        assertFalse(text.contains("Prompt limit"))
    }

    @Test
    fun displayReportsOpenRouterAudioEntriesWithoutPromptLimitCopy() {
        val text = dictionaryDuringTranscriptionDetail(
            DictionaryPromptPlan(
                sent = true,
                mechanism = "OpenRouter audio prompt spelling constraints",
                prompt = "prompt",
                includedTerms = 150,
                totalTerms = 150
            )
        )

        assertTrue(text.contains("all 150 Dictionary Entries"))
        assertFalse(text.contains("Prompt limit"))
    }
}
