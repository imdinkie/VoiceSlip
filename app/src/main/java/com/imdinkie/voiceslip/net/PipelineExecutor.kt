package com.imdinkie.voiceslip.net

import com.imdinkie.voiceslip.data.AudioDirectEngineId
import com.imdinkie.voiceslip.data.DEFAULT_CLEANUP_POLICY
import com.imdinkie.voiceslip.data.ModelOption
import com.imdinkie.voiceslip.data.OpenRouterProviderSort
import com.imdinkie.voiceslip.data.OpenRouterReasoningEffort
import com.imdinkie.voiceslip.data.PipelineConfig
import com.imdinkie.voiceslip.data.PipelineMode
import com.imdinkie.voiceslip.data.PostProcessingProvider
import com.imdinkie.voiceslip.data.ProviderId
import com.imdinkie.voiceslip.data.TranscriptionEngineId
import java.io.File

class PipelineExecutor(
    private val keyProvider: (ProviderId) -> String?,
    private val openRouterProviderSort: () -> OpenRouterProviderSort = { OpenRouterProviderSort.DEFAULT },
    private val openRouterModelLookup: (String) -> ModelOption? = { null }
) {
    fun execute(
        config: PipelineConfig,
        audioFile: File,
        dictionaryTerms: List<String>,
        transcriptionDictionaryTerms: List<String> = dictionaryTerms,
        styleId: String = "casual",
        styleName: String = "Casual",
        stylePrompt: String = "Use a casual texting style.",
        cleanupPolicy: String = defaultCleanupPolicy(),
        languageHints: String = "",
        preserveSpokenLanguage: Boolean = true
    ): PipelineResult {
        validate(config)
        return when (config.mode) {
            PipelineMode.PURE_TRANSCRIPTION -> {
                val transcript = runTranscription(config, audioFile, transcriptionDictionaryTerms, languageHints, preserveSpokenLanguage)
                PipelineResult(
                    rawTranscript = transcript.text,
                    finalText = transcript.text,
                    detectedLanguage = transcript.language,
                    pipelineSummary = transcript.displayName,
                    provider = transcript.provider.name.lowercase(),
                    model = transcript.result.model,
                    transcriptionProvider = transcript.provider.name,
                    transcriptionModel = transcript.result.model,
                    stylePreset = styleId,
                    metadataJson = transcript.result.metadata
                )
            }
            PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING -> {
                val transcript = runTranscription(config, audioFile, transcriptionDictionaryTerms, languageHints, preserveSpokenLanguage)
                val processed = runPostProcessing(
                    config.postProcessingProvider,
                    config.postProcessingModel,
                    transcript.result.text,
                    transcript.result.language,
                    dictionaryTerms,
                    stylePrompt,
                    cleanupPolicy,
                    preserveSpokenLanguage,
                    config.openRouterPostProcessingReasoningEffort
                )
                PipelineResult(
                    rawTranscript = transcript.result.text,
                    finalText = processed.finalText,
                    detectedLanguage = transcript.result.language,
                    pipelineSummary = "${transcript.displayName} -> ${config.postProcessingProvider.label} ${processed.model}",
                    provider = transcript.provider.name.lowercase(),
                    model = transcript.result.model,
                    transcriptionProvider = transcript.provider.name,
                    transcriptionModel = transcript.result.model,
                    postProcessingProvider = config.postProcessingProvider.name,
                    postProcessingModel = processed.model,
                    stylePreset = styleName,
                    metadataJson = listOfNotNull(transcript.result.metadata, processed.metadata).joinToString("\n")
                )
            }
            PipelineMode.AUDIO_DIRECT -> {
                val direct = runAudioDirect(config, audioFile, dictionaryTerms, stylePrompt, cleanupPolicy, languageHints, preserveSpokenLanguage)
                PipelineResult(
                    rawTranscript = null,
                    finalText = direct.result.finalText,
                    detectedLanguage = direct.result.language,
                    pipelineSummary = direct.displayName,
                    provider = direct.provider.name.lowercase(),
                    model = direct.result.model,
                    audioModelProvider = direct.provider.name,
                    audioModel = direct.result.model,
                    stylePreset = styleName,
                    metadataJson = direct.result.metadata
                )
            }
        }
    }

    fun validate(config: PipelineConfig) {
        if (!config.isRunnable()) {
            throw PipelineException("configuration", "Select a post-processing provider and model.")
        }
        config.requiredProviders().forEach { provider ->
            if (keyProvider(provider).isNullOrBlank()) {
                throw PipelineException("api_key", "Add your ${provider.label} API key in VoiceSlip.")
            }
        }
    }

    private fun runTranscription(
        config: PipelineConfig,
        audioFile: File,
        dictionaryTerms: List<String>,
        languageHints: String,
        preserveSpokenLanguage: Boolean
    ): TranscriptWithEngine {
        if (config.transcriptionEngineKind == com.imdinkie.voiceslip.data.EngineKind.OPENROUTER_AUDIO) {
            val model = config.openRouterAudioTranscriptionModel
            val result = OpenRouterClient().transcribeAudio(
                keyProvider(ProviderId.OPENROUTER).orEmpty(),
                model,
                audioFile,
                dictionaryTerms,
                languageHints,
                preserveSpokenLanguage,
                openRouterProviderSort(),
                config.openRouterAudioTranscriptionReasoningEffort,
                supportsOpenRouterReasoning(model)
            )
            if (result.text.isBlank()) throw PipelineException("transcription", "The transcription result was empty.")
            return TranscriptWithEngine("OpenRouter audio $model", ProviderId.OPENROUTER, result)
        }
        val engine = config.transcriptionEngine
        val key = keyProvider(engine.provider).orEmpty()
        val result = when (engine.provider) {
            ProviderId.MISTRAL -> {
                if (engine.audioChat) {
                    MistralTranscriptionClient().transcribeWithAudioChat(key, audioFile, engine, dictionaryTerms, languageHints, preserveSpokenLanguage)
                } else {
                    MistralTranscriptionClient().transcribe(key, audioFile, dictionaryTerms, engine.model)
                }
            }
            ProviderId.GROQ -> GroqClient().transcribe(key, audioFile, engine.model, dictionaryTerms)
            ProviderId.ELEVENLABS -> ElevenLabsClient().transcribe(key, audioFile, engine.model, dictionaryTerms)
            ProviderId.OPENROUTER -> throw PipelineException("configuration", "OpenRouter is not a transcription provider in V2.5.")
        }
        if (result.text.isBlank()) throw PipelineException("transcription", "The transcription result was empty.")
        return TranscriptWithEngine(engine.displayName, engine.provider, result)
    }

    private fun runPostProcessing(
        provider: PostProcessingProvider,
        model: String,
        rawTranscript: String,
        detectedLanguage: String?,
        dictionaryTerms: List<String>,
        stylePrompt: String,
        cleanupPolicy: String,
        preserveSpokenLanguage: Boolean,
        reasoningEffort: OpenRouterReasoningEffort
    ): PostProcessingResult {
        if (stylePrompt.contains("no intentional rewrite", ignoreCase = true)) {
            return PostProcessingResult(rawTranscript, model.ifBlank { "raw" })
        }
        return when (provider) {
            PostProcessingProvider.GROQ -> GroqClient().processText(
                apiKey = keyProvider(ProviderId.GROQ).orEmpty(),
                model = model,
                rawTranscript = rawTranscript,
                detectedLanguage = detectedLanguage,
                dictionaryTerms = dictionaryTerms,
                stylePrompt = stylePrompt,
                cleanupPolicy = cleanupPolicy,
                preserveSpokenLanguage = preserveSpokenLanguage
            )
            PostProcessingProvider.OPENROUTER -> OpenRouterClient().processText(
                apiKey = keyProvider(ProviderId.OPENROUTER).orEmpty(),
                model = model,
                rawTranscript = rawTranscript,
                detectedLanguage = detectedLanguage,
                dictionaryTerms = dictionaryTerms,
                stylePrompt = stylePrompt,
                cleanupPolicy = cleanupPolicy,
                preserveSpokenLanguage = preserveSpokenLanguage,
                providerSort = openRouterProviderSort(),
                reasoningEffort = reasoningEffort,
                supportsReasoning = supportsOpenRouterReasoning(model)
            )
            PostProcessingProvider.NONE -> throw PipelineException("configuration", "Select a post-processing provider.")
        }
    }

    private fun runAudioDirect(
        config: PipelineConfig,
        audioFile: File,
        dictionaryTerms: List<String>,
        stylePrompt: String,
        cleanupPolicy: String,
        languageHints: String,
        preserveSpokenLanguage: Boolean
    ): DirectWithEngine {
        if (config.audioDirectEngineKind == com.imdinkie.voiceslip.data.EngineKind.OPENROUTER_AUDIO) {
            val model = config.openRouterAudioDirectModel
            val result = OpenRouterClient().directAudio(
                keyProvider(ProviderId.OPENROUTER).orEmpty(),
                model,
                audioFile,
                stylePrompt,
                cleanupPolicy,
                dictionaryTerms,
                languageHints,
                preserveSpokenLanguage,
                openRouterProviderSort(),
                config.openRouterAudioDirectReasoningEffort,
                supportsOpenRouterReasoning(model)
            )
            if (result.finalText.isBlank()) throw PipelineException("audio_direct", "The audio model returned empty text.")
            return DirectWithEngine("OpenRouter audio $model", ProviderId.OPENROUTER, result)
        }
        val engine = config.audioDirectEngine
        val key = keyProvider(engine.provider).orEmpty()
        val result = MistralTranscriptionClient().directAudio(key, audioFile, engine, stylePrompt, cleanupPolicy, dictionaryTerms, languageHints, preserveSpokenLanguage)
        if (result.finalText.isBlank()) throw PipelineException("audio_direct", "The audio model returned empty text.")
        return DirectWithEngine(engine.displayName, engine.provider, result)
    }

    private fun supportsOpenRouterReasoning(model: String): Boolean =
        openRouterModelLookup(model)?.supportedParameters.orEmpty().any { it.equals("reasoning", ignoreCase = true) }
}

