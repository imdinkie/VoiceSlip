package com.example.voiceslip.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMillis: Long = 0L

    val isRecording: Boolean
        get() = recorder != null

    fun start(file: File) {
        stopInternal(deleteFile = true)
        outputFile = file
        startedAtMillis = System.currentTimeMillis()
        val mediaRecorder = if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setAudioSamplingRate(44_100)
        mediaRecorder.setAudioEncodingBitRate(96_000)
        mediaRecorder.setAudioChannels(1)
        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.prepare()
        mediaRecorder.start()
        recorder = mediaRecorder
    }

    fun maxAmplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    fun stop(): RecordingResult? {
        val file = outputFile ?: return null
        val duration = System.currentTimeMillis() - startedAtMillis
        stopInternal(deleteFile = false)
        return RecordingResult(file, duration)
    }

    fun cancel() {
        stopInternal(deleteFile = true)
    }

    private fun stopInternal(deleteFile: Boolean) {
        val current = recorder
        recorder = null
        runCatching { current?.stop() }
        runCatching { current?.reset() }
        runCatching { current?.release() }
        if (deleteFile) outputFile?.delete()
        outputFile = null
        startedAtMillis = 0L
    }
}

data class RecordingResult(
    val file: File,
    val durationMillis: Long
)

