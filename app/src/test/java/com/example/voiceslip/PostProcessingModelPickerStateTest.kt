package com.example.voiceslip

import com.example.voiceslip.data.ModelOption
import com.example.voiceslip.data.PipelineConfig
import com.example.voiceslip.data.PostProcessingProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostProcessingModelPickerStateTest {
    @Test
    fun pickerStartsOnCurrentProviderOrGroqWhenNoModelIsSelected() {
        assertEquals(
            PostProcessingProvider.GROQ,
            initialPostProcessingPickerState(PipelineConfig()).activeProvider
        )
        assertEquals(
            PostProcessingProvider.GROQ,
            initialPostProcessingPickerState(
                PipelineConfig(postProcessingProvider = PostProcessingProvider.OPENROUTER)
            ).activeProvider
        )
        assertEquals(
            PostProcessingProvider.OPENROUTER,
            initialPostProcessingPickerState(
                PipelineConfig(
                    postProcessingProvider = PostProcessingProvider.OPENROUTER,
                    openRouterPostProcessingModel = "openrouter/selected"
                )
            ).activeProvider
        )
    }

    @Test
    fun switchingProviderIsLocalUntilModelSelection() {
        val config = PipelineConfig(
            postProcessingProvider = PostProcessingProvider.GROQ,
            groqPostProcessingModel = "llama-3.1-8b",
            openRouterPostProcessingModel = "openrouter/old"
        )
        val switched = initialPostProcessingPickerState(config)
            .copy(query = "gemini")
            .switchProvider(PostProcessingProvider.OPENROUTER)

        assertEquals(PostProcessingProvider.GROQ, config.postProcessingProvider)
        assertEquals("llama-3.1-8b", config.groqPostProcessingModel)
        assertEquals(PostProcessingProvider.OPENROUTER, switched.activeProvider)
        assertEquals("", switched.query)

        val selected = switched.selectModel(config, "openrouter/new")

        assertEquals(PostProcessingProvider.OPENROUTER, selected.postProcessingProvider)
        assertEquals("openrouter/new", selected.openRouterPostProcessingModel)
        assertEquals("llama-3.1-8b", selected.groqPostProcessingModel)
    }

    @Test
    fun rowSelectionStoresProviderAndModelForActiveProvider() {
        val selected = PostProcessingPickerState(PostProcessingProvider.GROQ)
            .selectModel(PipelineConfig(), "mixtral-8x7b")

        assertEquals(PostProcessingProvider.GROQ, selected.postProcessingProvider)
        assertEquals("mixtral-8x7b", selected.groqPostProcessingModel)
    }

    @Test
    fun savedModelForInactiveProviderIsNotActiveSelection() {
        val config = PipelineConfig(
            postProcessingProvider = PostProcessingProvider.OPENROUTER,
            groqPostProcessingModel = "groq/previous",
            openRouterPostProcessingModel = "openrouter/current"
        )

        assertTrue(isActivePostProcessingModel(config, PostProcessingProvider.OPENROUTER, "openrouter/current"))
        assertFalse(isSavedPostProcessingModel(config, PostProcessingProvider.OPENROUTER, "openrouter/current"))
        assertFalse(isActivePostProcessingModel(config, PostProcessingProvider.GROQ, "groq/previous"))
        assertTrue(isSavedPostProcessingModel(config, PostProcessingProvider.GROQ, "groq/previous"))
    }

    @Test
    fun favoritesRemainPinnedInsideSearchResults() {
        val rows = modelRows(
            models = listOf(
                ModelOption(id = "provider/alpha-model", name = "Alpha Model"),
                ModelOption(id = "provider/bravo-model", name = "Bravo Model"),
                ModelOption(id = "provider/charlie-chat", name = "Charlie Chat")
            ),
            favoriteIds = listOf("provider/bravo-model"),
            selectedId = "",
            query = "model",
            fallbackProvider = "Groq"
        )

        assertEquals(listOf("provider/bravo-model", "provider/alpha-model"), rows.map { it.id })
    }

    @Test
    fun selectedUnavailableModelRemainsVisibleAndMarkedUnavailable() {
        val rows = modelRows(
            models = emptyList(),
            favoriteIds = emptyList(),
            selectedId = "provider/retired-model",
            query = "",
            fallbackProvider = "OpenRouter"
        )

        assertEquals(1, rows.size)
        assertEquals("provider/retired-model", rows.single().id)
        assertFalse(rows.single().isAvailable)
    }

    @Test
    fun favoritesOnlyRowsDoNotIncludeSelectedUnavailableModel() {
        val rows = modelRows(
            models = emptyList(),
            favoriteIds = listOf("provider/favorite"),
            selectedId = "provider/retired-model",
            query = "",
            favoritesOnly = true,
            fallbackProvider = "OpenRouter"
        )

        assertEquals(listOf("provider/favorite"), rows.map { it.id })
    }
}
