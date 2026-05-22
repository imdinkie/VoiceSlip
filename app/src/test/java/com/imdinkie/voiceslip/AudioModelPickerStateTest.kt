package com.imdinkie.voiceslip

import com.imdinkie.voiceslip.data.AudioDirectEngineId
import com.imdinkie.voiceslip.data.EngineKind
import com.imdinkie.voiceslip.data.ModelOption
import com.imdinkie.voiceslip.data.PipelineConfig
import com.imdinkie.voiceslip.data.ProviderId
import com.imdinkie.voiceslip.data.TranscriptionEngineId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioModelPickerStateTest {
    @Test
    fun pickerStartsOnCurrentProviderForEachRole() {
        val config = PipelineConfig(
            transcriptionEngineKind = EngineKind.OPENROUTER_AUDIO,
            openRouterAudioTranscriptionModel = "openrouter/transcribe",
            audioDirectEngineKind = EngineKind.BUILT_IN,
            audioDirectEngine = AudioDirectEngineId.MISTRAL_VOXTRAL_SMALL_AUDIO
        )

        assertEquals(
            ProviderId.OPENROUTER,
            initialAudioModelPickerState(AudioModelPickerRole.TRANSCRIPTION, config).activeProvider
        )
        assertEquals(
            ProviderId.MISTRAL,
            initialAudioModelPickerState(AudioModelPickerRole.AUDIO_DIRECT, config).activeProvider
        )
    }

    @Test
    fun switchingProviderIsLocalUntilTranscriptionModelSelection() {
        val config = PipelineConfig(
            transcriptionEngineKind = EngineKind.BUILT_IN,
            transcriptionEngine = TranscriptionEngineId.GROQ_WHISPER_LARGE_V3,
            openRouterAudioTranscriptionModel = "openrouter/old"
        )
        val switched = initialAudioModelPickerState(AudioModelPickerRole.TRANSCRIPTION, config)
            .copy(query = "gemini")
            .switchProvider(ProviderId.OPENROUTER)

        assertEquals(EngineKind.BUILT_IN, config.transcriptionEngineKind)
        assertEquals(TranscriptionEngineId.GROQ_WHISPER_LARGE_V3, config.transcriptionEngine)
        assertEquals(ProviderId.OPENROUTER, switched.activeProvider)
        assertEquals("", switched.query)

        val selected = switched.selectModel(config, "openrouter/new")

        assertEquals(EngineKind.OPENROUTER_AUDIO, selected.transcriptionEngineKind)
        assertEquals("openrouter/new", selected.openRouterAudioTranscriptionModel)
        assertEquals(TranscriptionEngineId.GROQ_WHISPER_LARGE_V3, selected.transcriptionEngine)
        assertEquals(TranscriptionEngineId.GROQ_WHISPER_LARGE_V3, selected.groqTranscriptionEngine)
    }

    @Test
    fun builtInTranscriptionSelectionStoresProviderMemory() {
        val selected = AudioModelPickerState(AudioModelPickerRole.TRANSCRIPTION, ProviderId.GROQ)
            .selectModel(PipelineConfig(), TranscriptionEngineId.GROQ_WHISPER_LARGE_V3_TURBO.name)

        assertEquals(EngineKind.BUILT_IN, selected.transcriptionEngineKind)
        assertEquals(TranscriptionEngineId.GROQ_WHISPER_LARGE_V3_TURBO, selected.transcriptionEngine)
        assertEquals(TranscriptionEngineId.GROQ_WHISPER_LARGE_V3_TURBO, selected.groqTranscriptionEngine)
        assertEquals(TranscriptionEngineId.MISTRAL_VOXTRAL_MINI_TRANSCRIBE, selected.mistralTranscriptionEngine)
    }

    @Test
    fun elevenLabsTranscriptionSelectionStoresProviderMemory() {
        val selected = AudioModelPickerState(AudioModelPickerRole.TRANSCRIPTION, ProviderId.ELEVENLABS)
            .selectModel(PipelineConfig(), TranscriptionEngineId.ELEVENLABS_SCRIBE_V2.name)

        assertEquals(EngineKind.BUILT_IN, selected.transcriptionEngineKind)
        assertEquals(TranscriptionEngineId.ELEVENLABS_SCRIBE_V2, selected.transcriptionEngine)
        assertEquals(TranscriptionEngineId.ELEVENLABS_SCRIBE_V2, selected.elevenLabsTranscriptionEngine)
    }

    @Test
    fun switchingAwayFromActiveBuiltInTranscriptionBackfillsProviderMemory() {
        val config = PipelineConfig(
            transcriptionEngineKind = EngineKind.BUILT_IN,
            transcriptionEngine = TranscriptionEngineId.MISTRAL_VOXTRAL_SMALL_AUDIO,
            mistralTranscriptionEngine = null
        )

        val selected = AudioModelPickerState(AudioModelPickerRole.TRANSCRIPTION, ProviderId.GROQ)
            .selectModel(config, TranscriptionEngineId.GROQ_WHISPER_LARGE_V3.name)

        assertEquals(TranscriptionEngineId.MISTRAL_VOXTRAL_SMALL_AUDIO, selected.mistralTranscriptionEngine)
        assertEquals(TranscriptionEngineId.GROQ_WHISPER_LARGE_V3, selected.groqTranscriptionEngine)
    }

    @Test
    fun audioDirectOpenRouterSelectionDoesNotChangeTranscriptionSelection() {
        val config = PipelineConfig(
            transcriptionEngineKind = EngineKind.OPENROUTER_AUDIO,
            openRouterAudioTranscriptionModel = "openrouter/transcription",
            audioDirectEngineKind = EngineKind.BUILT_IN,
            audioDirectEngine = AudioDirectEngineId.MISTRAL_VOXTRAL_SMALL_AUDIO
        )

        val selected = AudioModelPickerState(AudioModelPickerRole.AUDIO_DIRECT, ProviderId.OPENROUTER)
            .selectModel(config, "openrouter/direct")

        assertEquals("openrouter/transcription", selected.openRouterAudioTranscriptionModel)
        assertEquals(EngineKind.OPENROUTER_AUDIO, selected.audioDirectEngineKind)
        assertEquals("openrouter/direct", selected.openRouterAudioDirectModel)
        assertEquals(AudioDirectEngineId.MISTRAL_VOXTRAL_SMALL_AUDIO, selected.mistralAudioDirectEngine)
    }

    @Test
    fun inactiveProviderSavedModelIsNotActiveSelection() {
        val config = PipelineConfig(
            transcriptionEngineKind = EngineKind.OPENROUTER_AUDIO,
            transcriptionEngine = TranscriptionEngineId.MISTRAL_VOXTRAL_MINI_AUDIO,
            mistralTranscriptionEngine = TranscriptionEngineId.MISTRAL_VOXTRAL_MINI_AUDIO,
            openRouterAudioTranscriptionModel = "openrouter/current"
        )

        assertTrue(isActiveAudioModel(config, AudioModelPickerRole.TRANSCRIPTION, ProviderId.OPENROUTER, "openrouter/current"))
        assertFalse(isSavedAudioModel(config, AudioModelPickerRole.TRANSCRIPTION, ProviderId.OPENROUTER, "openrouter/current"))
        assertFalse(isActiveAudioModel(config, AudioModelPickerRole.TRANSCRIPTION, ProviderId.MISTRAL, TranscriptionEngineId.MISTRAL_VOXTRAL_MINI_AUDIO.name))
        assertTrue(isSavedAudioModel(config, AudioModelPickerRole.TRANSCRIPTION, ProviderId.MISTRAL, TranscriptionEngineId.MISTRAL_VOXTRAL_MINI_AUDIO.name))
    }

    @Test
    fun inactiveProviderWithoutMemoryIsNotDotted() {
        val config = PipelineConfig(
            transcriptionEngineKind = EngineKind.OPENROUTER_AUDIO,
            transcriptionEngine = TranscriptionEngineId.MISTRAL_VOXTRAL_MINI_AUDIO,
            openRouterAudioTranscriptionModel = "openrouter/current"
        )

        assertFalse(isSavedAudioModel(config, AudioModelPickerRole.TRANSCRIPTION, ProviderId.MISTRAL, TranscriptionEngineId.MISTRAL_VOXTRAL_MINI_AUDIO.name))
    }

    @Test
    fun builtInAudioDirectSelectionStoresProviderMemory() {
        val selected = AudioModelPickerState(AudioModelPickerRole.AUDIO_DIRECT, ProviderId.MISTRAL)
            .selectModel(PipelineConfig(), AudioDirectEngineId.MISTRAL_VOXTRAL_MINI_AUDIO.name)

        assertEquals(EngineKind.BUILT_IN, selected.audioDirectEngineKind)
        assertEquals(AudioDirectEngineId.MISTRAL_VOXTRAL_MINI_AUDIO, selected.audioDirectEngine)
        assertEquals(AudioDirectEngineId.MISTRAL_VOXTRAL_MINI_AUDIO, selected.mistralAudioDirectEngine)
    }

    @Test
    fun openRouterAudioFavoritesRemainPinnedInsideSearchResults() {
        val rows = modelRows(
            models = listOf(
                ModelOption(id = "openrouter/alpha-audio", name = "Alpha Audio"),
                ModelOption(id = "openrouter/bravo-audio", name = "Bravo Audio"),
                ModelOption(id = "openrouter/charlie-text", name = "Charlie Text")
            ),
            favoriteIds = listOf("openrouter/bravo-audio"),
            selectedId = "",
            query = "audio",
            fallbackProvider = "OpenRouter"
        )

        assertEquals(listOf("openrouter/bravo-audio", "openrouter/alpha-audio"), rows.map { it.id })
    }

    @Test
    fun selectedUnavailableOpenRouterAudioModelRemainsVisible() {
        val rows = modelRows(
            models = emptyList(),
            favoriteIds = emptyList(),
            selectedId = "openrouter/retired-audio",
            query = "",
            fallbackProvider = "OpenRouter"
        )

        assertEquals(1, rows.size)
        assertEquals("openrouter/retired-audio", rows.single().id)
        assertFalse(rows.single().isAvailable)
    }
}
