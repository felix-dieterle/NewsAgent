package com.newsagent.cache

import com.newsagent.models.NewsArticle
import com.newsagent.models.NewsSummary
import com.newsagent.models.CredibilityScore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Simple in-memory cache manager for NewsAgent
 * Reduces redundant API calls and improves performance
 */
class CacheManager private constructor() {
    
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val ttl: Long
    ) {
        fun isValid(): Boolean = System.currentTimeMillis() - timestamp < ttl
    }
    
    private val articlesCache = ConcurrentHashMap<String, CacheEntry<List<NewsArticle>>>()
    private val summaryCache = ConcurrentHashMap<String, CacheEntry<NewsSummary>>()
    private val credibilityCache = ConcurrentHashMap<String, CacheEntry<CredibilityScore>>()
    
    companion object {
        @Volatile
        private var instance: CacheManager? = null
        
        fun getInstance(): CacheManager {
            return instance ?: synchronized(this) {
                instance ?: CacheManager().also { instance = it }
            }
        }
        
        // Cache TTL configurations (in milliseconds)
        private val ARTICLES_TTL = TimeUnit.MINUTES.toMillis(15) // 15 minutes
        private val SUMMARY_TTL = TimeUnit.HOURS.toMillis(24) // 24 hours
        private val CREDIBILITY_TTL = TimeUnit.HOURS.toMillis(24) // 24 hours
    }
    
    /**
     * Cache articles with a cache key
     */
    fun cacheArticles(key: String, articles: List<NewsArticle>, ttl: Long = ARTICLES_TTL) {
        articlesCache[key] = CacheEntry(articles, System.currentTimeMillis(), ttl)
    }
    
    /**
     * Get cached articles if still valid
     */
    fun getCachedArticles(key: String): List<NewsArticle>? {
        val entry = articlesCache[key] ?: return null
        return if (entry.isValid()) entry.data else {
            articlesCache.remove(key)
            null
        }
    }
    
    /**
     * Cache summary for an article
     */
    fun cacheSummary(articleId: String, summary: NewsSummary, ttl: Long = SUMMARY_TTL) {
        summaryCache[articleId] = CacheEntry(summary, System.currentTimeMillis(), ttl)
    }
    
    /**
     * Get cached summary if still valid
     */
    fun getCachedSummary(articleId: String): NewsSummary? {
        val entry = summaryCache[articleId] ?: return null
        return if (entry.isValid()) entry.data else {
            summaryCache.remove(articleId)
            null
        }
    }
    
    /**
     * Cache credibility score for an article
     */
    fun cacheCredibility(articleId: String, score: CredibilityScore, ttl: Long = CREDIBILITY_TTL) {
        credibilityCache[articleId] = CacheEntry(score, System.currentTimeMillis(), ttl)
    }
    
    /**
     * Get cached credibility score if still valid
     */
    fun getCachedCredibility(articleId: String): CredibilityScore? {
        val entry = credibilityCache[articleId] ?: return null
        return if (entry.isValid()) entry.data else {
            credibilityCache.remove(articleId)
            null
        }
    }
    
    /**
     * Clear all caches
     */
    fun clearAll() {
        articlesCache.clear()
        summaryCache.clear()
        credibilityCache.clear()
    }
    
    /**
     * Clear expired entries from all caches
     */
    fun clearExpired() {
        articlesCache.entries.removeIf { !it.value.isValid() }
        summaryCache.entries.removeIf { !it.value.isValid() }
        credibilityCache.entries.removeIf { !it.value.isValid() }
    }
    
    /**
     * Get cache statistics for monitoring
     */
    fun getStats(): CacheStats {
        return CacheStats(
            articlesCount = articlesCache.size,
            summaryCount = summaryCache.size,
            credibilityCount = credibilityCache.size
        )
    }
    
    data class CacheStats(
        val articlesCount: Int,
        val summaryCount: Int,
        val credibilityCount: Int
    )
}
