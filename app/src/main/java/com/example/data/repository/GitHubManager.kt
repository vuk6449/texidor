package com.example.data.repository

import com.example.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class GitHubManager(private val projectManager: ProjectManager) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun cloneRepository(
        repoUrlOrSlug: String,
        token: String? = null,
        onProgress: (String) -> Unit = {}
    ): Result<Project> = withContext(Dispatchers.IO) {
        try {
            onProgress("Parsing repository coordinates...")
            val (owner, repo) = parseOwnerAndRepo(repoUrlOrSlug)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid GitHub URL or slug. Expected: 'owner/repo' or 'https://github.com/owner/repo'"))

            onProgress("Connecting to GitHub API for $owner/$repo...")
            val apiUrl = "https://api.github.com/repos/$owner/$repo/zipball/HEAD"

            val requestBuilder = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "Texidor-IDE-Android")
                .header("Accept", "application/vnd.github.v3+json")

            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                // If HEAD fails, fallback to direct archive
                val fallbackUrl = "https://github.com/$owner/$repo/archive/refs/heads/main.zip"
                val fallbackResp = client.newCall(
                    Request.Builder()
                        .url(fallbackUrl)
                        .header("User-Agent", "Texidor-IDE-Android")
                        .build()
                ).execute()

                if (!fallbackResp.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("GitHub returned HTTP ${response.code}: ${response.message}")
                    )
                }

                onProgress("Downloading and extracting project files...")
                val bodyStream = fallbackResp.body?.byteStream()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val project = projectManager.importProjectFromZip(repo, ZipInputStream(bodyStream))
                return@withContext Result.success(
                    project.copy(
                        gitBranch = "main",
                        gitRemoteUrl = "https://github.com/$owner/$repo"
                    )
                )
            }

            onProgress("Downloading and extracting project files...")
            val bodyStream = response.body?.byteStream()
                ?: return@withContext Result.failure(IOException("Empty response body from GitHub"))

            val project = projectManager.importProjectFromZip(repo, ZipInputStream(bodyStream))
            Result.success(
                project.copy(
                    gitBranch = "main",
                    gitRemoteUrl = "https://github.com/$owner/$repo"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGist(
        token: String,
        description: String,
        files: Map<String, String>,
        isPublic: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (token.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("GitHub Personal Access Token required"))
            }

            val filesObj = JSONObject()
            for ((name, content) in files) {
                val fObj = JSONObject().apply {
                    put("content", content)
                }
                filesObj.put(name, fObj)
            }

            val payload = JSONObject().apply {
                put("description", description)
                put("public", isPublic)
                put("files", filesObj)
            }

            val request = Request.Builder()
                .url("https://api.github.com/gists")
                .header("User-Agent", "Texidor-IDE-Android")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("GitHub error: ${response.code} ${response.message}"))
            }

            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val htmlUrl = json.optString("html_url", "")
            Result.success(htmlUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseOwnerAndRepo(input: String): Pair<String, String>? {
        var clean = input.trim()
            .removePrefix("git@github.com:")
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removeSuffix(".git")
            .removeSuffix("/")

        val parts = clean.split('/')
        return if (parts.size >= 2) {
            Pair(parts[parts.size - 2], parts[parts.size - 1])
        } else {
            null
        }
    }
}
