package com.newsagent.services

import android.content.Context
import android.content.SharedPreferences
import com.newsagent.api.*
import com.newsagent.cache.CacheManager
import com.newsagent.models.CredibilityScore
import com.newsagent.models.NewsArticle
import com.newsagent.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Service for checking news credibility
 * Implements caching to avoid redundant checks
 */
class CredibilityCheckService(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("newsagent_prefs", Context.MODE_PRIVATE)
    private val cacheManager = CacheManager.getInstance()
    
    private val credibilityApi: CredibilityApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        // This is a placeholder URL - replace with actual credibility checking API
        val baseUrl = prefs.getString("credibility_api_url", "https://api.credibilitycheck.example/") ?: "https://api.credibilitycheck.example/"
        
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CredibilityApi::class.java)
    }
    
    /**
     * Check credibility of a news article
     * If API is not available, performs basic heuristic checks
     * Uses cache to avoid redundant checks
     */
    suspend fun checkCredibility(article: NewsArticle): CredibilityScore = withContext(Dispatchers.IO) {
        try {
            // Check cache first using article URL as stable identifier
            val cacheKey = article.url.hashCode().toString()
            cacheManager.getCachedCredibility(cacheKey)?.let { cached ->
                Logger.d("CredibilityCheckService", "Returning cached credibility for: ${article.title}")
                return@withContext cached
            }
            
            Logger.d("CredibilityCheckService", "Checking credibility for: ${article.title}")
            
            val apiUrl = prefs.getString("credibility_api_url", "")
            
            val score = if (apiUrl.isNullOrEmpty()) {
                // Fallback to basic heuristic checks
                performBasicCredibilityCheck(article)
            } else {
                try {
                    val request = CredibilityRequest(
                        title = article.title,
                        content = article.content ?: article.description ?: "",
                        source = article.source,
                        url = article.url
                    )
                    
                    val response = credibilityApi.checkCredibility(request)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        CredibilityScore(
                            articleId = article.id,
                            score = body.score,
                            factors = body.factors,
                            verified = body.verified,
                            concerns = body.concerns,
                            checkedAt = System.currentTimeMillis()
                        )
                    } else {
                        performBasicCredibilityCheck(article)
                    }
                } catch (e: Exception) {
                    Logger.w("CredibilityCheckService", "API check failed, using heuristic", e)
                    performBasicCredibilityCheck(article)
                }
            }
            
            // Cache the result
            cacheManager.cacheCredibility(cacheKey, score)
            Logger.i("CredibilityCheckService", "Credibility checked and cached")
            
            score
        } catch (e: Exception) {
            Logger.e("CredibilityCheckService", "Exception checking credibility", e)
            e.printStackTrace()
            performBasicCredibilityCheck(article)
        }
    }
    
    /**
     * Perform basic credibility checks based on heuristics
     */
    private fun performBasicCredibilityCheck(article: NewsArticle): CredibilityScore {
        val factors = mutableMapOf<String, Float>()
        val concerns = mutableListOf<String>()
        
        // Check if source is known and reputable
        val sourceScore = checkSourceReputation(article.source)
        factors["source_reputation"] = sourceScore
        
        if (sourceScore < 0.5f) {
            concerns.add("Quelle möglicherweise nicht vertrauenswürdig")
        }
        
        // Check for sensational language
        val sensationalismScore = checkSensationalism(article.title)
        factors["sensationalism"] = 1.0f - sensationalismScore
        
        if (sensationalismScore > 0.7f) {
            concerns.add("Reißerische Sprache erkannt")
        }
        
        // Check if author is present
        val hasAuthor = !article.author.isNullOrEmpty()
        factors["has_author"] = if (hasAuthor) 1.0f else 0.5f
        
        if (!hasAuthor) {
            concerns.add("Kein Autor angegeben")
        }
        
        // Calculate overall score
        val overallScore = factors.values.average().toFloat()
        
        return CredibilityScore(
            articleId = article.id,
            score = overallScore,
            factors = factors,
            verified = overallScore >= 0.7f,
            concerns = concerns,
            checkedAt = System.currentTimeMillis()
        )
    }
    
    private fun checkSourceReputation(source: String): Float {
        // Known reputable German news sources
        val reputableSources = listOf(
            "tagesschau", "zdf", "ard", "spiegel", "zeit", "faz", "süddeutsche",
            "handelsblatt", "tagesspiegel", "welt", "focus", "stern", "dpa"
        )
        
        val sourceLower = source.lowercase()
        return if (reputableSources.any { sourceLower.contains(it) }) {
            0.9f
        } else {
            0.5f // Neutral score for unknown sources
        }
    }
    
    private fun checkSensationalism(title: String): Float {
        val sensationalWords = listOf(
            "schock", "unglaublich", "skandal", "sensation", "katastrophe",
            "drastisch", "horror", "wahnsinn", "unfassbar", "krass"
        )
        
        val titleLower = title.lowercase()
        val count = sensationalWords.count { titleLower.contains(it) }
        
        return when {
            count >= 3 -> 1.0f
            count == 2 -> 0.8f
            count == 1 -> 0.5f
            else -> 0.0f
        }
    }
}
