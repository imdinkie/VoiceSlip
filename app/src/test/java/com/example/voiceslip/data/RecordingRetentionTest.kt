package com.example.voiceslip.data

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecordingRetentionTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun deletesOldOrphanRecordingFilesButKeepsRecentAndRetainedFiles() {
        val dir = temp.newFolder("recordings")
        val now = 20 * 60 * 1000L
        val retained = recordingFile(dir, "retained.wav", lastModified = 0L)
        val oldOrphan = recordingFile(dir, "old-orphan.wav", lastModified = 0L)
        val recentOrphan = recordingFile(dir, "recent-orphan.wav", lastModified = now - 60_000L)

        cleanupOrphanedRecordingFiles(
            recordingsDir = dir,
            retainedAudioPaths = setOf(retained.absolutePath),
            nowMillis = now,
            minAgeMillis = 10 * 60 * 1000L
        )

        assertTrue(retained.exists())
        assertFalse(oldOrphan.exists())
        assertTrue(recentOrphan.exists())
    }

    private fun recordingFile(dir: File, name: String, lastModified: Long): File =
        File(dir, name).apply {
            writeText("audio")
            setLastModified(lastModified)
        }
}
