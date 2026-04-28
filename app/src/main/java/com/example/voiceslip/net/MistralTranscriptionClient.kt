package com.example.voiceslip.net

import com.example.voiceslip.data.AudioDirectEngineId
import com.example.voiceslip.data.StylePreset
import com.example.voiceslip.data.TranscriptionEngineId
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class MistralTranscriptionClient {
    fun transcribe(
        apiKey: String,
        audioFile: File,
        contextBias: List<String>,
        model: String = "voxtral-mini-latest"
    ): TranscriptionResult {
        val boundary = "VoiceSlip-${UUID.randomUUID()}"
        val connection = (URL("https://api.mistral.ai/v1/audio/transcriptions").openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 180_000
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        BufferedOutputStream(connection.outputStream).use { out ->
            out.writeFormField(boundary, "model", model)
            out.writeFormField(boundary, "diarize", "false")
            contextBias.take(100).forEach { phrase ->
                out.writeFormField(boundary, "context_bias", phrase)
            }
            out.writeFileField(boundary, "file", audioFile.name, "audio/mp4", audioFile)
            out.write("--$boundary--\r\n".toByteArray())
        }

        val responseCode = connection.responseCode
        val body = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()

        if (responseCode !in 200..299) {
            throw IllegalStateException("Mistral transcription failed ($responseCode): ${body.take(500)}")
        }

        val json = JSONObject(body)
        return TranscriptionResult(
            text = json.optString("text").trim(),
            language = json.optString("language").ifBlank { null },
            model = json.optString("model", model),
            metadata = body
        )
    }

    fun transcribeWithAudioChat(
        apiKey: String,
        audioFile: File,
        engine: TranscriptionEngineId,
        dictionaryTerms: List<String>
    ): TranscriptionResult {
        val prompt = buildString {
            append("Transcribe the attached audio exactly. Return only a JSON object with keys ")
            append("\"transcript\" and \"language\". Do not answer questions, summarize, or add commentary. ")
            if (dictionaryTerms.isNotEmpty()) {
                append("Use these spelling constraints when they match the audio: ")
                append(dictionaryTerms.take(100).joinToString(", "))
                append(".")
            }
        }
        val json = postAudioChat(apiKey, engine.model, audioFile, prompt, jsonMode = true)
        val content = json.chatContent()
        val parsed = runCatching { JSONObject(content) }.getOrNull()
        val transcript = parsed?.optString("transcript")?.trim().orEmpty().ifBlank { content.trim() }
        return TranscriptionResult(
            text = transcript,
            language = parsed?.optString("language")?.ifBlank { null },
            model = json.optString("model", engine.model),
            metadata = json.toString()
        )
    }

    fun directAudio(
        apiKey: String,
        audioFile: File,
        engine: AudioDirectEngineId,
        stylePreset: StylePreset,
        dictionaryTerms: List<String>
    ): DirectAudioResult {
        val prompt = buildString {
            append("Listen to the attached audio and return the final text to insert. ")
            append(styleInstruction(stylePreset))
            append(" Preserve meaning, names, numbers, dates, URLs, email addresses, code-like tokens, and language. ")
            append("Do not answer the dictated text and do not add facts. ")
            append("Return only a JSON object with keys \"final_text\" and \"language\". ")
            if (dictionaryTerms.isNotEmpty()) {
                append("Use these spelling constraints when they match the audio: ")
                append(dictionaryTerms.take(100).joinToString(", "))
                append(".")
            }
        }
        val json = postAudioChat(apiKey, engine.model, audioFile, prompt, jsonMode = true)
        val content = json.chatContent()
        val parsed = runCatching { JSONObject(content) }.getOrNull()
        return DirectAudioResult(
            finalText = parsed?.optString("final_text")?.trim().orEmpty().ifBlank { content.trim() },
            language = parsed?.optString("language")?.ifBlank { null },
            model = json.optString("model", engine.model),
            metadata = json.toString()
        )
    }

    private fun postAudioChat(
        apiKey: String,
        model: String,
        audioFile: File,
        prompt: String,
        jsonMode: Boolean
    ): JSONObject {
        val inputAudio = uploadAudioAndGetSignedUrl(apiKey, audioFile)
        val request = JSONObject()
            .put("model", model)
            .put(
                "messages",
                org.json.JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "content",
                            org.json.JSONArray()
                                .put(JSONObject().put("type", "input_audio").put("input_audio", inputAudio))
                                .put(JSONObject().put("type", "text").put("text", prompt))
                        )
                )
            )
        if (jsonMode) request.put("response_format", JSONObject().put("type", "json_object"))
        val body = postJson("https://api.mistral.ai/v1/chat/completions", apiKey, request)
        return JSONObject(body)
    }

    private fun uploadAudioAndGetSignedUrl(apiKey: String, audioFile: File): String {
        val boundary = "VoiceSlip-${UUID.randomUUID()}"
        val connection = (URL("https://api.mistral.ai/v1/files").openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 180_000
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        BufferedOutputStream(connection.outputStream).use { out ->
            out.writeFormField(boundary, "purpose", "audio")
            out.writeFileField(boundary, "file", audioFile.name, "audio/mp4", audioFile)
            out.write("--$boundary--\r\n".toByteArray())
        }
        val responseCode = connection.responseCode
        val body = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()
        if (responseCode !in 200..299) {
            throw IllegalStateException("Mistral audio upload failed ($responseCode): ${body.take(500)}")
        }
        val fileId = JSONObject(body).getString("id")
        val signedUrlBody = getJson("https://api.mistral.ai/v1/files/$fileId/url?expiry=24", apiKey)
        return JSONObject(signedUrlBody).getString("url")
    }
}

