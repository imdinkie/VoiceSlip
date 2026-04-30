package com.example.voiceslip.net

import com.example.voiceslip.data.ModelOption
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
        val connection = (URL("https://openrouter.ai/api/v1/models").openConnection() as HttpURLConnection)
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
        return (0 until data.length()).mapNotNull { index ->
            val item = data.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id")
            if (id.isBlank()) return@mapNotNull null
            val architecture = item.optJSONObject("architecture")
            val modality = architecture?.optString("modality").orEmpty().lowercase()
            if ("text" !in modality && modality.isNotBlank()) return@mapNotNull null
            ModelOption(
                id = id,
                name = item.optString("name", id),
                provider = "OpenRouter",
                contextLength = item.optInt("context_length").takeIf { it > 0 }
            )
        }.sortedBy { it.name.lowercase() }
    }
}
