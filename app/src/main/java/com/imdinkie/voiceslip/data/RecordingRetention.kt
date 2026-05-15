package com.imdinkie.voiceslip.data

import java.io.File

internal const val ORPHAN_RECORDING_MIN_AGE_MS = 10 * 60 * 1000L

internal fun cleanupOrphanedRecordingFiles(
    recordingsDir: File,
    retainedAudioPaths: Set<String>,
    nowMillis: Long = System.currentTimeMillis(),
    minAgeMillis: Long = ORPHAN_RECORDING_MIN_AGE_MS
) {
    recordingsDir.listFiles()
        .orEmpty()
        .filter { it.isFile }
        .filter { it.absolutePath !in retainedAudioPaths }
        .filter { nowMillis - it.lastModified() >= minAgeMillis }
        .forEach { runCatching { it.delete() } }
}
