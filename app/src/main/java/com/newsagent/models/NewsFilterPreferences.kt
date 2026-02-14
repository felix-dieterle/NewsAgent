package com.newsagent.models

/**
 * Preferences for filtering news articles
 */
data class NewsFilterPreferences(
    val selectedCategories: Set<String> = emptySet(),
    val keywords: Set<String> = emptySet(),
    val showOnlyUnread: Boolean = false,
    val sortBy: SortOrder = SortOrder.RECENT
)

enum class SortOrder {
    RECENT,           // Sort by published date (newest first)
    CREDIBILITY,      // Sort by credibility score (highest first)
    RELEVANCE         // Sort by relevance to keywords/categories
}
