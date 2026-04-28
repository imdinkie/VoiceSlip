package com.example.voiceslip.data

enum class RecordingStatus {
    RECORDING,
    TRANSCRIBING,
    SUCCEEDED,
    FAILED,
    CANCELED
}

enum class BubbleSize(val label: String, val dp: Int) {
    SMALL("Small", 56),
    MEDIUM("Medium", 64),
    LARGE("Large", 76)
}

data class HistoryItem(
    val id: String,
    val createdAtMillis: Long,
    val audioPath: String,
    val durationMillis: Long,
    val status: RecordingStatus,
    val transcript: String? = null,
    val error: String? = null,
    val provider: String = "mistral",
    val model: String = "voxtral-mini-latest",
    val retryCount: Int = 0
)

data class DictionaryEntry(
    val id: String,
    val phrase: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
