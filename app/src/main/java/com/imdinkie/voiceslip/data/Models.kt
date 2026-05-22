package com.imdinkie.voiceslip.data

enum class RecordingStatus {
    RECORDING,
    TRANSCRIBING,
    SUCCEEDED,
    FAILED,
    CANCELED
}

const val BUBBLE_SIZE_MIN_DP = 44
const val BUBBLE_SIZE_DEFAULT_DP = 56
const val BUBBLE_SIZE_MAX_DP = 64
const val BUBBLE_OPACITY_MIN_PERCENT = 20
const val BUBBLE_OPACITY_DEFAULT_PERCENT = 80
const val BUBBLE_OPACITY_MAX_PERCENT = 100

enum class ProviderId(val label: String) {
    MISTRAL("Mistral"),
    GROQ("Groq"),
    OPENROUTER("OpenRouter"),
    ELEVENLABS("ElevenLabs")
}

enum class PipelineMode(val label: String) {
    PURE_TRANSCRIPTION("Pure transcription"),
    TRANSCRIPTION_PLUS_POST_PROCESSING("Transcription + post-processing"),
    AUDIO_DIRECT("Audio direct")
}

enum class StylePreset(val label: String) {
    VERY_CASUAL("Very casual"),
    CASUAL("Casual"),
    FORMAL("Formal"),
    EXCITED("Excited")
}

enum class PostProcessingProvider(val label: String) {
    NONE("None"),
    GROQ("Groq"),
    OPENROUTER("OpenRouter")
}

enum class EngineKind {
    BUILT_IN,
    OPENROUTER_AUDIO
}

enum class OpenRouterProviderSort(val label: String, val apiValue: String?) {
    DEFAULT("Default", null),
    PRICE("Price", "price"),
    THROUGHPUT("Throughput", "throughput"),
    LATENCY("Latency", "latency")
}

enum class OpenRouterReasoningEffort(val label: String, val apiValue: String?) {
    AUTO("Auto", null),
    NONE("None", "none"),
    MINIMAL("Minimal", "minimal"),
    LOW("Low", "low"),
    MEDIUM("Medium", "medium"),
    HIGH("High", "high"),
    XHIGH("XHigh", "xhigh")
}

val DEFAULT_OPENROUTER_AUDIO_FAVORITES = listOf(
    "google/gemini-3.1-flash-lite-preview",
    "~google/gemini-pro-latest",
    "~google/gemini-flash-latest"
)

enum class TranscriptionEngineId(
    val displayName: String,
    val provider: ProviderId,
    val model: String,
    val audioChat: Boolean = false
) {
    MISTRAL_VOXTRAL_MINI_TRANSCRIBE(
        "Mistral Voxtral Mini Transcribe",
        ProviderId.MISTRAL,
        "voxtral-mini-latest"
    ),
    MISTRAL_VOXTRAL_MINI_AUDIO(
        "Mistral Voxtral Mini audio chat",
        ProviderId.MISTRAL,
        "voxtral-mini-latest",
        audioChat = true
    ),
    MISTRAL_VOXTRAL_SMALL_AUDIO(
        "Mistral Voxtral Small audio chat",
        ProviderId.MISTRAL,
        "voxtral-small-latest",
        audioChat = true
    ),
    GROQ_WHISPER_LARGE_V3(
        "Groq Whisper Large V3",
        ProviderId.GROQ,
        "whisper-large-v3"
    ),
    GROQ_WHISPER_LARGE_V3_TURBO(
        "Groq Whisper Large V3 Turbo",
        ProviderId.GROQ,
        "whisper-large-v3-turbo"
    ),
    ELEVENLABS_SCRIBE_V2(
        "ElevenLabs Scribe v2",
        ProviderId.ELEVENLABS,
        "scribe_v2"
    ),
    ELEVENLABS_SCRIBE_V1(
        "ElevenLabs Scribe v1",
        ProviderId.ELEVENLABS,
        "scribe_v1"
    )
}

enum class AudioDirectEngineId(
    val displayName: String,
    val provider: ProviderId,
    val model: String
) {
    MISTRAL_VOXTRAL_MINI_AUDIO(
        "Mistral Voxtral Mini direct",
        ProviderId.MISTRAL,
        "voxtral-mini-latest"
    ),
    MISTRAL_VOXTRAL_SMALL_AUDIO(
        "Mistral Voxtral Small direct",
        ProviderId.MISTRAL,
        "voxtral-small-latest"
    )
}

