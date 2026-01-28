package com.newsagent.services

import android.content.Context
import android.content.SharedPreferences
import com.newsagent.api.*
import com.newsagent.cache.CacheManager
import com.newsagent.models.CredibilityScore
import com.newsagent.models.NewsArticle
import com.newsagent.models.NewsSummary
import com.newsagent.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Service for generating AI summaries using OpenRouter
 * Implements caching to reduce redundant API calls and costs
 */
class AiSummaryService(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("newsagent_prefs", Context.MODE_PRIVATE)
    private val cacheManager = CacheManager.getInstance()
    
    private val openRouterApi: OpenRouterApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // Only log basic info to reduce overhead
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenRouterApi::class.java)
    }
    
    /**
     * Generate a summary for a news article using OpenRouter AI
     * Uses cache to avoid redundant API calls for the same article
     */
    suspend fun generateSummary(article: NewsArticle): NewsSummary? = withContext(Dispatchers.IO) {
        try {
            // Check cache first using article URL as stable identifier
            val cacheKey = article.url.hashCode().toString()
            cacheManager.getCachedSummary(cacheKey)?.let { cached ->
                Logger.d("AiSummaryService", "Returning cached summary for article: ${article.title}")
                return@withContext cached
            }
            
            val apiKey = prefs.getString("openrouter_api_key", "") ?: ""
            if (apiKey.isEmpty()) {
                Logger.w("AiSummaryService", "OpenRouter API key not configured")
                return@withContext null
            }
            
            Logger.d("AiSummaryService", "Generating new summary for article: ${article.title}")
            
            val prompt = buildSummaryPrompt(article)
            val request = ChatRequest(
                model = "google/gemini-flash-1.5", // Free tier model
                messages = listOf(
                    ChatMessage(role = "user", content = prompt)
                )
            )
            
            val response = openRouterApi.generateCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )
            
            if (response.isSuccessful && response.body() != null) {
                val summaryText = response.body()!!.choices.firstOrNull()?.message?.content ?: return@withContext null
                val summary = parseSummary(article.id, summaryText)
                
                // Cache the summary
                cacheManager.cacheSummary(cacheKey, summary)
                Logger.i("AiSummaryService", "Successfully generated and cached summary")
                
                summary
            } else {
                Logger.e("AiSummaryService", "API request failed: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Logger.e("AiSummaryService", "Exception generating summary", e)
            e.printStackTrace()
            null
        }
    }
    
    private fun buildSummaryPrompt(article: NewsArticle): String {
        return """
            Bitte fasse den folgenden Nachrichtenartikel auf Deutsch zusammen.
            
            Titel: ${article.title}
            Inhalt: ${article.content ?: article.description ?: "Keine Inhaltsbeschreibung verfügbar"}
            
            Erstelle:
            1. Eine prägnante Zusammenfassung (max. 3 Sätze)
            2. 3-5 wichtige Punkte als Aufzählungsliste
            
            Format:
            ZUSAMMENFASSUNG: [deine Zusammenfassung]
            PUNKTE:
            - [Punkt 1]
            - [Punkt 2]
            - [Punkt 3]
        """.trimIndent()
    }
    
    private fun parseSummary(articleId: String, summaryText: String): NewsSummary {
        val lines = summaryText.split("\n")
        var summary = ""
        val keyPoints = mutableListOf<String>()
        var inPoints = false
        
        for (line in lines) {
            when {
                line.startsWith("ZUSAMMENFASSUNG:") -> {
                    summary = line.substringAfter("ZUSAMMENFASSUNG:").trim()
                }
                line.startsWith("PUNKTE:") -> {
                    inPoints = true
                }
                inPoints && line.trim().startsWith("-") -> {
                    keyPoints.add(line.trim().removePrefix("-").trim())
                }
                summary.isEmpty() && line.isNotBlank() -> {
                    summary = line.trim()
                }
            }
        }
        
        return NewsSummary(
            articleId = articleId,
            summary = summary.ifEmpty { summaryText },
            keyPoints = keyPoints,
            generatedAt = System.currentTimeMillis()
        )
    }
}
