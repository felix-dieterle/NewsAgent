package com.newsagent.utils

import com.newsagent.models.NewsArticle

/**
 * Utility for deduplicating articles to avoid redundant processing
 * 
 * Deduplication reduces:
 * - Redundant AI summary generation (expensive)
 * - Redundant credibility checks
 * - UI clutter from duplicate articles
 */
object ArticleDeduplicator {
    
    /**
     * Deduplicate a list of articles based on URL
     * Articles with the same URL are considered duplicates
     * 
     * @param articles List of articles to deduplicate
     * @return List of unique articles (first occurrence kept)
     */
    fun deduplicateByUrl(articles: List<NewsArticle>): List<NewsArticle> {
        val seenUrls = mutableSetOf<String>()
        val uniqueArticles = mutableListOf<NewsArticle>()
        
        var duplicateCount = 0
        
        for (article in articles) {
            val normalizedUrl = normalizeUrl(article.url)
            
            if (!seenUrls.contains(normalizedUrl)) {
                seenUrls.add(normalizedUrl)
                uniqueArticles.add(article)
            } else {
                duplicateCount++
            }
        }
        
        if (duplicateCount > 0) {
            Logger.i("ArticleDeduplicator", "Removed $duplicateCount duplicate articles (${uniqueArticles.size} unique remaining)")
        }
        
        return uniqueArticles
    }
    
    /**
     * Deduplicate by title similarity (for articles from different sources)
     * This is more aggressive and may catch duplicates with different URLs
     * 
     * @param articles List of articles to deduplicate
     * @param similarityThreshold Similarity threshold (0.0 to 1.0, default 0.85)
     * @return List of unique articles
     */
    fun deduplicateByTitle(articles: List<NewsArticle>, similarityThreshold: Double = 0.85): List<NewsArticle> {
        val uniqueArticles = mutableListOf<NewsArticle>()
        
        var duplicateCount = 0
        
        for (article in articles) {
            var isDuplicate = false
            
            for (existing in uniqueArticles) {
                val similarity = calculateTitleSimilarity(article.title, existing.title)
                
                if (similarity >= similarityThreshold) {
                    isDuplicate = true
                    duplicateCount++
                    break
                }
            }
            
            if (!isDuplicate) {
                uniqueArticles.add(article)
            }
        }
        
        if (duplicateCount > 0) {
            Logger.i("ArticleDeduplicator", "Removed $duplicateCount similar articles by title (${uniqueArticles.size} unique remaining)")
        }
        
        return uniqueArticles
    }
    
    /**
     * Normalize URL for comparison (removes trailing slashes, query params, etc.)
     */
    private fun normalizeUrl(url: String): String {
        return url
            .lowercase()
            .replace(Regex("[?#].*"), "") // Remove query params and fragments
            .trimEnd('/')
    }
    
    /**
     * Calculate similarity between two titles using simple word overlap
     * Returns a value between 0.0 (completely different) and 1.0 (identical)
     */
    private fun calculateTitleSimilarity(title1: String, title2: String): Double {
        val words1 = title1.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        val words2 = title2.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0
        }
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        // Jaccard similarity
        return intersection.toDouble() / union.toDouble()
    }
    
    /**
     * Get statistics about deduplication
     */
    fun getDeduplicationStats(original: List<NewsArticle>, deduplicated: List<NewsArticle>): DeduplicationStats {
        val removed = original.size - deduplicated.size
        val percentage = if (original.isNotEmpty()) {
            (removed.toDouble() / original.size * 100).toInt()
        } else {
            0
        }
        
        return DeduplicationStats(
            originalCount = original.size,
            uniqueCount = deduplicated.size,
            duplicatesRemoved = removed,
            reductionPercentage = percentage
        )
    }
    
    data class DeduplicationStats(
        val originalCount: Int,
        val uniqueCount: Int,
        val duplicatesRemoved: Int,
        val reductionPercentage: Int
    )
}