fun outputGuardRejection(text: String, durationMillis: Long): String? {
    val clean = text.trim()
    if (clean.length < 800) return null
    if (repeatedLine(clean)) return "Rejected model output because it repeated the same line excessively."
    if (repeatedPhrase(clean)) return "Rejected model output because it contains extreme repeated phrasing."
    if (durationMillis > 0) {
        val seconds = (durationMillis / 1000L).coerceAtLeast(1L)
        val maxChars = maxOf(4_000L, seconds * 80L)
        if (clean.length > maxChars && clean.length > 8_000) {
            return "Rejected model output because ${clean.length} characters is wildly too long for a ${seconds}s recording."
        }
    }
    return null
}

private fun repeatedLine(text: String): Boolean {
    val lines = text.lines().map { it.trim() }.filter { it.length >= 20 }
    if (lines.size < 8) return false
    return lines.groupingBy { it }.eachCount().values.any { it >= 8 }
}

private fun repeatedPhrase(text: String): Boolean {
    val words = Regex("\\S+").findAll(text.lowercase()).map { it.value.trim(',', '.', '!', '?', ';', ':', '"', '\'') }.filter { it.length > 1 }.toList()
    if (words.size < 120) return false
    for (size in listOf(3, 4, 5, 6, 8, 10)) {
        var repeatCount = 1
        var previous: List<String>? = null
        words.windowed(size, size, partialWindows = false).forEach { chunk ->
            if (chunk == previous) {
                repeatCount++
                if (repeatCount >= 10) return true
            } else {
                previous = chunk
                repeatCount = 1
            }
        }
    }
    return false
}

internal fun defaultCleanupPolicy(): String =
    DEFAULT_CLEANUP_POLICY

data class PipelineResult(
    val rawTranscript: String?,
    val finalText: String,
    val detectedLanguage: String?,
    val pipelineSummary: String,
    val provider: String,
    val model: String,
    val transcriptionProvider: String? = null,
    val transcriptionModel: String? = null,
    val audioModelProvider: String? = null,
    val audioModel: String? = null,
    val postProcessingProvider: String? = null,
    val postProcessingModel: String? = null,
    val stylePreset: String,
    val metadataJson: String? = null
)

class PipelineException(
    val stage: String,
    override val message: String
) : IllegalStateException(message)

private data class TranscriptWithEngine(
    val displayName: String,
    val provider: ProviderId,
    val result: TranscriptionResult
) {
    val text: String get() = result.text
    val language: String? get() = result.language
}

private data class DirectWithEngine(
    val displayName: String,
    val provider: ProviderId,
    val result: DirectAudioResult
)