data class PipelineConfig(
    val mode: PipelineMode = PipelineMode.PURE_TRANSCRIPTION,
    val transcriptionEngineKind: EngineKind = EngineKind.BUILT_IN,
    val transcriptionEngine: TranscriptionEngineId = TranscriptionEngineId.MISTRAL_VOXTRAL_MINI_TRANSCRIBE,
    val mistralTranscriptionEngine: TranscriptionEngineId? = null,
    val groqTranscriptionEngine: TranscriptionEngineId? = null,
    val elevenLabsTranscriptionEngine: TranscriptionEngineId? = null,
    val openRouterAudioTranscriptionModel: String = "",
    val openRouterAudioTranscriptionReasoningEffort: OpenRouterReasoningEffort = OpenRouterReasoningEffort.NONE,
    val audioDirectEngineKind: EngineKind = EngineKind.BUILT_IN,
    val audioDirectEngine: AudioDirectEngineId = AudioDirectEngineId.MISTRAL_VOXTRAL_SMALL_AUDIO,
    val mistralAudioDirectEngine: AudioDirectEngineId? = null,
    val postProcessingProvider: PostProcessingProvider = PostProcessingProvider.NONE,
    val groqPostProcessingModel: String = "",
    val openRouterPostProcessingModel: String = "",
    val openRouterPostProcessingReasoningEffort: OpenRouterReasoningEffort = OpenRouterReasoningEffort.NONE,
    val openRouterAudioDirectReasoningEffort: OpenRouterReasoningEffort = OpenRouterReasoningEffort.NONE,
    val openRouterAudioDirectModel: String = ""
) {
    val postProcessingModel: String
        get() = when (postProcessingProvider) {
            PostProcessingProvider.GROQ -> groqPostProcessingModel
            PostProcessingProvider.OPENROUTER -> openRouterPostProcessingModel
            PostProcessingProvider.NONE -> ""
        }

    fun requiredProviders(): Set<ProviderId> = buildSet {
        when (mode) {
            PipelineMode.PURE_TRANSCRIPTION -> add(transcriptionProvider())
            PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING -> {
                add(transcriptionProvider())
                when (postProcessingProvider) {
                    PostProcessingProvider.GROQ -> add(ProviderId.GROQ)
                    PostProcessingProvider.OPENROUTER -> add(ProviderId.OPENROUTER)
                    PostProcessingProvider.NONE -> Unit
                }
            }
            PipelineMode.AUDIO_DIRECT -> add(audioDirectProvider())
        }
    }

    fun isRunnable(): Boolean {
        val hasValidModeSelection = when (mode) {
            PipelineMode.PURE_TRANSCRIPTION,
            PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING -> transcriptionEngineKind == EngineKind.BUILT_IN || openRouterAudioTranscriptionModel.isNotBlank()
            PipelineMode.AUDIO_DIRECT -> audioDirectEngineKind == EngineKind.BUILT_IN || openRouterAudioDirectModel.isNotBlank()
        }
        val hasPostProcessing = mode != PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING ||
            postProcessingProvider != PostProcessingProvider.NONE && postProcessingModel.isNotBlank()
        return hasValidModeSelection && hasPostProcessing
    }

    fun transcriptionProvider(): ProviderId =
        if (transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO) ProviderId.OPENROUTER else transcriptionEngine.provider

    fun transcriptionModel(): String =
        if (transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO) openRouterAudioTranscriptionModel else transcriptionEngine.model

    fun transcriptionDisplayName(): String =
        if (transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO) openRouterAudioTranscriptionModel.ifBlank { "OpenRouter audio" } else transcriptionEngine.displayName

    fun audioDirectProvider(): ProviderId =
        if (audioDirectEngineKind == EngineKind.OPENROUTER_AUDIO) ProviderId.OPENROUTER else audioDirectEngine.provider

    fun audioDirectModel(): String =
        if (audioDirectEngineKind == EngineKind.OPENROUTER_AUDIO) openRouterAudioDirectModel else audioDirectEngine.model

    fun audioDirectDisplayName(): String =
        if (audioDirectEngineKind == EngineKind.OPENROUTER_AUDIO) openRouterAudioDirectModel.ifBlank { "OpenRouter audio" } else audioDirectEngine.displayName

    fun withPostProcessingModel(model: String): PipelineConfig = when (postProcessingProvider) {
        PostProcessingProvider.GROQ -> copy(groqPostProcessingModel = model)
        PostProcessingProvider.OPENROUTER -> copy(openRouterPostProcessingModel = model)
        PostProcessingProvider.NONE -> this
    }
}

