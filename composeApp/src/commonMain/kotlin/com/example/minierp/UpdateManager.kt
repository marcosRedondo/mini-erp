package com.example.minierp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class GitHubRelease(
    val tag_name: String
)

object UpdateManager {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    const val APP_VERSION = "1.0.0"
    private const val REPO_URL = "https://api.github.com/repos/marcosRedondo/mini-erp/releases/latest"
    const val DOWNLOAD_URL = "https://github.com/marcosRedondo/mini-erp/releases/latest"

    suspend fun getLatestVersion(): String? = withContext(Dispatchers.Default) {
        try {
            val response: HttpResponse = client.get(REPO_URL)
            val release = json.decodeFromString<GitHubRelease>(response.bodyAsText())
            release.tag_name.removePrefix("v")
        } catch (e: Exception) {
            null
        }
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        
        for (i in 0 until minOf(currentParts.size, latestParts.size)) {
            if (latestParts[i] > currentParts[i]) return true
            if (latestParts[i] < currentParts[i]) return false
        }
        return latestParts.size > currentParts.size
    }
}
