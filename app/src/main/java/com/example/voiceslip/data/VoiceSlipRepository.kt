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

    fun getHapticsEnabled(): Boolean = prefs.getBoolean("haptics_enabled", false)

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
    }

    fun getBubbleSize(): BubbleSize {
        val stored = prefs.getString("bubble_size", BubbleSize.MEDIUM.name).orEmpty()
        return BubbleSize.entries.firstOrNull { it.name == stored } ?: BubbleSize.MEDIUM
    }

    fun setBubbleSize(size: BubbleSize) {
        prefs.edit().putString("bubble_size", size.name).apply()
    }

    fun getBubbleX(): Int = prefs.getInt("bubble_x", -1)

    fun getBubbleY(): Int = prefs.getInt("bubble_y", -1)

    fun setBubblePosition(x: Int, y: Int) {
        prefs.edit().putInt("bubble_x", x).putInt("bubble_y", y).apply()
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
    .put("error", error)
    .put("provider", provider)
    .put("model", model)
    .put("retryCount", retryCount)

private fun JSONObject.toHistoryItem(): HistoryItem = HistoryItem(
    id = getString("id"),
    createdAtMillis = getLong("createdAtMillis"),
    audioPath = getString("audioPath"),
    durationMillis = optLong("durationMillis", 0L),
    status = runCatching { RecordingStatus.valueOf(getString("status")) }.getOrDefault(RecordingStatus.FAILED),
    transcript = optString("transcript").ifBlank { null },
    error = optString("error").ifBlank { null },
    provider = optString("provider", "mistral"),
    model = optString("model", "voxtral-mini-latest"),
    retryCount = optInt("retryCount", 0)
)
