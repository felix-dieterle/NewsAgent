package com.newsagent.services

import android.content.Context
import android.content.SharedPreferences
import com.newsagent.api.*
import com.newsagent.models.CredibilityScore
import com.newsagent.models.NewsArticle
import com.newsagent.models.NewsSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

/**
 * Service for generating AI summaries using OpenRouter
 */
class AiSummaryService(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("newsagent_prefs", Context.MODE_PRIVATE)
    
    private val openRouterApi: OpenRouterApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
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
     */
    suspend fun generateSummary(article: NewsArticle): NewsSummary? = withContext(Dispatchers.IO) {
        try {
            val apiKey = prefs.getString("openrouter_api_key", "") ?: ""
            if (apiKey.isEmpty()) {
                return@withContext null
            }
            
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
                parseSummary(article.id, summaryText)
            } else {
                null
            }
        } catch (e: Exception) {
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
