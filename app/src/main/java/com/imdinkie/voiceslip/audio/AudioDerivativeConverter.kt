package com.imdinkie.voiceslip.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.RandomAccessFile

class AudioDerivativeConverter {
    fun fileForUpload(original: File, requiredFormat: AudioFileFormat): File {
        val originalFormat = audioFileFormat(original)
        if (originalFormat == requiredFormat) return original
        if (originalFormat == AudioFileFormat.M4A && requiredFormat == AudioFileFormat.WAV) {
            val derived = derivedAudioFile(original, AudioFileFormat.WAV)
            if (derived.exists() && derived.length() > WAV_HEADER_BYTES) return derived
            convertM4aToWav(original, derived)
            return derived
        }
        return original
    }

    private fun convertM4aToWav(input: File, output: File) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(input.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")
            } ?: throw IllegalStateException("No audio track found for conversion.")
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: throw IllegalStateException("Audio MIME type missing.")
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            extractor.selectTrack(trackIndex)
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }
            decodeToWav(extractor, codec!!, output, sampleRate, channels)
        } catch (error: Throwable) {
            output.delete()
            throw error
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun decodeToWav(
        extractor: MediaExtractor,
        codec: MediaCodec,
        output: File,
        sampleRate: Int,
        channels: Int
    ) {
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var dataBytes = 0L
        RandomAccessFile(output, "rw").use { wav ->
            wav.setLength(0)
            wav.write(wavHeader(0, sampleRate, channels))
            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex) ?: throw IllegalStateException("Decoder input buffer missing.")
                        val size = extractor.readSampleData(inputBuffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outputIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outputIndex >= 0 -> {
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        if (info.size > 0) {
                            val outputBuffer = codec.getOutputBuffer(outputIndex) ?: throw IllegalStateException("Decoder output buffer missing.")
                            outputBuffer.position(info.offset)
                            outputBuffer.limit(info.offset + info.size)
                            val bytes = ByteArray(info.size)
                            outputBuffer.get(bytes)
                            wav.write(bytes)
                            dataBytes += bytes.size
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                }
            }
            wav.seek(0)
            wav.write(wavHeader(dataBytes, sampleRate, channels))
        }
    }

    private fun wavHeader(dataBytes: Long, sampleRate: Int, channels: Int): ByteArray {
        val byteRate = sampleRate * channels * BYTES_PER_SAMPLE
        val blockAlign = channels * BYTES_PER_SAMPLE
        val totalDataLen = dataBytes + 36
        return ByteArray(WAV_HEADER_BYTES.toInt()).also { header ->
            header.writeAscii(0, "RIFF")
            header.writeIntLe(4, totalDataLen.toInt())
            header.writeAscii(8, "WAVE")
            header.writeAscii(12, "fmt ")
            header.writeIntLe(16, 16)
            header.writeShortLe(20, 1)
            header.writeShortLe(22, channels)
            header.writeIntLe(24, sampleRate)
            header.writeIntLe(28, byteRate)
            header.writeShortLe(32, blockAlign)
            header.writeShortLe(34, 16)
            header.writeAscii(36, "data")
            header.writeIntLe(40, dataBytes.toInt())
        }
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
        const val BYTES_PER_SAMPLE = 2
        const val WAV_HEADER_BYTES = 44L
    }
}

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
