package com.newsagent.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * GitHub API interface for fetching release information
 */
interface GitHubApi {
    
    /**
     * Get the latest release for a repository
     * @param owner Repository owner (username or organization)
     * @param repo Repository name
     * @return Response containing the latest release information
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRelease>
}

/**
 * Data class representing a GitHub release
 */
data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val assets: List<GitHubAsset>,
    val html_url: String,
    val published_at: String
)

/**
 * Data class representing a GitHub release asset (APK file)
 */
data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long,
    val content_type: String
)
