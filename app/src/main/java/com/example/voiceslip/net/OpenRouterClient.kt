package com.example.voiceslip.net

import com.example.voiceslip.data.ModelOption
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

class OpenRouterClient {
    fun processText(
        apiKey: String,
        model: String,
        rawTranscript: String,
        detectedLanguage: String?,
        dictionaryTerms: List<String>,
        stylePrompt: String,
        cleanupPolicy: String,
        preserveSpokenLanguage: Boolean
    ): PostProcessingResult {
        val request = postProcessingRequest(model, rawTranscript, detectedLanguage, dictionaryTerms, stylePrompt, cleanupPolicy, preserveSpokenLanguage)
        val json = JSONObject(postJson("https://openrouter.ai/api/v1/chat/completions", apiKey, request))
        return parsePostProcessing(json, model)
    }

    fun listModels(apiKey: String): List<ModelOption> {
        return fetchModels(apiKey)
            .filter { it.supportsText() }
            .map { it.toModelOption() }
            .sortedBy { it.name.lowercase() }
    }

    fun listAudioModels(apiKey: String): List<ModelOption> {
        return fetchModels(apiKey, outputModalities = "all")
            .filter { it.supportsOpenRouterAudioToText() }
            .map { it.toModelOption() }
            .sortedBy { it.name.lowercase() }
    }

    fun transcribeAudio(
        apiKey: String,
        model: String,
        audioFile: File,
        dictionaryTerms: List<String>,
        languageHints: String,
        preserveSpokenLanguage: Boolean
    ): TranscriptionResult {
        val prompt = buildAudioTranscriptionPrompt(languageHints, preserveSpokenLanguage, dictionaryTerms)
        val json = postAudioChat(apiKey, model, audioFile, prompt)
        return TranscriptionResult(
            text = json.chatContent().trim(),
            language = null,
            model = json.optString("model", model),
            metadata = json.toString()
        )
    }

    fun directAudio(
        apiKey: String,
        model: String,
        audioFile: File,
        stylePrompt: String,
        cleanupPolicy: String,
        dictionaryTerms: List<String>,
        languageHints: String,
        preserveSpokenLanguage: Boolean
    ): DirectAudioResult {
        val prompt = buildAudioDirectPrompt(cleanupPolicy, stylePrompt, languageHints, preserveSpokenLanguage, dictionaryTerms)
        val json = postAudioChat(apiKey, model, audioFile, prompt)
        return DirectAudioResult(
            finalText = json.chatContent().trim(),
            language = null,
            model = json.optString("model", model),
            metadata = json.toString()
        )
    }

    private fun fetchModels(apiKey: String, outputModalities: String? = null): List<JSONObject> {
        val url = buildString {
            append("https://openrouter.ai/api/v1/models")
            outputModalities?.let { append("?output_modalities=").append(it) }
        }
        val connection = (URL(url).openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        if (apiKey.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $apiKey")
        val responseCode = connection.responseCode
        val body = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()
        if (responseCode !in 200..299) {
            throw IllegalStateException("OpenRouter model list failed ($responseCode): ${body.take(500)}")
        }
        val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
        return (0 until data.length()).mapNotNull { data.optJSONObject(it) }.filter { it.optString("id").isNotBlank() }
    }

    private fun postAudioChat(apiKey: String, model: String, audioFile: File, prompt: String): JSONObject {
        val request = JSONObject()
            .put("model", model)
            .put("temperature", 0.1)
            .put("max_tokens", 6000)
            .put("stream", false)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "content",
                            JSONArray()
                                .put(JSONObject().put("type", "text").put("text", prompt))
                                .put(
                                    JSONObject()
                                        .put("type", "input_audio")
                                        .put(
                                            "input_audio",
                                            JSONObject()
                                                .put("data", Base64.getEncoder().encodeToString(audioFile.readBytes()))
                                                .put("format", openRouterAudioFormat(audioFile))
                                        )
                                )
                        )
                )
            )
        return JSONObject(postJson("https://openrouter.ai/api/v1/chat/completions", apiKey, request))
    }
}

private fun buildAudioTranscriptionPrompt(
    languageHints: String,
    preserveSpokenLanguage: Boolean,
    dictionaryTerms: List<String>
): String = buildString {
    appendLine("Transcribe the attached audio faithfully. Return only the transcript text.")
    appendLine()
    buildAudioLanguageBlock(languageHints, preserveSpokenLanguage)?.let {
        appendLine(it)
        appendLine()
    }
    appendLine("Do not answer questions, summarize, add commentary, include labels, JSON, markdown, explanations, or alternatives.")
    if (dictionaryTerms.isNotEmpty()) {
        appendLine()
        append("Use these spelling constraints when they match the audio: ")
        append(dictionaryTerms.joinToString(", "))
        append(".")
    }
}

