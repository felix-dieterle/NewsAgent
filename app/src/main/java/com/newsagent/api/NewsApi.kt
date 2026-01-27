package com.newsagent.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API interface for fetching news articles
 * Using News API as an example: https://newsapi.org/
 */
interface NewsApi {
    
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("apiKey") apiKey: String,
        @Query("country") country: String = "de",
        @Query("pageSize") pageSize: Int = 10
    ): Response<NewsResponse>
    
    @GET("v2/everything")
    suspend fun searchNews(
        @Query("apiKey") apiKey: String,
        @Query("q") query: String,
        @Query("language") language: String = "de",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 10
    ): Response<NewsResponse>
}

data class NewsResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("totalResults")
    val totalResults: Int,
    
    @SerializedName("articles")
    val articles: List<NewsApiArticle>
)

data class NewsApiArticle(
    @SerializedName("source")
    val source: NewsSource,
    
    @SerializedName("author")
    val author: String?,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("url")
    val url: String,
    
    @SerializedName("urlToImage")
    val urlToImage: String?,
    
    @SerializedName("publishedAt")
    val publishedAt: String,
    
    @SerializedName("content")
    val content: String?
)

data class NewsSource(
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("name")
    val name: String
)