data class ModelOption(
    val id: String,
    val name: String,
    val provider: String = "",
    val contextLength: Int? = null,
    val promptPricePerMillion: Double? = null,
    val completionPricePerMillion: Double? = null,
    val supportedParameters: List<String> = emptyList()
)

data class OpenRouterEndpointMetric(
    val p50: Double? = null
)

data class OpenRouterEndpointOption(
    val name: String,
    val providerName: String,
    val tag: String,
    val promptPricePerMillion: Double? = null,
    val completionPricePerMillion: Double? = null,
    val throughput: OpenRouterEndpointMetric = OpenRouterEndpointMetric(),
    val latency: OpenRouterEndpointMetric = OpenRouterEndpointMetric(),
    val uptimeLast30m: Double? = null
)

data class OpenRouterEndpointDetails(
    val modelId: String,
    val modelName: String,
    val endpoints: List<OpenRouterEndpointOption>,
    val fetchedAtMillis: Long
)

data class HistoryItem(
    val id: String,
    val createdAtMillis: Long,
    val audioPath: String,
    val durationMillis: Long,
    val status: RecordingStatus,
    val transcript: String? = null,
    val rawTranscript: String? = null,
    val finalText: String? = null,
    val detectedLanguage: String? = null,
    val error: String? = null,
    val provider: String = "mistral",
    val model: String = "voxtral-mini-latest",
    val pipelineMode: String = PipelineMode.PURE_TRANSCRIPTION.name,
    val transcriptionProvider: String? = null,
    val transcriptionModel: String? = null,
    val audioModelProvider: String? = null,
    val audioModel: String? = null,
    val postProcessingProvider: String? = null,
    val postProcessingModel: String? = null,
    val stylePreset: String = StylePreset.CASUAL.name,
    val pipelineSummary: String? = null,
    val errorStage: String? = null,
    val metadataJson: String? = null,
    val retryCount: Int = 0,
    val targetPackage: String? = null,
    val targetAppLabel: String? = null,
    val resolvedCategoryId: String? = null,
    val resolvedCategoryName: String? = null,
    val resolvedStyleId: String? = null,
    val resolvedStyleName: String? = null,
    val stylePromptSnapshot: String? = null,
    val dictionarySnapshot: String? = null,
    val pipelineConfigSnapshot: String? = null,
    val dictionaryRoutingSnapshot: String? = null
) {
    fun displayText(): String? = finalText ?: transcript ?: rawTranscript
}

data class DictionaryEntry(
    val id: String,
    val phrase: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class VoiceStyle(
    val id: String,
    val name: String,
    val basePresetId: String?,
    val defaultPrompt: String,
    val userPromptOverride: String?,
    val isBuiltIn: Boolean
) {
    val effectivePrompt: String get() = userPromptOverride?.takeIf { it.isNotBlank() } ?: defaultPrompt
}

data class VoiceCategory(
    val id: String,
    val name: String,
    val styleId: String,
    val isPreset: Boolean
)

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val iconCacheKey: String,
    val categoryId: String?,
    val categoryName: String?,
    val lastSeenAtMillis: Long?
)

data class StyleResolution(
    val targetPackage: String?,
    val targetAppLabel: String,
    val categoryId: String,
    val categoryName: String,
    val styleId: String,
    val styleName: String,
    val stylePrompt: String
)

data class DictionaryPromptPlan(
    val sent: Boolean,
    val mechanism: String,
    val prompt: String?,
    val includedTerms: Int,
    val totalTerms: Int,
    val limit: Int? = null,
    val truncated: Boolean = includedTerms < totalTerms
)

data class PipelinePreview(
    val title: String,
    val lines: List<String>
)
