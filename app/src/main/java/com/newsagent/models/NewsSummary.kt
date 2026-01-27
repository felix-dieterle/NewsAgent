package com.newsagent.models

import com.google.gson.annotations.SerializedName

/**
 * Represents an AI-generated summary of a news article
 */
data class NewsSummary(
    @SerializedName("articleId")
    val articleId: String,
    
    @SerializedName("summary")
    val summary: String,
    
    @SerializedName("keyPoints")
    val keyPoints: List<String>,
    
    @SerializedName("generatedAt")
    val generatedAt: Long,
    
    @SerializedName("audioUrl")
    val audioUrl: String? = null,
    
    @SerializedName("model")
    val model: String = "openrouter"
)
