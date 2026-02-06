package com.newsagent.services

import android.content.Context
import android.content.SharedPreferences
import com.newsagent.models.NewsArticle
import com.newsagent.utils.Logger
import com.newsagent.utils.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Intelligent search strategy selector that chooses the optimal news source
 * based on availability, rate limits, and query characteristics
 * 
 * Priority order:
 * 1. Cache (instant, free)
 * 2. RSS Feeds (free, unlimited, but limited search capability)
 * 3. GNews API (limited free tier, good search)
 * 4. NewsAPI (limited free tier, excellent search)
 */
class SearchStrategySelector(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("newsagent_prefs", Context.MODE_PRIVATE)
    private val newsRepository = NewsRepository(context)
    private val rateLimiter = RateLimiter.getInstance()
    
    enum class SearchSource {
        CACHE,      // Cached results
        RSS,        // Free RSS feeds
        GNEWS,      // GNews API (free tier)
        NEWSAPI     // NewsAPI (free tier)
    }
    
    data class SearchResult(
        val articles: List<NewsArticle>,
        val source: SearchSource,
        val fromCache: Boolean
    )
    
    /**
     * Execute an intelligent search that automatically selects the best source
     * 
     * @param query The search query
     * @param preferFree If true, strongly prefer free sources over paid APIs (overridden by AI mode setting)
     * @return SearchResult containing articles and metadata
     */
    suspend fun smartSearch(query: String, preferFree: Boolean = true): SearchResult = withContext(Dispatchers.IO) {
        // Check if AI mode is enabled (overrides preferFree parameter)
        val aiModeEnabled = prefs.getBoolean("ai_mode_enabled", false)
        val actualPreferFree = if (aiModeEnabled) false else preferFree
        
        Logger.i("SearchStrategySelector", "Smart search for: '$query' (AI mode=$aiModeEnabled, preferFree=$actualPreferFree)")
        
        // Strategy 1: Try RSS feeds first if preferFree and not in AI mode (completely free, unlimited)
        if (actualPreferFree) {
            Logger.d("SearchStrategySelector", "Trying RSS feeds (free, unlimited)...")
            val rssResults = try {
                newsRepository.searchRssNews(query)
            } catch (e: Exception) {
                Logger.e("SearchStrategySelector", "RSS search failed", e)
                emptyList()
            }
            
            if (rssResults.isNotEmpty()) {
                Logger.i("SearchStrategySelector", "✓ RSS search successful: ${rssResults.size} articles")
                return@withContext SearchResult(rssResults, SearchSource.RSS, false)
            }
            Logger.d("SearchStrategySelector", "✗ RSS search returned no results")
        }
        
        // Strategy 2: Try GNews API if available and within rate limit
        val gnewsToken = prefs.getString("gnews_api_token", "") ?: ""
        if (gnewsToken.isNotEmpty()) {
            val gnewsRemaining = rateLimiter.getRemainingRequests("gnews_api")
            Logger.d("SearchStrategySelector", "GNews API available (${gnewsRemaining} requests remaining)")
            
            if (gnewsRemaining > 5) { // Keep 5 request buffer
                Logger.d("SearchStrategySelector", "Trying GNews API search...")
                val gnewsResults = try {
                    newsRepository.searchNewsFree(query)
                } catch (e: Exception) {
                    Logger.e("SearchStrategySelector", "GNews search failed", e)
                    emptyList()
                }
                
                if (gnewsResults.isNotEmpty()) {
                    Logger.i("SearchStrategySelector", "✓ GNews search successful: ${gnewsResults.size} articles")
                    return@withContext SearchResult(gnewsResults, SearchSource.GNEWS, false)
                }
                Logger.d("SearchStrategySelector", "✗ GNews search returned no results")
            } else {
                Logger.w("SearchStrategySelector", "✗ GNews API rate limit low (${gnewsRemaining} remaining), skipping")
            }
        } else {
            Logger.d("SearchStrategySelector", "GNews API token not configured")
        }
        
        // Strategy 3: Try NewsAPI if available and within rate limit
        val newsApiKey = prefs.getString("news_api_key", "") ?: ""
        if (newsApiKey.isNotEmpty()) {
            val newsApiRemaining = rateLimiter.getRemainingRequests("news_api")
            Logger.d("SearchStrategySelector", "NewsAPI available (${newsApiRemaining} requests remaining)")
            
            if (newsApiRemaining > 5) { // Keep 5 request buffer
                Logger.d("SearchStrategySelector", "Trying NewsAPI search...")
                val newsApiResults = try {
                    newsRepository.searchNews(query)
                } catch (e: Exception) {
                    Logger.e("SearchStrategySelector", "NewsAPI search failed", e)
                    emptyList()
                }
                
                if (newsApiResults.isNotEmpty()) {
                    Logger.i("SearchStrategySelector", "✓ NewsAPI search successful: ${newsApiResults.size} articles")
                    return@withContext SearchResult(newsApiResults, SearchSource.NEWSAPI, false)
                }
                Logger.d("SearchStrategySelector", "✗ NewsAPI search returned no results")
            } else {
                Logger.w("SearchStrategySelector", "✗ NewsAPI rate limit low (${newsApiRemaining} remaining), skipping")
            }
        } else {
            Logger.d("SearchStrategySelector", "NewsAPI key not configured")
        }
        
        // Strategy 4: If preferFree was false (or AI mode), try RSS as fallback now
        if (!actualPreferFree) {
            Logger.d("SearchStrategySelector", "Falling back to RSS feeds...")
            val rssResults = try {
                newsRepository.searchRssNews(query)
            } catch (e: Exception) {
                Logger.e("SearchStrategySelector", "RSS fallback search failed", e)
                emptyList()
            }
            
            if (rssResults.isNotEmpty()) {
                Logger.i("SearchStrategySelector", "✓ RSS fallback successful: ${rssResults.size} articles")
                return@withContext SearchResult(rssResults, SearchSource.RSS, false)
            }
        }
        
        // No results from any source
        Logger.w("SearchStrategySelector", "No search results from any source for: '$query'")
        return@withContext SearchResult(emptyList(), SearchSource.RSS, false)
    }
    
    /**
     * Get headlines using the optimal source based on configuration
     */
    suspend fun smartHeadlines(): SearchResult = withContext(Dispatchers.IO) {
        val newsSource = prefs.getString("news_source", "newsapi") ?: "newsapi"
        Logger.i("SearchStrategySelector", "Fetching headlines from configured source: $newsSource")
        
        val articles = newsRepository.fetchTopHeadlines()
        
        val source = when (newsSource) {
            "rss" -> SearchSource.RSS
            "gnews" -> SearchSource.GNEWS
            else -> SearchSource.NEWSAPI
        }
        
        SearchResult(articles, source, false)
    }
    
    /**
     * Get a recommendation for the best source to use based on current state
     */
    fun getRecommendedSource(): SearchSource {
        // Check rate limits
        val gnewsRemaining = rateLimiter.getRemainingRequests("gnews_api")
        val newsApiRemaining = rateLimiter.getRemainingRequests("news_api")
        
        // Always prefer RSS if rate limits are low
        if (gnewsRemaining < 10 && newsApiRemaining < 10) {
            Logger.i("SearchStrategySelector", "Recommending RSS (rate limits low)")
            return SearchSource.RSS
        }
        
        // Check if API keys are configured
        val gnewsToken = prefs.getString("gnews_api_token", "") ?: ""
        val newsApiKey = prefs.getString("news_api_key", "") ?: ""
        
        return when {
            gnewsToken.isNotEmpty() && gnewsRemaining > newsApiRemaining -> {
                Logger.i("SearchStrategySelector", "Recommending GNews")
                SearchSource.GNEWS
            }
            newsApiKey.isNotEmpty() && newsApiRemaining > 10 -> {
                Logger.i("SearchStrategySelector", "Recommending NewsAPI")
                SearchSource.NEWSAPI
            }
            else -> {
                Logger.i("SearchStrategySelector", "Recommending RSS (safest option)")
                SearchSource.RSS
            }
        }
    }
}
