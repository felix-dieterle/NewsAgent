package com.newsagent.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API interface for free news sources that don't require authentication
 * Uses public RSS feeds and free APIs
 */
interface FreeNewsApi {
    
    /**
     * Search news using GNews API
     * Free tier: 100 requests per day
     * Requires API token from gnews.io (free registration)
     */
    @GET("api/v4/search")
    suspend fun searchGNews(
        @Query("q") query: String,
        @Query("token") apiToken: String,
        @Query("lang") language: String = "de",
        @Query("max") maxResults: Int = 10,
        @Query("country") country: String = "de"
    ): Response<GNewsResponse>
    
    /**
     * Get top headlines using GNews API
     * Requires API token from gnews.io (free registration)
     */
    @GET("api/v4/top-headlines")
    suspend fun getGNewsHeadlines(
        @Query("token") apiToken: String,
        @Query("lang") language: String = "de",
        @Query("max") maxResults: Int = 10,
        @Query("country") country: String = "de"
    ): Response<GNewsResponse>
}

/**
 * Response from GNews API
 */
data class GNewsResponse(
    @SerializedName("totalArticles")
    val totalArticles: Int,
    
    @SerializedName("articles")
    val articles: List<GNewsArticle>
)

/**
 * Article from GNews API
 */
data class GNewsArticle(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("content")
    val content: String?,
    
    @SerializedName("url")
    val url: String,
    
    @SerializedName("image")
    val image: String?,
    
    @SerializedName("publishedAt")
    val publishedAt: String,
    
    @SerializedName("source")
    val source: GNewsSource
)

/**
 * News source from GNews API
 */
data class GNewsSource(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("url")
    val url: String?
)