internal fun buildAudioDirectPrompt(
    cleanupPolicy: String,
    stylePrompt: String,
    languageHints: String,
    preserveSpokenLanguage: Boolean,
    dictionaryTerms: List<String>
): String = buildString {
    appendLine("Transcribe the attached audio faithfully and return the final insertable text.")
    appendLine()
    buildAudioLanguageBlock(languageHints, preserveSpokenLanguage)?.let {
        appendLine(it)
        appendLine()
    }
    appendLine("Follow these global cleanup rules:")
    append(cleanupPolicy)
    appendLine()
    appendLine()
    appendLine("Apply this formatting style:")
    append(stylePrompt)
    appendLine()
    appendLine()
    appendLine("Return only the final text. Do not include labels, JSON, markdown, explanations, or alternatives.")
    if (dictionaryTerms.isNotEmpty()) {
        appendLine()
        append("Use these spelling constraints when they match the audio: ")
        append(dictionaryTerms.joinToString(", "))
        append(".")
    }
}

internal fun buildAudioTranscriptionPromptPreview(languageHints: String, preserveSpokenLanguage: Boolean, dictionaryPrompt: String?): String =
    buildAudioTranscriptionPrompt(languageHints, preserveSpokenLanguage, dictionaryPrompt?.let { listOf("{{dictionary_terms}}") }.orEmpty())
        .replace("Use these spelling constraints when they match the audio: {{dictionary_terms}}.", dictionaryPrompt.orEmpty())

private fun JSONObject.toModelOption(): ModelOption {
    val id = optString("id")
    return ModelOption(
        id = id,
        name = optString("name", id),
        provider = "OpenRouter",
        contextLength = optInt("context_length").takeIf { it > 0 }
    )
}

private fun JSONObject.supportsText(): Boolean {
    val architecture = optJSONObject("architecture")
    val modality = architecture?.optString("modality").orEmpty().lowercase()
    if (modality.isBlank()) return true
    return supportsTextOutput()
}

internal fun JSONObject.supportsOpenRouterAudioToText(): Boolean =
    supportsOpenRouterAudioToText(
        inputModalities = modalityList("input_modalities", "inputModalities"),
        outputModalities = modalityList("output_modalities", "outputModalities"),
        fallbackModalities = modalityList("modalities", "modalities"),
        modalityPath = modalityPath()
    )

internal fun supportsOpenRouterAudioToText(
    inputModalities: List<String>,
    outputModalities: List<String>,
    fallbackModalities: List<String> = emptyList(),
    modalityPath: String? = null
): Boolean {
    val modalityInput = modalityPath?.substringBefore("->").orEmpty()
    val supportsAudioInput = inputModalities.containsModality("audio") ||
        modalityInput.containsModalityToken("audio") ||
        fallbackModalities.containsModality("audio")
    return supportsAudioInput && supportsOpenRouterTextOutput(outputModalities, fallbackModalities, modalityPath)
}

private fun JSONObject.supportsTextOutput(): Boolean =
    supportsOpenRouterTextOutput(
        outputModalities = modalityList("output_modalities", "outputModalities"),
        fallbackModalities = modalityList("modalities", "modalities"),
        modalityPath = modalityPath()
    )

private fun supportsOpenRouterTextOutput(
    outputModalities: List<String>,
    fallbackModalities: List<String>,
    modalityPath: String?
): Boolean {
    val modalityOutput = modalityPath?.substringAfter("->", missingDelimiterValue = "").orEmpty()
    return outputModalities.containsModality("text") ||
        modalityOutput.containsModalityToken("text") ||
        fallbackModalities.containsModality("text")
}

private fun JSONObject.modalityList(snakeKey: String, camelKey: String): List<String> {
    val architecture = optJSONObject("architecture")
    val targets = listOf(this, architecture).filterNotNull()
    return targets.flatMap { target ->
        listOf(snakeKey, camelKey).flatMap { key ->
            target.optJSONArray(key).toStringList() + target.optString(key).splitModalityTokens()
        }
    }.filter { it.isNotBlank() }
}

private fun JSONObject.modalityPath(): String? =
    optJSONObject("architecture")?.optString("modality")?.takeIf { it.isNotBlank() }
        ?: optString("modality").takeIf { it.isNotBlank() }

private fun String.containsModalityToken(value: String): Boolean =
    splitModalityTokens().any { it.equals(value, ignoreCase = true) }

private fun List<String>.containsModality(value: String): Boolean =
    any { it.equals(value, ignoreCase = true) }

private fun String.splitModalityTokens(): List<String> =
    split('+', ',', ' ', '\t', '\n', '\r').map { it.trim() }.filter { it.isNotBlank() }

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optString(it).takeIf { value -> value.isNotBlank() } }
}

private fun openRouterAudioFormat(file: File): String = when (file.extension.lowercase()) {
    "wav" -> "wav"
    "mp3" -> "mp3"
    "m4a", "mp4" -> "m4a"
    "aac" -> "aac"
    "ogg" -> "ogg"
    "flac" -> "flac"
    "aiff", "aif" -> "aiff"
    else -> "wav"
}
