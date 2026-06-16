package com.imdinkie.voiceslip

import com.imdinkie.voiceslip.data.AudioDirectEngineId
import com.imdinkie.voiceslip.data.EngineKind
import com.imdinkie.voiceslip.data.ModelOption
import com.imdinkie.voiceslip.data.OpenRouterReasoningEffort
import com.imdinkie.voiceslip.data.PipelineConfig
import com.imdinkie.voiceslip.data.PostProcessingProvider
import com.imdinkie.voiceslip.data.ProviderId
import com.imdinkie.voiceslip.data.TranscriptionEngineId

internal data class ModelDisplayRow(
    val id: String,
    val name: String,
    val provider: String,
    val isAvailable: Boolean,
    val detail: String = id
)

internal data class PostProcessingPickerState(
    val activeProvider: PostProcessingProvider,
    val query: String = ""
) {
    fun switchProvider(provider: PostProcessingProvider): PostProcessingPickerState =
        copy(activeProvider = provider, query = "")

    fun selectModel(
        config: PipelineConfig,
        modelId: String,
        reasoningEffort: OpenRouterReasoningEffort = OpenRouterReasoningEffort.NONE
    ): PipelineConfig =
        config.copy(postProcessingProvider = activeProvider)
            .withPostProcessingModel(modelId)
            .let {
                if (activeProvider == PostProcessingProvider.OPENROUTER) {
                    it.copy(openRouterPostProcessingReasoningEffort = reasoningEffort)
                } else {
                    it
                }
            }
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

internal enum class AudioModelPickerRole {
    TRANSCRIPTION,
    AUDIO_DIRECT
}

internal data class AudioModelPickerState(
    val role: AudioModelPickerRole,
    val activeProvider: ProviderId,
    val query: String = ""
) {
    fun switchProvider(provider: ProviderId): AudioModelPickerState =
        copy(activeProvider = provider, query = "")

    fun selectModel(
        config: PipelineConfig,
        modelId: String,
        reasoningEffort: OpenRouterReasoningEffort = OpenRouterReasoningEffort.NONE
    ): PipelineConfig =
        when (role) {
            AudioModelPickerRole.TRANSCRIPTION -> selectTranscriptionModel(config, activeProvider, modelId, reasoningEffort)
            AudioModelPickerRole.AUDIO_DIRECT -> selectAudioDirectModel(config, activeProvider, modelId, reasoningEffort)
        }
}

internal fun initialAudioModelPickerState(role: AudioModelPickerRole, config: PipelineConfig): AudioModelPickerState =
    AudioModelPickerState(
        role = role,
        activeProvider = when (role) {
            AudioModelPickerRole.TRANSCRIPTION -> config.transcriptionProvider()
            AudioModelPickerRole.AUDIO_DIRECT -> config.audioDirectProvider()
        }
    )

internal fun selectedAudioModelId(config: PipelineConfig, role: AudioModelPickerRole, provider: ProviderId): String =
    when (role) {
        AudioModelPickerRole.TRANSCRIPTION -> when (provider) {
            ProviderId.MISTRAL, ProviderId.GROQ, ProviderId.ELEVENLABS -> config.transcriptionEngine.takeIf { it.provider == provider }?.name.orEmpty()
            ProviderId.OPENROUTER -> config.openRouterAudioTranscriptionModel
        }
        AudioModelPickerRole.AUDIO_DIRECT -> when (provider) {
            ProviderId.MISTRAL -> config.audioDirectEngine.takeIf { it.provider == provider }?.name.orEmpty()
            ProviderId.GROQ, ProviderId.ELEVENLABS -> ""
            ProviderId.OPENROUTER -> config.openRouterAudioDirectModel
        }
    }

internal fun savedAudioModelId(config: PipelineConfig, role: AudioModelPickerRole, provider: ProviderId): String =
    when (role) {
        AudioModelPickerRole.TRANSCRIPTION -> when (provider) {
            ProviderId.MISTRAL -> config.mistralTranscriptionEngine?.name.orEmpty()
            ProviderId.GROQ -> config.groqTranscriptionEngine?.name.orEmpty()
            ProviderId.ELEVENLABS -> config.elevenLabsTranscriptionEngine?.name.orEmpty()
            ProviderId.OPENROUTER -> config.openRouterAudioTranscriptionModel
        }
        AudioModelPickerRole.AUDIO_DIRECT -> when (provider) {
            ProviderId.MISTRAL -> config.mistralAudioDirectEngine?.name.orEmpty()
            ProviderId.GROQ, ProviderId.ELEVENLABS -> ""
            ProviderId.OPENROUTER -> config.openRouterAudioDirectModel
        }
    }

internal fun isActiveAudioModel(config: PipelineConfig, role: AudioModelPickerRole, provider: ProviderId, modelId: String): Boolean =
    activeAudioProvider(config, role) == provider && selectedAudioModelId(config, role, provider) == modelId

internal fun isSavedAudioModel(config: PipelineConfig, role: AudioModelPickerRole, provider: ProviderId, modelId: String): Boolean =
    activeAudioProvider(config, role) != provider && savedAudioModelId(config, role, provider) == modelId

internal fun builtInAudioRows(role: AudioModelPickerRole, provider: ProviderId): List<ModelDisplayRow> =
    when (role) {
        AudioModelPickerRole.TRANSCRIPTION -> TranscriptionEngineId.entries
            .filter { it.provider == provider }
            .map {
                ModelDisplayRow(
                    id = it.name,
                    name = it.displayName,
                    provider = it.provider.label,
                    isAvailable = true,
                    detail = "${it.model} · ${transcriptionEngineRole(it)}"
                )
            }
        AudioModelPickerRole.AUDIO_DIRECT -> AudioDirectEngineId.entries
            .filter { it.provider == provider }
            .map {
                ModelDisplayRow(
                    id = it.name,
                    name = it.displayName,
                    provider = it.provider.label,
                    isAvailable = true,
                    detail = "${it.model} · audio chat -> final text"
                )
            }
    }

private fun activeAudioProvider(config: PipelineConfig, role: AudioModelPickerRole): ProviderId =
    when (role) {
        AudioModelPickerRole.TRANSCRIPTION -> config.transcriptionProvider()
        AudioModelPickerRole.AUDIO_DIRECT -> config.audioDirectProvider()
    }

private fun selectTranscriptionModel(
    config: PipelineConfig,
    provider: ProviderId,
    modelId: String,
    reasoningEffort: OpenRouterReasoningEffort
): PipelineConfig =
    when (provider) {
        ProviderId.MISTRAL, ProviderId.GROQ, ProviderId.ELEVENLABS -> {
            val engine = TranscriptionEngineId.entries.firstOrNull { it.name == modelId } ?: return config
            val rememberedConfig = config.rememberActiveTranscriptionModel()
            when (provider) {
                ProviderId.MISTRAL -> rememberedConfig.copy(
                    transcriptionEngineKind = EngineKind.BUILT_IN,
                    transcriptionEngine = engine,
                    mistralTranscriptionEngine = engine
                )
                ProviderId.GROQ -> rememberedConfig.copy(
                    transcriptionEngineKind = EngineKind.BUILT_IN,
                    transcriptionEngine = engine,
                    groqTranscriptionEngine = engine
                )
                ProviderId.ELEVENLABS -> rememberedConfig.copy(
                    transcriptionEngineKind = EngineKind.BUILT_IN,
                    transcriptionEngine = engine,
                    elevenLabsTranscriptionEngine = engine
                )
                ProviderId.OPENROUTER -> rememberedConfig
            }
        }
        ProviderId.OPENROUTER -> config.rememberActiveTranscriptionModel().copy(
            transcriptionEngineKind = EngineKind.OPENROUTER_AUDIO,
            openRouterAudioTranscriptionModel = modelId,
            openRouterAudioTranscriptionReasoningEffort = reasoningEffort
        )
    }

private fun selectAudioDirectModel(
    config: PipelineConfig,
    provider: ProviderId,
    modelId: String,
    reasoningEffort: OpenRouterReasoningEffort
): PipelineConfig =
    when (provider) {
        ProviderId.MISTRAL -> {
            val engine = AudioDirectEngineId.entries.firstOrNull { it.name == modelId } ?: return config
            config.rememberActiveAudioDirectModel().copy(
                audioDirectEngineKind = EngineKind.BUILT_IN,
                audioDirectEngine = engine,
                mistralAudioDirectEngine = engine
            )
        }
        ProviderId.GROQ,
        ProviderId.ELEVENLABS -> config.rememberActiveAudioDirectModel()
        ProviderId.OPENROUTER -> config.rememberActiveAudioDirectModel().copy(
            audioDirectEngineKind = EngineKind.OPENROUTER_AUDIO,
            openRouterAudioDirectModel = modelId,
            openRouterAudioDirectReasoningEffort = reasoningEffort
        )
    }

private fun PipelineConfig.rememberActiveTranscriptionModel(): PipelineConfig {
    if (transcriptionEngineKind != EngineKind.BUILT_IN) return this
    return when (transcriptionEngine.provider) {
        ProviderId.MISTRAL -> if (mistralTranscriptionEngine == null) copy(mistralTranscriptionEngine = transcriptionEngine) else this
        ProviderId.GROQ -> if (groqTranscriptionEngine == null) copy(groqTranscriptionEngine = transcriptionEngine) else this
        ProviderId.ELEVENLABS -> if (elevenLabsTranscriptionEngine == null) copy(elevenLabsTranscriptionEngine = transcriptionEngine) else this
        ProviderId.OPENROUTER -> this
    }
}

private fun PipelineConfig.rememberActiveAudioDirectModel(): PipelineConfig {
    if (audioDirectEngineKind != EngineKind.BUILT_IN) return this
    return when (audioDirectEngine.provider) {
        ProviderId.MISTRAL -> if (mistralAudioDirectEngine == null) copy(mistralAudioDirectEngine = audioDirectEngine) else this
        ProviderId.GROQ,
        ProviderId.ELEVENLABS,
        ProviderId.OPENROUTER -> this
    }
}

private fun transcriptionEngineRole(engine: TranscriptionEngineId): String =
    when {
        engine.audioChat -> "audio chat -> raw transcript"
        engine.provider == ProviderId.GROQ -> "speech-to-text endpoint"
        engine.provider == ProviderId.ELEVENLABS -> if (engine == TranscriptionEngineId.ELEVENLABS_SCRIBE_V2) "from \$0.22/hr · keyterms +20% · M4A" else "from \$0.22/hr · no keyterms · M4A"
        else -> "transcription endpoint"
    }

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
            isAvailable = model != null,
            detail = model?.compactModelDetail(fallbackProvider) ?: id
        )
        val isPinnedSelected = !favoritesOnly && selectedId?.takeIf { it.isNotBlank() } == id
        row.takeIf { isPinnedSelected || modelRowMatchesQuery(it, cleanQuery) }
    }
}

