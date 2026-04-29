package com.example.voiceslip.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class VoiceSlipRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("voiceslip_store", Context.MODE_PRIVATE)

    val recordingsDir: File = File(appContext.filesDir, "recordings").apply { mkdirs() }

    fun getAppEnabled(): Boolean = prefs.getBoolean("app_enabled", true)

    fun setAppEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("app_enabled", enabled).apply()
    }

    fun getHapticsEnabled(): Boolean = prefs.getBoolean("haptics_enabled", false)

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
    }

    fun getBubbleSizeDp(): Int {
        val stored = prefs.getInt("bubble_size_dp", Int.MIN_VALUE)
        if (stored != Int.MIN_VALUE) {
            return stored.coerceIn(BUBBLE_SIZE_MIN_DP, BUBBLE_SIZE_MAX_DP)
        }
        return when (prefs.getString("bubble_size", "MEDIUM")) {
            "SMALL" -> 48
            "LARGE" -> BUBBLE_SIZE_MAX_DP
            else -> BUBBLE_SIZE_DEFAULT_DP
        }
    }

    fun setBubbleSizeDp(sizeDp: Int) {
        prefs.edit().putInt("bubble_size_dp", sizeDp.coerceIn(BUBBLE_SIZE_MIN_DP, BUBBLE_SIZE_MAX_DP)).apply()
    }

    fun getBubbleOpacityPercent(): Int {
        return prefs.getInt("bubble_opacity_percent", BUBBLE_OPACITY_DEFAULT_PERCENT)
            .coerceIn(BUBBLE_OPACITY_MIN_PERCENT, BUBBLE_OPACITY_MAX_PERCENT)
    }

    fun setBubbleOpacityPercent(opacityPercent: Int) {
        prefs.edit()
            .putInt("bubble_opacity_percent", opacityPercent.coerceIn(BUBBLE_OPACITY_MIN_PERCENT, BUBBLE_OPACITY_MAX_PERCENT))
            .apply()
    }

    fun getBubbleX(): Int = prefs.getInt("bubble_x", -1)

    fun getBubbleY(): Int = prefs.getInt("bubble_y", -1)

    fun setBubblePosition(x: Int, y: Int) {
        prefs.edit().putInt("bubble_x", x).putInt("bubble_y", y).apply()
    }

    fun getPipelineConfig(): PipelineConfig {
        return PipelineConfig(
            mode = enumValue(prefs.getString("pipeline_mode", null), PipelineMode.PURE_TRANSCRIPTION),
            transcriptionEngine = enumValue(
                prefs.getString("transcription_engine", null),
                TranscriptionEngineId.MISTRAL_VOXTRAL_MINI_TRANSCRIBE
            ),
            audioDirectEngine = enumValue(
                prefs.getString("audio_direct_engine", null),
                AudioDirectEngineId.MISTRAL_VOXTRAL_SMALL_AUDIO
            ),
            postProcessingProvider = enumValue(
                prefs.getString("post_processing_provider", null),
                PostProcessingProvider.NONE
            ),
            postProcessingModel = prefs.getString("post_processing_model", "").orEmpty(),
            stylePreset = enumValue(prefs.getString("style_preset", null), StylePreset.RAW)
        )
    }

    fun setPipelineConfig(config: PipelineConfig) {
        prefs.edit()
            .putString("pipeline_mode", config.mode.name)
            .putString("transcription_engine", config.transcriptionEngine.name)
            .putString("audio_direct_engine", config.audioDirectEngine.name)
            .putString("post_processing_provider", config.postProcessingProvider.name)
            .putString("post_processing_model", config.postProcessingModel)
            .putString("style_preset", config.stylePreset.name)
            .apply()
    }

    fun getCachedModels(provider: ProviderId): List<ModelOption> {
        val array = JSONArray(prefs.getString("${provider.name.lowercase()}_models", "[]"))
        return (0 until array.length()).mapNotNull { index ->
            runCatching { array.getJSONObject(index).toModelOption() }.getOrNull()
        }.sortedBy { it.name.lowercase() }
    }

    fun setCachedModels(provider: ProviderId, models: List<ModelOption>) {
        val array = JSONArray()
        models.forEach { array.put(it.toJson()) }
        prefs.edit().putString("${provider.name.lowercase()}_models", array.toString()).apply()
    }

    fun listDictionary(): List<DictionaryEntry> {
        val array = JSONArray(prefs.getString("dictionary", "[]"))
        return (0 until array.length()).mapNotNull { index ->
            runCatching { array.getJSONObject(index).toDictionaryEntry() }.getOrNull()
        }.sortedBy { it.phrase.lowercase() }
    }

    fun addDictionaryEntry(phrase: String) {
        val clean = phrase.trim()
        if (clean.isBlank()) return
        val now = System.currentTimeMillis()
        val entries = listDictionary().toMutableList()
        if (entries.any { it.phrase.equals(clean, ignoreCase = true) }) return
        entries += DictionaryEntry(UUID.randomUUID().toString(), clean, now, now)
        saveDictionary(entries)
    }

    fun deleteDictionaryEntry(id: String) {
        saveDictionary(listDictionary().filterNot { it.id == id })
    }

    fun listHistory(): List<HistoryItem> {
        val array = JSONArray(prefs.getString("history", "[]"))
        return (0 until array.length()).mapNotNull { index ->
            runCatching { array.getJSONObject(index).toHistoryItem() }.getOrNull()
        }.sortedByDescending { it.createdAtMillis }
    }

    fun upsertHistory(item: HistoryItem) {
        val existing = listHistory().filterNot { it.id == item.id }.toMutableList()
        existing += item
        saveHistory(existing.sortedByDescending { it.createdAtMillis })
    }

    fun deleteHistory(id: String) {
        val item = listHistory().firstOrNull { it.id == id }
        if (item != null) runCatching { File(item.audioPath).delete() }
        saveHistory(listHistory().filterNot { it.id == id })
    }

    fun clearHistory() {
        listHistory().forEach { runCatching { File(it.audioPath).delete() } }
        prefs.edit().putString("history", "[]").apply()
    }

    private fun saveDictionary(entries: List<DictionaryEntry>) {
        val array = JSONArray()
        entries.forEach { array.put(it.toJson()) }
        prefs.edit().putString("dictionary", array.toString()).apply()
    }

    private fun saveHistory(items: List<HistoryItem>) {
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        prefs.edit().putString("history", array.toString()).apply()
    }
}

