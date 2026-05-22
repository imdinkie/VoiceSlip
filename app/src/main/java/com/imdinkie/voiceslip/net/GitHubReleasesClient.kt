package com.imdinkie.voiceslip.net

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val LATEST_RELEASE_URL = "https://api.github.com/repos/imdinkie/VoiceSlip/releases/latest"

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val htmlUrl: String,
    val publishedAt: String
)

class GitHubReleasesClient {
    fun latestStableRelease(): GitHubRelease {
        val json = JSONObject(getGitHubJson(LATEST_RELEASE_URL))
        if (json.optBoolean("prerelease", false)) {
            throw IllegalStateException("Latest GitHub release is marked as a prerelease")
        }
        val tagName = json.optString("tag_name").trim()
        val htmlUrl = json.optString("html_url").trim()
        if (tagName.isBlank() || htmlUrl.isBlank()) {
            throw IllegalStateException("GitHub release response is missing tag_name or html_url")
        }
        return GitHubRelease(
            tagName = tagName,
            name = json.optString("name", tagName).trim().ifBlank { tagName },
            htmlUrl = htmlUrl,
            publishedAt = json.optString("published_at").trim()
        )
    }
}

class GitHubReleaseCheckException(message: String) : Exception(message)

internal fun isReleaseNewer(installedVersionName: String, releaseTagName: String): Boolean {
    val installed = VersionParts.parse(installedVersionName) ?: return false
    val release = VersionParts.parse(releaseTagName) ?: return false
    return release > installed
}

private data class VersionParts(val numbers: List<Int>) : Comparable<VersionParts> {
    override fun compareTo(other: VersionParts): Int {
        val width = maxOf(numbers.size, other.numbers.size)
        for (index in 0 until width) {
            val left = numbers.getOrElse(index) { 0 }
            val right = other.numbers.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }

    companion object {
        fun parse(raw: String): VersionParts? {
            val clean = raw.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore("-")
                .substringBefore("+")
            val parts = clean.split('.')
                .map { it.toIntOrNull() ?: return null }
                .takeIf { it.isNotEmpty() }
                ?: return null
            return VersionParts(parts)
        }
    }
}

private fun getGitHubJson(url: String): String {
    val connection = (URL(url).openConnection() as HttpURLConnection)
    connection.requestMethod = "GET"
    connection.connectTimeout = 20_000
    connection.readTimeout = 60_000
    connection.setRequestProperty("Accept", "application/vnd.github+json")
    connection.setRequestProperty("User-Agent", "VoiceSlip Android")
    val responseCode = connection.responseCode
    val body = if (responseCode in 200..299) {
        connection.inputStream.bufferedReader().use { it.readText() }
    } else {
        connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }
    connection.disconnect()
    if (responseCode !in 200..299) {
        throw GitHubReleaseCheckException(gitHubReleaseErrorMessage(responseCode))
    }
    return body
}

private fun gitHubReleaseErrorMessage(responseCode: Int): String =
    when (responseCode) {
        403 -> "GitHub refused the release check. Try again later."
        404 -> "GitHub releases were not found for this app."
        in 500..599 -> "GitHub is temporarily unavailable. Try again later."
        else -> "GitHub release check failed with HTTP $responseCode."
    }
