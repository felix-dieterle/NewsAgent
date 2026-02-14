package com.newsagent.utils

import com.newsagent.models.NewsArticle
import com.newsagent.models.NewsFilterPreferences
import com.newsagent.models.SortOrder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for filtering and sorting news articles based on user preferences
 */
object ArticleFilterHelper {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    /**
     * Apply filters to a list of articles based on user preferences
     */
    fun filterArticles(
        articles: List<NewsArticle>,
        preferences: NewsFilterPreferences
    ): List<NewsArticle> {
        var filtered = articles
        
        // Filter by categories if any are selected
        if (preferences.selectedCategories.isNotEmpty()) {
            filtered = filtered.filter { article ->
                article.category in preferences.selectedCategories
            }
        }
        
        // Filter by keywords - check title, description, and tags
        if (preferences.keywords.isNotEmpty()) {
            filtered = filtered.filter { article ->
                matchesKeywords(article, preferences.keywords)
            }
        }
        
        // Filter by read status if requested
        if (preferences.showOnlyUnread) {
            filtered = filtered.filter { !it.isRead }
        }
        
        return filtered
    }
    
    /**
     * Check if article matches any of the keywords
     */
    private fun matchesKeywords(article: NewsArticle, keywords: Set<String>): Boolean {
        val searchableText = buildString {
            append(article.title.lowercase())
            article.description?.let { append(" ").append(it.lowercase()) }
            article.tags.forEach { append(" ").append(it.lowercase()) }
        }
        
        return keywords.any { keyword ->
            searchableText.contains(keyword.lowercase())
        }
    }
    
    /**
     * Sort articles based on the specified order
     */
    fun sortArticles(
        articles: List<NewsArticle>,
        sortOrder: SortOrder
    ): List<NewsArticle> {
        return when (sortOrder) {
            SortOrder.RECENT -> sortByRecent(articles)
            SortOrder.CREDIBILITY -> sortByCredibility(articles)
            SortOrder.RELEVANCE -> articles // Relevance is handled by filtering
        }
    }
    
    /**
     * Sort by published date (newest first)
     */
    private fun sortByRecent(articles: List<NewsArticle>): List<NewsArticle> {
        return articles.sortedByDescending { article ->
            try {
                dateFormat.parse(article.publishedAt)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }
    
    /**
     * Sort by credibility score (highest first), then by recency
     */
    private fun sortByCredibility(articles: List<NewsArticle>): List<NewsArticle> {
        return articles.sortedWith(
            compareByDescending<NewsArticle> { it.credibilityScore?.score ?: 0.0 }
                .thenByDescending { article ->
                    try {
                        dateFormat.parse(article.publishedAt)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
        )
    }
    
    /**
     * Infer category from article content
     * This is a simple heuristic-based categorization
     */
    fun inferCategory(article: NewsArticle): String {
        val text = buildString {
            append(article.title.lowercase())
            article.description?.let { append(" ").append(it.lowercase()) }
        }
        
        return when {
            containsAny(text, listOf("technologie", "tech", "digital", "ki", "ai", "computer", "software", "internet")) -> "Technologie"
            containsAny(text, listOf("politik", "regierung", "bundestag", "politiker", "wahl", "gesetz")) -> "Politik"
            containsAny(text, listOf("wirtschaft", "börse", "aktie", "unternehmen", "markt", "inflation", "euro")) -> "Wirtschaft"
            containsAny(text, listOf("sport", "fußball", "bundesliga", "olympia", "weltmeister")) -> "Sport"
            containsAny(text, listOf("wissenschaft", "forschung", "studie", "universum", "gesundheit", "medizin")) -> "Wissenschaft"
            containsAny(text, listOf("kultur", "kunst", "musik", "film", "theater", "buch")) -> "Kultur"
            containsAny(text, listOf("umwelt", "klima", "energie", "natur", "öko")) -> "Umwelt"
            else -> "Allgemein"
        }
    }
    
    /**
     * Check if text contains any of the keywords
     */
    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }
    
    /**
     * Extract tags from article content
     */
    fun extractTags(article: NewsArticle): List<String> {
        val tags = mutableSetOf<String>()
        
        val text = buildString {
            append(article.title.lowercase())
            article.description?.let { append(" ").append(it.lowercase()) }
        }
        
        // Add important keywords as tags
        val importantKeywords = listOf(
            "ki", "ai", "technologie", "politik", "wirtschaft", "sport",
            "wissenschaft", "kultur", "umwelt", "klima", "gesundheit",
            "digital", "europa", "deutschland", "wahl", "bundesliga"
        )
        
        importantKeywords.forEach { keyword ->
            if (text.contains(keyword)) {
                tags.add(keyword)
            }
        }
        
        return tags.take(5).toList() // Limit to 5 tags
    }
}