internal fun modelRowMatchesQuery(row: ModelDisplayRow, query: String): Boolean {
    val tokens = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return true
    val targets = listOf(row.name, row.id, row.provider, row.detail).filter { it.isNotBlank() }
    return tokens.all { token -> targets.any { it.matchesFuzzyToken(token) } }
}

private fun String.matchesFuzzyToken(token: String): Boolean {
    if (contains(token, ignoreCase = true)) return true
    val target = searchNormalized()
    val needle = token.searchNormalized()
    if (needle.isBlank()) return true
    if (target.contains(needle)) return true
    if (needle.length < 2) return false
    var index = 0
    for (char in target) {
        if (char == needle[index]) {
            index++
            if (index == needle.length) return true
        }
    }
    return false
}

private fun String.searchNormalized(): String =
    lowercase().filter { it.isLetterOrDigit() }

private fun ModelOption.compactModelDetail(fallbackProvider: String): String {
    val metadata = listOfNotNull(
        priceSummary(),
        contextLength?.let { keepTogether("${formatContextLength(it)} ctx") }
    ).joinToString(" · ")
    if (metadata.isNotBlank()) return metadata
    return if ((provider.ifBlank { fallbackProvider }).equals("Groq", ignoreCase = true) && name == id) "" else id
}

private fun ModelOption.priceSummary(): String? {
    val input = promptPricePerMillion ?: return null
    val output = completionPricePerMillion ?: return null
    return "from ${formatPrice(input)}/${formatPrice(output)}"
}

private fun formatContextLength(value: Int): String =
    when {
        value >= 1_000_000 -> "${value / 1_000_000}M"
        value >= 1_000 -> "${value / 1_000}k"
        else -> value.toString()
    }

private fun formatPrice(value: Double): String =
    if (value == 0.0) "\$0" else "\$" + if (value < 10) "%.2f".format(value) else "%.0f".format(value)

private fun keepTogether(value: String): String = value.replace(" ", "\u2060\u00A0\u2060").replace("/", "\u2060/\u2060")
