package com.example.voiceslip.net

import com.example.voiceslip.data.AudioDirectEngineId
import com.example.voiceslip.data.PipelineConfig
import com.example.voiceslip.data.PipelineMode
import com.example.voiceslip.data.PostProcessingProvider
import com.example.voiceslip.data.ProviderId
import com.example.voiceslip.data.StylePreset
import com.example.voiceslip.data.TranscriptionEngineId
import java.io.File

class PipelineExecutor(
    private val keyProvider: (ProviderId) -> String?
) {
    fun execute(
        config: PipelineConfig,
        audioFile: File,
        dictionaryTerms: List<String>
    ): PipelineResult {
        validate(config)
        return when (config.mode) {
            PipelineMode.PURE_TRANSCRIPTION -> {
                val transcript = runTranscription(config.transcriptionEngine, audioFile, dictionaryTerms)
                PipelineResult(
                    rawTranscript = transcript.text,
                    finalText = transcript.text,
                    detectedLanguage = transcript.language,
                    pipelineSummary = config.transcriptionEngine.displayName,
                    provider = transcript.engine.provider.name.lowercase(),
                    model = transcript.result.model,
                    transcriptionProvider = transcript.engine.provider.name,
                    transcriptionModel = transcript.result.model,
                    stylePreset = StylePreset.RAW.name,
                    metadataJson = transcript.result.metadata
                )
            }
            PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING -> {
                val transcript = runTranscription(config.transcriptionEngine, audioFile, dictionaryTerms)
                val processed = runPostProcessing(
                    config.postProcessingProvider,
                    config.postProcessingModel,
                    transcript.result.text,
                    transcript.result.language,
                    dictionaryTerms,
                    config.stylePreset
                )
                PipelineResult(
                    rawTranscript = transcript.result.text,
                    finalText = processed.finalText,
                    detectedLanguage = transcript.result.language,
                    pipelineSummary = "${config.transcriptionEngine.displayName} -> ${config.postProcessingProvider.label} ${processed.model}",
                    provider = transcript.engine.provider.name.lowercase(),
                    model = transcript.result.model,
                    transcriptionProvider = transcript.engine.provider.name,
                    transcriptionModel = transcript.result.model,
                    postProcessingProvider = config.postProcessingProvider.name,
                    postProcessingModel = processed.model,
                    stylePreset = config.stylePreset.name,
                    metadataJson = listOfNotNull(transcript.result.metadata, processed.metadata).joinToString("\n")
                )
            }
            PipelineMode.AUDIO_DIRECT -> {
                val direct = runAudioDirect(config.audioDirectEngine, audioFile, dictionaryTerms, config.stylePreset)
                PipelineResult(
                    rawTranscript = null,
                    finalText = direct.result.finalText,
                    detectedLanguage = direct.result.language,
                    pipelineSummary = "${config.audioDirectEngine.displayName} direct",
                    provider = direct.engine.provider.name.lowercase(),
                    model = direct.result.model,
                    audioModelProvider = direct.engine.provider.name,
                    audioModel = direct.result.model,
                    stylePreset = config.stylePreset.name,
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
        engine: TranscriptionEngineId,
        audioFile: File,
        dictionaryTerms: List<String>
    ): TranscriptWithEngine {
        val key = keyProvider(engine.provider).orEmpty()
        val result = when (engine.provider) {
            ProviderId.MISTRAL -> {
                if (engine.audioChat) {
                    MistralTranscriptionClient().transcribeWithAudioChat(key, audioFile, engine, dictionaryTerms)
                } else {
                    MistralTranscriptionClient().transcribe(key, audioFile, dictionaryTerms, engine.model)
                }
            }
            ProviderId.GROQ -> GroqClient().transcribe(key, audioFile, engine.model, dictionaryTerms)
            ProviderId.OPENROUTER -> throw PipelineException("configuration", "OpenRouter is not a transcription provider in V2.5.")
        }
        if (result.text.isBlank()) throw PipelineException("transcription", "The transcription result was empty.")
        return TranscriptWithEngine(engine, result)
    }

    private fun runPostProcessing(
        provider: PostProcessingProvider,
        model: String,
        rawTranscript: String,
        detectedLanguage: String?,
        dictionaryTerms: List<String>,
        stylePreset: StylePreset
    ): PostProcessingResult {
        if (stylePreset == StylePreset.RAW) {
            return PostProcessingResult(rawTranscript, model.ifBlank { "raw" })
        }
        return when (provider) {
            PostProcessingProvider.GROQ -> GroqClient().processText(
                apiKey = keyProvider(ProviderId.GROQ).orEmpty(),
                model = model,
                rawTranscript = rawTranscript,
                detectedLanguage = detectedLanguage,
                dictionaryTerms = dictionaryTerms,
                stylePreset = stylePreset
            )
            PostProcessingProvider.OPENROUTER -> OpenRouterClient().processText(
                apiKey = keyProvider(ProviderId.OPENROUTER).orEmpty(),
                model = model,
                rawTranscript = rawTranscript,
                detectedLanguage = detectedLanguage,
                dictionaryTerms = dictionaryTerms,
                stylePreset = stylePreset
            )
            PostProcessingProvider.NONE -> throw PipelineException("configuration", "Select a post-processing provider.")
        }
    }

    private fun runAudioDirect(
        engine: AudioDirectEngineId,
        audioFile: File,
        dictionaryTerms: List<String>,
        stylePreset: StylePreset
    ): DirectWithEngine {
        val key = keyProvider(engine.provider).orEmpty()
        val result = MistralTranscriptionClient().directAudio(key, audioFile, engine, stylePreset, dictionaryTerms)
        if (result.finalText.isBlank()) throw PipelineException("audio_direct", "The audio model returned empty text.")
        return DirectWithEngine(engine, result)
    }
}

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
    val engine: TranscriptionEngineId,
    val result: TranscriptionResult
) {
    val text: String get() = result.text
    val language: String? get() = result.language
}

private data class DirectWithEngine(
    val engine: AudioDirectEngineId,
    val result: DirectAudioResult
)
