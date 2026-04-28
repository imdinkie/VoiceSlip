package com.example.voiceslip.net

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
        contextBias: List<String>
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
            out.writeFormField(boundary, "model", "voxtral-mini-latest")
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
            model = json.optString("model", "voxtral-mini-latest")
        )
    }
}

data class TranscriptionResult(
    val text: String,
    val language: String?,
    val model: String
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

