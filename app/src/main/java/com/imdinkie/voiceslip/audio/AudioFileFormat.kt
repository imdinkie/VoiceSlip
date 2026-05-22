package com.imdinkie.voiceslip.audio

import android.media.MediaExtractor
import android.media.MediaFormat
import com.imdinkie.voiceslip.data.EngineKind
import com.imdinkie.voiceslip.data.PipelineConfig
import com.imdinkie.voiceslip.data.PipelineMode
import com.imdinkie.voiceslip.data.ProviderId
import java.io.File
import java.util.Locale

enum class AudioFileFormat(
    val extension: String,
    val label: String
) {
    WAV("wav", "WAV"),
    M4A("m4a", "M4A/AAC")
}

fun recordingFormatFor(config: PipelineConfig): AudioFileFormat =
    when (config.mode) {
        PipelineMode.AUDIO_DIRECT -> if (config.audioDirectProvider() == ProviderId.MISTRAL) AudioFileFormat.WAV else AudioFileFormat.M4A
        PipelineMode.PURE_TRANSCRIPTION,
        PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING -> {
            if (
                config.transcriptionEngineKind == EngineKind.BUILT_IN &&
                config.transcriptionEngine.provider == ProviderId.MISTRAL &&
                config.transcriptionEngine.audioChat
            ) {
                AudioFileFormat.WAV
            } else {
                AudioFileFormat.M4A
            }
        }
    }

fun requiredUploadFormatFor(config: PipelineConfig): AudioFileFormat =
    recordingFormatFor(config)

fun audioFileFormat(file: File): AudioFileFormat? =
    when (file.extension.lowercase(Locale.US)) {
        "wav" -> AudioFileFormat.WAV
        "m4a", "mp4", "aac" -> AudioFileFormat.M4A
        else -> null
    }

fun derivedAudioFile(original: File, format: AudioFileFormat): File =
    File(original.parentFile ?: File("."), "${original.nameWithoutExtension}.derived.${format.extension}")

fun audioDurationMillis(file: File): Long? {
    val format = audioFileFormat(file) ?: return null
    if (format == AudioFileFormat.WAV) return wavDurationMillis(file)
    val extractor = MediaExtractor()
    return runCatching {
        extractor.setDataSource(file.absolutePath)
        for (index in 0 until extractor.trackCount) {
            val track = extractor.getTrackFormat(index)
            if (track.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) {
                val micros = track.getLong(MediaFormat.KEY_DURATION)
                return@runCatching micros / 1000L
            }
        }
        null
    }.getOrNull().also {
        runCatching { extractor.release() }
    }
}

private fun wavDurationMillis(file: File): Long? {
    if (file.length() <= WAV_HEADER_BYTES) return null
    return ((file.length() - WAV_HEADER_BYTES) * 1000L) / WAV_BYTES_PER_SECOND
}

private const val WAV_HEADER_BYTES = 44L
private const val WAV_BYTES_PER_SECOND = 16_000L * 1 * 2
