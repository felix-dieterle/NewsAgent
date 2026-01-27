package com.newsagent.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API interface for news credibility checking
 * This is a placeholder interface that can be implemented with various fact-checking APIs
 */
interface CredibilityApi {
    
    @POST("api/v1/check")
    suspend fun checkCredibility(
        @Body request: CredibilityRequest
    ): Response<CredibilityResponse>
}

data class CredibilityRequest(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("source")
    val source: String,
    
    @SerializedName("url")
    val url: String
)

data class CredibilityResponse(
    @SerializedName("score")
    val score: Float,
    
    @SerializedName("factors")
    val factors: Map<String, Float>,
    
    @SerializedName("verified")
    val verified: Boolean,
    
    @SerializedName("concerns")
    val concerns: List<String>,
    
    @SerializedName("details")
    val details: String?
)