private fun DictionaryEntry.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("phrase", phrase)
    .put("createdAtMillis", createdAtMillis)
    .put("updatedAtMillis", updatedAtMillis)

private fun JSONObject.toDictionaryEntry(): DictionaryEntry = DictionaryEntry(
    id = getString("id"),
    phrase = getString("phrase"),
    createdAtMillis = optLong("createdAtMillis", 0L),
    updatedAtMillis = optLong("updatedAtMillis", 0L)
)

private fun HistoryItem.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("createdAtMillis", createdAtMillis)
    .put("audioPath", audioPath)
    .put("durationMillis", durationMillis)
    .put("status", status.name)
    .put("transcript", transcript)
    .put("rawTranscript", rawTranscript)
    .put("finalText", finalText)
    .put("detectedLanguage", detectedLanguage)
    .put("error", error)
    .put("provider", provider)
    .put("model", model)
    .put("pipelineMode", pipelineMode)
    .put("transcriptionProvider", transcriptionProvider)
    .put("transcriptionModel", transcriptionModel)
    .put("audioModelProvider", audioModelProvider)
    .put("audioModel", audioModel)
    .put("postProcessingProvider", postProcessingProvider)
    .put("postProcessingModel", postProcessingModel)
    .put("stylePreset", stylePreset)
    .put("pipelineSummary", pipelineSummary)
    .put("errorStage", errorStage)
    .put("metadataJson", metadataJson)
    .put("retryCount", retryCount)

private fun JSONObject.toHistoryItem(): HistoryItem = HistoryItem(
    id = getString("id"),
    createdAtMillis = getLong("createdAtMillis"),
    audioPath = getString("audioPath"),
    durationMillis = optLong("durationMillis", 0L),
    status = runCatching { RecordingStatus.valueOf(getString("status")) }.getOrDefault(RecordingStatus.FAILED),
    transcript = optString("transcript").ifBlank { null },
    rawTranscript = optString("rawTranscript").ifBlank { optString("transcript").ifBlank { null } },
    finalText = optString("finalText").ifBlank { optString("transcript").ifBlank { null } },
    detectedLanguage = optString("detectedLanguage").ifBlank { null },
    error = optString("error").ifBlank { null },
    provider = optString("provider", "mistral"),
    model = optString("model", "voxtral-mini-latest"),
    pipelineMode = optString("pipelineMode", PipelineMode.PURE_TRANSCRIPTION.name),
    transcriptionProvider = optString("transcriptionProvider").ifBlank { null },
    transcriptionModel = optString("transcriptionModel").ifBlank { null },
    audioModelProvider = optString("audioModelProvider").ifBlank { null },
    audioModel = optString("audioModel").ifBlank { null },
    postProcessingProvider = optString("postProcessingProvider").ifBlank { null },
    postProcessingModel = optString("postProcessingModel").ifBlank { null },
    stylePreset = optString("stylePreset", StylePreset.RAW.name),
    pipelineSummary = optString("pipelineSummary").ifBlank { null },
    errorStage = optString("errorStage").ifBlank { null },
    metadataJson = optString("metadataJson").ifBlank { null },
    retryCount = optInt("retryCount", 0)
)

private fun ModelOption.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("provider", provider)
    .put("contextLength", contextLength)

private fun JSONObject.toModelOption(): ModelOption = ModelOption(
    id = getString("id"),
    name = optString("name", getString("id")),
    provider = optString("provider"),
    contextLength = if (has("contextLength") && !isNull("contextLength")) optInt("contextLength") else null
)

private inline fun <reified T : Enum<T>> enumValue(name: String?, default: T): T {
    if (name.isNullOrBlank()) return default
    return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
}
