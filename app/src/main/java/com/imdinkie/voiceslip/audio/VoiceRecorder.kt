package com.imdinkie.voiceslip.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max

class VoiceRecorder(private val context: Context) {
    private var recorder: AudioRecord? = null
    private var outputFile: File? = null
    private var recordingThread: Thread? = null
    private val recording = AtomicBoolean(false)
    private val peakAmplitude = AtomicInteger(0)
    private var startedAtMillis: Long = 0L

    val isRecording: Boolean
        get() = recording.get()

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(file: File) {
        stopInternal(deleteFile = true)
        outputFile = file
        startedAtMillis = System.currentTimeMillis()
        peakAmplitude.set(0)

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBuffer, SAMPLE_RATE / 2)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            throw IllegalStateException("Could not initialize microphone recorder")
        }

        writeEmptyWavHeader(file)
        recording.set(true)
        audioRecord.startRecording()
        recorder = audioRecord
        recordingThread = Thread {
            writePcmToWav(file, audioRecord, bufferSize)
        }.apply {
            name = "VoiceSlip-WavRecorder"
            start()
        }
    }

    fun maxAmplitude(): Int = peakAmplitude.getAndSet(0)

    fun stop(): RecordingResult? {
        val file = outputFile ?: return null
        val duration = System.currentTimeMillis() - startedAtMillis
        stopInternal(deleteFile = false)
        return RecordingResult(file, duration)
    }

    fun cancel() {
        stopInternal(deleteFile = true)
    }

    private fun writePcmToWav(file: File, audioRecord: AudioRecord, bufferSize: Int) {
        val buffer = ShortArray(bufferSize / BYTES_PER_SAMPLE)
        var dataBytes = 0L
        runCatching {
            RandomAccessFile(file, "rw").use { wav ->
                wav.seek(WAV_HEADER_BYTES.toLong())
                while (recording.get()) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    var localPeak = 0
                    for (index in 0 until read) {
                        val sample = buffer[index].toInt()
                        localPeak = max(localPeak, abs(sample))
                        wav.write(sample and 0xff)
                        wav.write(sample shr 8 and 0xff)
                    }
                    peakAmplitude.accumulateAndGet(localPeak, ::max)
                    dataBytes += read * BYTES_PER_SAMPLE
                }
                wav.seek(0)
                wav.write(wavHeader(dataBytes))
            }
        }
    }

    private fun stopInternal(deleteFile: Boolean) {
        recording.set(false)
        val current = recorder
        recorder = null
        runCatching { current?.stop() }
        runCatching { current?.release() }
        runCatching { recordingThread?.join(1500) }
        recordingThread = null
        if (deleteFile) outputFile?.delete()
        outputFile = null
        startedAtMillis = 0L
    }

    private fun writeEmptyWavHeader(file: File) {
        file.outputStream().use { it.write(wavHeader(dataBytes = 0)) }
    }

    private fun wavHeader(dataBytes: Long): ByteArray {
        val byteRate = SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE
        val blockAlign = CHANNELS * BYTES_PER_SAMPLE
        val totalDataLen = dataBytes + 36
        return ByteArray(WAV_HEADER_BYTES).also { header ->
            header.writeAscii(0, "RIFF")
            header.writeIntLe(4, totalDataLen.toInt())
            header.writeAscii(8, "WAVE")
            header.writeAscii(12, "fmt ")
            header.writeIntLe(16, 16)
            header.writeShortLe(20, 1)
            header.writeShortLe(22, CHANNELS)
            header.writeIntLe(24, SAMPLE_RATE)
            header.writeIntLe(28, byteRate)
            header.writeShortLe(32, blockAlign)
            header.writeShortLe(34, 16)
            header.writeAscii(36, "data")
            header.writeIntLe(40, dataBytes.toInt())
        }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNELS = 1
        const val BYTES_PER_SAMPLE = 2
        const val WAV_HEADER_BYTES = 44
    }
}

data class RecordingResult(
    val file: File,
    val durationMillis: Long
)

private fun ByteArray.writeAscii(offset: Int, value: String) {
    value.forEachIndexed { index, char -> this[offset + index] = char.code.toByte() }
}

private fun ByteArray.writeIntLe(offset: Int, value: Int) {
    this[offset] = (value and 0xff).toByte()
    this[offset + 1] = (value shr 8 and 0xff).toByte()
    this[offset + 2] = (value shr 16 and 0xff).toByte()
    this[offset + 3] = (value shr 24 and 0xff).toByte()
}

private fun ByteArray.writeShortLe(offset: Int, value: Int) {
    this[offset] = (value and 0xff).toByte()
    this[offset + 1] = (value shr 8 and 0xff).toByte()
}
