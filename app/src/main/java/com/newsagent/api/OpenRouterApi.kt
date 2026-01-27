package com.newsagent.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * API interface for OpenRouter AI service
 * Documentation: https://openrouter.ai/docs
 */
interface OpenRouterApi {
    
    @POST("api/v1/chat/completions")
    suspend fun generateCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://newsagent.app",
        @Header("X-Title") title: String = "NewsAgent",
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

data class ChatRequest(
    @SerializedName("model")
    val model: String = "google/gemini-flash-1.5", // Free tier model
    
    @SerializedName("messages")
    val messages: List<ChatMessage>
)

data class ChatMessage(
    @SerializedName("role")
    val role: String, // "user" or "assistant"
    
    @SerializedName("content")
    val content: String
)

data class ChatResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("choices")
    val choices: List<ChatChoice>,
    
    @SerializedName("usage")
    val usage: Usage?
)

data class ChatChoice(
    @SerializedName("message")
    val message: ChatMessage,
    
    @SerializedName("finish_reason")
    val finishReason: String
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    
    @SerializedName("total_tokens")
    val totalTokens: Int
)
