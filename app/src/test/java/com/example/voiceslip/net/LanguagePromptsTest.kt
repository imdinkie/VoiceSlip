package com.example.voiceslip.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguagePromptsTest {
    @Test
    fun audioLanguageBlockUsesStrongDefaultInstruction() {
        assertEquals(
            """
            Language:
            Transcribe in the language spoken in the audio. Do not translate under any circumstance.
            If multiple languages are spoken, keep each part in its spoken language.
            """.trimIndent(),
            buildAudioLanguageBlock("", preserveSpokenLanguage = true)
        )
    }

    @Test
    fun audioLanguageBlockAddsExamplesFromCommaSeparatedHints() {
        assertEquals(
            """
            Language:
            Transcribe in the language spoken in the audio. Do not translate under any circumstance.
            If multiple languages are spoken, keep each part in its spoken language.
            If the audio is English, output English. If the audio is German, output German. If the audio is Spanish, output Spanish.
            """.trimIndent(),
            buildAudioLanguageBlock("English, , German, Spanish", preserveSpokenLanguage = true)
        )
    }

    @Test
    fun audioLanguageBlockIsOmittedWhenPreservationIsOff() {
        assertNull(buildAudioLanguageBlock("English, German", preserveSpokenLanguage = false))
    }

    @Test
    fun postProcessingLanguageBlockUsesDetectedLanguageWhenAvailable() {
        assertEquals(
            "Language:\nKeep the output in the detected/spoken language. Detected language: de. Do not translate under any circumstance.",
            buildPostProcessingLanguageBlock(" de ", preserveSpokenLanguage = true)
        )
    }

    @Test
    fun postProcessingLanguageBlockFallsBackToTranscriptLanguage() {
        assertEquals(
            "Language:\nKeep the output in the same language as the transcript. Do not translate under any circumstance.",
            buildPostProcessingLanguageBlock(null, preserveSpokenLanguage = true)
        )
    }

    @Test
    fun postProcessingLanguageBlockIsOmittedWhenPreservationIsOff() {
        assertNull(buildPostProcessingLanguageBlock("de", preserveSpokenLanguage = false))
    }
}
