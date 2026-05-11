package com.example.voiceslip.net

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterClientTest {
    @Test
    fun audioInputTextOutputModelIsIncluded() {
        assertTrue(checkSupportsAudioToText(
            inputModalities = listOf("text", "audio", "image"),
            outputModalities = listOf("text"),
            modality = "text+image+audio->text"
        ))
    }

    @Test
    fun audioInputAudioOutputOnlyModelIsExcluded() {
        assertFalse(checkSupportsAudioToText(
            inputModalities = listOf("text", "audio"),
            outputModalities = listOf("audio"),
            modality = "text+audio->audio"
        ))
    }

    @Test
    fun audioInputTranscriptionOutputModelIsExcluded() {
        assertFalse(checkSupportsAudioToText(
            inputModalities = listOf("audio"),
            outputModalities = listOf("transcription"),
            modality = "audio->transcription"
        ))
    }

    @Test
    fun textInputImageOutputModelIsExcluded() {
        assertFalse(checkSupportsAudioToText(
            inputModalities = listOf("text"),
            outputModalities = listOf("image"),
            modality = "text->image"
        ))
    }

    @Test
    fun textOnlyModelIsExcluded() {
        assertFalse(checkSupportsAudioToText(
            inputModalities = listOf("text"),
            outputModalities = listOf("text"),
            modality = "text->text"
        ))
    }

    @Test
    fun fallbackModalityPathIsSupported() {
        assertTrue(checkSupportsAudioToText(
            inputModalities = emptyList(),
            outputModalities = emptyList(),
            modality = "text+audio->text"
        ))
    }

    @Test
    fun fallbackModalitiesCanRepresentAudioToTextSupport() {
        assertTrue(supportsOpenRouterAudioToText(
            inputModalities = emptyList(),
            outputModalities = emptyList(),
            fallbackModalities = listOf("audio", "text"),
            modalityPath = null
        ))
    }

    @Test
    fun defaultAudioFavoriteDoesNotBypassCapabilityFiltering() {
        assertFalse(checkSupportsAudioToText(
            inputModalities = listOf("text"),
            outputModalities = listOf("text"),
            modality = "text->text"
        ))
    }

    private fun checkSupportsAudioToText(
        inputModalities: List<String>,
        outputModalities: List<String>,
        modality: String
    ): Boolean = supportsOpenRouterAudioToText(
        inputModalities = inputModalities,
        outputModalities = outputModalities,
        modalityPath = modality
    )
}
