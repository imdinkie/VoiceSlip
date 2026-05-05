package com.example.voiceslip

import com.example.voiceslip.data.ModelOption
import com.example.voiceslip.data.PipelineConfig
import com.example.voiceslip.data.PostProcessingProvider

internal data class ModelDisplayRow(
    val id: String,
    val name: String,
    val provider: String,
    val isAvailable: Boolean
)

internal data class PostProcessingPickerState(
    val activeProvider: PostProcessingProvider,
    val query: String = ""
) {
    fun switchProvider(provider: PostProcessingProvider): PostProcessingPickerState =
        copy(activeProvider = provider, query = "")

    fun selectModel(config: PipelineConfig, modelId: String): PipelineConfig =
        config.copy(postProcessingProvider = activeProvider).withPostProcessingModel(modelId)
}

internal fun initialPostProcessingPickerState(config: PipelineConfig): PostProcessingPickerState =
    PostProcessingPickerState(
        activeProvider = if (config.postProcessingProvider == PostProcessingProvider.NONE || config.postProcessingModel.isBlank()) {
            PostProcessingProvider.GROQ
        } else {
            config.postProcessingProvider
        }
    )

internal fun selectedPostProcessingModel(config: PipelineConfig, provider: PostProcessingProvider): String =
    when (provider) {
        PostProcessingProvider.GROQ -> config.groqPostProcessingModel
        PostProcessingProvider.OPENROUTER -> config.openRouterPostProcessingModel
        PostProcessingProvider.NONE -> ""
    }

internal fun isActivePostProcessingModel(config: PipelineConfig, provider: PostProcessingProvider, modelId: String): Boolean =
    config.postProcessingProvider == provider && selectedPostProcessingModel(config, provider) == modelId

internal fun isSavedPostProcessingModel(config: PipelineConfig, provider: PostProcessingProvider, modelId: String): Boolean =
    config.postProcessingProvider != provider && selectedPostProcessingModel(config, provider) == modelId

internal fun modelRows(
    models: List<ModelOption>,
    favoriteIds: List<String>,
    selectedId: String?,
    query: String,
    favoritesOnly: Boolean = false,
    fallbackProvider: String = "OpenRouter"
): List<ModelDisplayRow> {
    val cachedById = models.associateBy { it.id }
    val orderedIds = mutableListOf<String>()
    favoriteIds.forEach { if (it !in orderedIds) orderedIds += it }
    selectedId?.takeIf { it.isNotBlank() && it !in orderedIds && !favoritesOnly }?.let { orderedIds += it }
    if (!favoritesOnly) {
        models.map { it.id }.forEach { if (it !in orderedIds) orderedIds += it }
    }
    val cleanQuery = query.trim()
    return orderedIds.mapNotNull { id ->
        val model = cachedById[id]
        val row = ModelDisplayRow(
            id = id,
            name = model?.name ?: id,
            provider = model?.provider?.takeIf { it.isNotBlank() } ?: fallbackProvider,
            isAvailable = model != null
        )
        row.takeIf { cleanQuery.isBlank() || it.id.contains(cleanQuery, true) || it.name.contains(cleanQuery, true) }
    }
}
