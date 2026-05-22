package com.imdinkie.voiceslip.net

import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ElevenLabsClient {
    fun transcribe(
        apiKey: String,
        audioFile: File,
        model: String,
        keyterms: List<String>
    ): TranscriptionResult {
        val boundary = "VoiceSlip-${UUID.randomUUID()}"
        val connection = (URL("https://api.elevenlabs.io/v1/speech-to-text").openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 180_000
        connection.setRequestProperty("xi-api-key", apiKey)
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        BufferedOutputStream(connection.outputStream).use { out ->
            out.writeFormField(boundary, "model_id", model)
            out.writeFormField(boundary, "diarize", "false")
            keyterms.filter { it.length < 50 }.take(1000).forEach { term ->
                out.writeFormField(boundary, "keyterms", term)
            }
            out.writeFileField(boundary, "file", audioFile.name, audioMimeType(audioFile), audioFile)
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
            throw IllegalStateException("ElevenLabs transcription failed ($responseCode): ${body.take(500)}")
        }
        val json = JSONObject(body)
        return TranscriptionResult(
            text = json.optString("text").trim(),
            language = json.optString("language_code").ifBlank { null },
            model = model,
            metadata = body
        )
    }
}

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
