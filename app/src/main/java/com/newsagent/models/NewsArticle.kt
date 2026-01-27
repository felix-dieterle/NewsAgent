package com.newsagent.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a news article fetched from various sources
 */
data class NewsArticle(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("content")
    val content: String?,
    
    @SerializedName("url")
    val url: String,
    
    @SerializedName("source")
    val source: String,
    
    @SerializedName("publishedAt")
    val publishedAt: String,
    
    @SerializedName("imageUrl")
    val imageUrl: String?,
    
    @SerializedName("author")
    val author: String?,
    
    var summary: NewsSummary? = null,
    var credibilityScore: CredibilityScore? = null,
    var isFavorite: Boolean = false
)
