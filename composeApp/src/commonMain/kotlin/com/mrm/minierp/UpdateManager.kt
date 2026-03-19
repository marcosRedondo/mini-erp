package com.mrm.minierp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.mrm.minierp.generated.BuildInfo

@Serializable
data class GitHubRelease(
    val tag_name: String
)

object UpdateManager {
    private val client by lazy { HttpClient() }
    private val json = Json { ignoreUnknownKeys = true }
    
    val APP_VERSION = BuildInfo.APP_VERSION
    private const val REPO_URL = "https://api.github.com/repos/marcosRedondo/mini-erp/releases/latest"
    const val DOWNLOAD_URL = "https://github.com/marcosRedondo/mini-erp/releases/latest"

    suspend fun getLatestVersion(): String? = withContext(Dispatchers.Default) {
        try {
            println("Checking for updates at $REPO_URL")
            val response: HttpResponse = client.get(REPO_URL)
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val release = json.decodeFromString<GitHubRelease>(body)
                release.tag_name.removePrefix("v")
            } else {
                println("Update check failed with status: ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("Error checking for updates: ${e.message}")
            e.printStackTrace()
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
