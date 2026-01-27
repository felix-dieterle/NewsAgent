package com.newsagent.models

import com.google.gson.annotations.SerializedName

/**
 * Represents the credibility score of a news article
 */
data class CredibilityScore(
    @SerializedName("articleId")
    val articleId: String,
    
    @SerializedName("score")
    val score: Float, // 0.0 to 1.0
    
    @SerializedName("factors")
    val factors: Map<String, Float>,
    
    @SerializedName("verified")
    val verified: Boolean,
    
    @SerializedName("concerns")
    val concerns: List<String>,
    
    @SerializedName("checkedAt")
    val checkedAt: Long
)