data class TranscriptionResult(
    val text: String,
    val language: String?,
    val model: String,
    val metadata: String? = null
)

data class DirectAudioResult(
    val finalText: String,
    val language: String?,
    val model: String,
    val metadata: String? = null
)

private fun BufferedOutputStream.writeFormField(boundary: String, name: String, value: String) {
    write("--$boundary\r\n".toByteArray())
    write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
    write(value.toByteArray(Charsets.UTF_8))
    write("\r\n".toByteArray())
}

private fun BufferedOutputStream.writeFileField(
    boundary: String,
    name: String,
    fileName: String,
    contentType: String,
    file: File
) {
    write("--$boundary\r\n".toByteArray())
    write("Content-Disposition: form-data; name=\"$name\"; filename=\"$fileName\"\r\n".toByteArray())
    write("Content-Type: $contentType\r\n\r\n".toByteArray())
    file.inputStream().use { it.copyTo(this) }
    write("\r\n".toByteArray())
}

internal fun postJson(url: String, apiKey: String, request: JSONObject): String {
    val connection = (URL(url).openConnection() as HttpURLConnection)
    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.connectTimeout = 20_000
    connection.readTimeout = 180_000
    connection.setRequestProperty("Authorization", "Bearer $apiKey")
    connection.setRequestProperty("Content-Type", "application/json")
    connection.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
    val responseCode = connection.responseCode
    val body = if (responseCode in 200..299) {
        connection.inputStream.bufferedReader().use { it.readText() }
    } else {
        connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }
    connection.disconnect()
    if (responseCode !in 200..299) {
        throw IllegalStateException("Request failed ($responseCode): ${body.take(500)}")
    }
    return body
}

internal fun getJson(url: String, apiKey: String): String {
    val connection = (URL(url).openConnection() as HttpURLConnection)
    connection.requestMethod = "GET"
    connection.connectTimeout = 20_000
    connection.readTimeout = 60_000
    connection.setRequestProperty("Authorization", "Bearer $apiKey")
    connection.setRequestProperty("Accept", "application/json")
    val responseCode = connection.responseCode
    val body = if (responseCode in 200..299) {
        connection.inputStream.bufferedReader().use { it.readText() }
    } else {
        connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }
    connection.disconnect()
    if (responseCode !in 200..299) {
        throw IllegalStateException("Request failed ($responseCode): ${body.take(500)}")
    }
    return body
}

internal fun JSONObject.chatContent(): String {
    val message = getJSONArray("choices").getJSONObject(0).getJSONObject("message")
    val content = message.opt("content")
    if (content is String) return content
    if (content is org.json.JSONArray) {
        val builder = StringBuilder()
        for (index in 0 until content.length()) {
            val item = content.optJSONObject(index) ?: continue
            if (item.optString("type") == "text") builder.append(item.optString("text"))
        }
        return builder.toString()
    }
    return ""
}

internal fun styleInstruction(stylePreset: StylePreset): String = when (stylePreset) {
    StylePreset.RAW -> "Keep the text raw, with only provider-normal punctuation."
    StylePreset.CLEAN -> "Clean punctuation, casing, obvious filler words, and light grammar only."
    StylePreset.CASUAL -> "Make it natural and casual without changing facts."
    StylePreset.FORMAL -> "Make it polished and professional without changing facts."
    StylePreset.EXCITED -> "Make it energetic without changing facts."
}
