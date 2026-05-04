package com.example.voiceslip.net

internal fun buildAudioLanguageBlock(languageHints: String, preserveSpokenLanguage: Boolean): String? {
    if (!preserveSpokenLanguage) return null
    val examples = buildLanguageHintExamples(languageHints)
    return buildString {
        appendLine("Language:")
        appendLine("Transcribe in the language spoken in the audio. Do not translate under any circumstance.")
        append("If multiple languages are spoken, keep each part in its spoken language.")
        if (examples.isNotBlank()) {
            appendLine()
            append(examples)
        }
    }
}

internal fun buildPostProcessingLanguageBlock(detectedLanguage: String?, preserveSpokenLanguage: Boolean): String? {
    if (!preserveSpokenLanguage) return null
    val cleanLanguage = detectedLanguage?.trim().orEmpty()
    return if (cleanLanguage.isBlank()) {
        "Language:\nKeep the output in the same language as the transcript. Do not translate under any circumstance."
    } else {
        "Language:\nKeep the output in the detected/spoken language. Detected language: $cleanLanguage. Do not translate under any circumstance."
    }
}

internal fun buildLanguageHintExamples(languageHints: String): String = languageHints
    .split(',')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .joinToString(" ") { language -> "If the audio is $language, output $language." }
