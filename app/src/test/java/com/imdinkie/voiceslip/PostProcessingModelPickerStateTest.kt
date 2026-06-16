package com.imdinkie.voiceslip

import com.imdinkie.voiceslip.data.ModelOption
import com.imdinkie.voiceslip.data.PipelineConfig
import com.imdinkie.voiceslip.data.PostProcessingProvider
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
    fun modelRowsSupportFuzzySearchAcrossNameAndId() {
        val rows = modelRows(
            models = listOf(
                ModelOption(id = "google/gemini-flash-1.5", name = "Gemini Flash 1.5"),
                ModelOption(id = "openai/gpt-4o-mini", name = "GPT-4o Mini"),
                ModelOption(id = "anthropic/claude-haiku", name = "Claude Haiku")
            ),
            favoriteIds = emptyList(),
            selectedId = "",
            query = "gmn fls",
            fallbackProvider = "OpenRouter"
        )

        assertEquals(listOf("google/gemini-flash-1.5"), rows.map { it.id })

        val idRows = modelRows(
            models = listOf(
                ModelOption(id = "openai/gpt-4o-mini", name = "GPT-4o Mini"),
                ModelOption(id = "anthropic/claude-haiku", name = "Claude Haiku")
            ),
            favoriteIds = emptyList(),
            selectedId = "",
            query = "gpt4o",
            fallbackProvider = "OpenRouter"
        )

        assertEquals(listOf("openai/gpt-4o-mini"), idRows.map { it.id })
    }

    @Test
    fun exactSearchResultsSuppressFuzzyFallback() {
        val rows = modelRows(
            models = listOf(
                ModelOption(id = "google/gemini-flash", name = "Gemini Flash"),
                ModelOption(id = "provider/efficient-large", name = "Great Efficient Model")
            ),
            favoriteIds = emptyList(),
            selectedId = "",
            query = "gem",
            fallbackProvider = "OpenRouter"
        )

        assertEquals(listOf("google/gemini-flash"), rows.map { it.id })
    }

    @Test
    fun shortQueriesDoNotRunFuzzyFallback() {
        val rows = modelRows(
            models = listOf(
                ModelOption(id = "provider/efficient-large", name = "Great Efficient Model")
            ),
            favoriteIds = emptyList(),
            selectedId = "",
            query = "gm",
            fallbackProvider = "OpenRouter"
        )

        assertEquals(emptyList<String>(), rows.map { it.id })
    }

    @Test
    fun fuzzyFallbackIsCappedForLargeCatalogs() {
        val models = (1..150).map {
            ModelOption(id = "provider/efficient-large-$it", name = "Great Efficient Model $it")
        }

        val rows = modelRows(
            models = models,
            favoriteIds = emptyList(),
            selectedId = "",
            query = "gem",
            fallbackProvider = "OpenRouter"
        )

        assertEquals(100, rows.size)
        assertEquals("provider/efficient-large-1", rows.first().id)
        assertEquals("provider/efficient-large-100", rows.last().id)
    }

    @Test
    fun selectedModelRemainsVisibleWhenSearchDoesNotMatch() {
        val rows = modelRows(
            models = listOf(
                ModelOption(id = "provider/selected-model", name = "Selected Model"),
                ModelOption(id = "provider/matching-model", name = "Matching Model")
            ),
            favoriteIds = emptyList(),
            selectedId = "provider/selected-model",
            query = "matching",
            fallbackProvider = "OpenRouter"
        )

        assertEquals(listOf("provider/selected-model", "provider/matching-model"), rows.map { it.id })
    }

    @Test
    fun modelRowsPreferCompactPriceMetadataOverRawId() {
        val rows = modelRows(
            models = listOf(
                ModelOption(
                    id = "google/gemini-flash",
                    name = "Gemini Flash",
                    promptPricePerMillion = 0.30,
                    completionPricePerMillion = 2.50,
                    contextLength = 1_000_000
                )
            ),
            favoriteIds = emptyList(),
            selectedId = "",
            query = "",
            fallbackProvider = "OpenRouter"
        )

        assertEquals("from \$0.30/\$2.50 · 1M\u2060\u00A0\u2060ctx", rows.single().detail)
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
