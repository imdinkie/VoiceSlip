package com.imdinkie.voiceslip.audio

import com.imdinkie.voiceslip.data.AudioDirectEngineId
import com.imdinkie.voiceslip.data.EngineKind
import com.imdinkie.voiceslip.data.PipelineConfig
import com.imdinkie.voiceslip.data.PipelineMode
import com.imdinkie.voiceslip.data.TranscriptionEngineId
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFileFormatTest {
    @Test
    fun mistralAudioChatRecordsWav() {
        val config = PipelineConfig(
            mode = PipelineMode.AUDIO_DIRECT,
            audioDirectEngine = AudioDirectEngineId.MISTRAL_VOXTRAL_SMALL_AUDIO
        )

        assertEquals(AudioFileFormat.WAV, recordingFormatFor(config))
    }

    @Test
    fun transcriptionEndpointsRecordM4a() {
        val config = PipelineConfig(
            mode = PipelineMode.PURE_TRANSCRIPTION,
            transcriptionEngine = TranscriptionEngineId.ELEVENLABS_SCRIBE_V2
        )

        assertEquals(AudioFileFormat.M4A, recordingFormatFor(config))
    }

    @Test
    fun openRouterAudioRecordsM4a() {
        val config = PipelineConfig(
            mode = PipelineMode.PURE_TRANSCRIPTION,
            transcriptionEngineKind = EngineKind.OPENROUTER_AUDIO,
            openRouterAudioTranscriptionModel = "google/gemini-3-flash-preview"
        )

        assertEquals(AudioFileFormat.M4A, recordingFormatFor(config))
    }
}
