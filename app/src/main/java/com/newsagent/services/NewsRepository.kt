package com.newsagent.services

import android.content.Context
import android.content.SharedPreferences
import com.newsagent.api.*
import com.newsagent.cache.CacheManager
import com.newsagent.models.NewsArticle
import com.newsagent.models.NewsFilterPreferences
import com.newsagent.models.SortOrder
import com.newsagent.utils.ArticleFilterHelper
import com.newsagent.utils.Logger
import com.newsagent.utils.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Repository for fetching and managing news articles
 * Implements caching to reduce redundant API calls and improve performance
 */
class NewsRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("newsagent_prefs", Context.MODE_PRIVATE)
    private val cacheManager = CacheManager.getInstance()
    private val rateLimiter = RateLimiter.getInstance()
    
    // HTTP cache for network responses (10 MB)
    private val httpCache = Cache(
        directory = File(context.cacheDir, "http_cache"),
        maxSize = 10L * 1024L * 1024L // 10 MB
    )
    
    private val newsApi: NewsApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // Use BASIC level to avoid logging request/response bodies and sensitive headers
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .cache(httpCache)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl("https://newsapi.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApi::class.java)
    }
    
    private val freeNewsApi: FreeNewsApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .cache(httpCache)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl("https://gnews.io/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FreeNewsApi::class.java)
    }
    
    private val googleCustomSearchApi: GoogleCustomSearchApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .cache(httpCache)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleCustomSearchApi::class.java)
    }
    
    /**
     * Fetch top headlines with caching - routes to appropriate source based on settings
     */
    suspend fun fetchTopHeadlines(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val newsSource = prefs.getString("news_source", "newsapi") ?: "newsapi"
        val country = prefs.getString("country", "de") ?: "de"
        val language = prefs.getString("language", "de") ?: "de"
        val maxArticles = prefs.getInt("max_articles", 10)
        
        Logger.i("NewsRepository", "Fetching headlines from source: $newsSource (country=$country, language=$language, max=$maxArticles)")
        
        when (newsSource) {
            "gnews" -> fetchTopHeadlinesFree()
            "rss" -> fetchRssNews()
            else -> fetchTopHeadlinesFromNewsApi()
        }
    }
    
    /**
     * Get a human-readable description of the current search configuration
     */
    fun getSearchStatusMessage(): String {
        val newsSource = prefs.getString("news_source", "newsapi") ?: "newsapi"
        val country = prefs.getString("country", "de") ?: "de"
        val language = prefs.getString("language", "de") ?: "de"
        val maxArticles = prefs.getInt("max_articles", 10)
        
        val sourceName = when (newsSource) {
            "gnews" -> "GNews"
            "rss" -> "RSS Feeds (${getRssFeedSourceNames()})"
            else -> "NewsAPI"
        }
        
        return "Quelle: $sourceName | Land: $country | Sprache: $language | Max: $maxArticles"
    }
    
    /**
     * Get a comma-separated list of RSS feed source names
     */
    fun getRssFeedSourceNames(): String {
        return RssFeedParser.GERMAN_RSS_FEEDS.keys.joinToString(", ")
    }
    
    /**
     * Get status message for GNews search
     */
    fun getGNewsSearchStatusMessage(query: String): String {
        val language = prefs.getString("language", "de") ?: "de"
        val country = prefs.getString("country", "de") ?: "de"
        val maxArticles = prefs.getInt("max_articles", 10)
        return "Suche nach '$query'...\nQuelle: GNews | Land: $country | Sprache: $language | Max: $maxArticles"
    }
    
    /**
     * Get status message for RSS news loading
     */
    fun getRssLoadStatusMessage(): String {
        val feedSources = getRssFeedSourceNames()
        val maxArticles = prefs.getInt("max_articles", 10)
        return "Lade RSS-Nachrichten (100% kostenlos)...\nQuellen: $feedSources\nMax: $maxArticles"
    }
    
    /**
     * Get status message for RSS search
     */
    fun getRssSearchStatusMessage(query: String): String {
        val feedSources = getRssFeedSourceNames()
        val maxArticles = prefs.getInt("max_articles", 10)
        return "Suche in RSS-Feeds nach '$query'...\nQuellen: $feedSources\nMax: $maxArticles"
    }
    
    /**
     * Fetch top headlines from NewsAPI.org with caching
     */
    private suspend fun fetchTopHeadlinesFromNewsApi(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val apiKey = prefs.getString("news_api_key", "") ?: ""
            if (apiKey.isEmpty()) {
                Logger.w("NewsRepository", "⚠️ News API key not configured - returning 0 results")
                Logger.i("NewsRepository", "➡️ Bitte API-Schlüssel in den Einstellungen konfigurieren")
                return@withContext emptyList<NewsArticle>()
            }
            
            val country = prefs.getString("country", "de") ?: "de"
            val pageSize = prefs.getInt("max_articles", 10)
            
            // Check cache first
            val cacheKey = "headlines_${country}_${pageSize}"
            cacheManager.getCachedArticles(cacheKey)?.let { cached ->
                Logger.d("NewsRepository", "Returning ${cached.size} cached headlines")
                return@withContext cached
            }
            
            // Check rate limit
            if (!rateLimiter.allowRequest("news_api")) {
                val remaining = rateLimiter.getRemainingRequests("news_api")
                Logger.w("NewsRepository", "⚠️ Rate limit reached. Remaining requests: $remaining - returning 0 results")
                Logger.i("NewsRepository", "➡️ Warten Sie bis das Rate Limit zurückgesetzt wird oder verwenden Sie RSS Feeds")
                return@withContext emptyList<NewsArticle>()
            }
            
            Logger.i("NewsRepository", "Querying NewsAPI: country=$country, pageSize=$pageSize")
            
            val response = newsApi.getTopHeadlines(
                apiKey = apiKey,
                country = country,
                pageSize = pageSize
            )
            
            if (response.isSuccessful && response.body() != null) {
                val articles = response.body()!!.articles.map { convertToNewsArticle(it) }
                Logger.i("NewsRepository", "✅ Successfully fetched ${articles.size} headlines")
                
                // Cache the results
                cacheManager.cacheArticles(cacheKey, articles)
                
                articles
            } else {
                Logger.e("NewsRepository", "❌ API request failed: ${response.code()} - ${response.message()}")
                Logger.i("NewsRepository", "➡️ Überprüfen Sie Ihren API-Schlüssel und Rate Limits")
                emptyList()
            }
        } catch (e: Exception) {
            Logger.e("NewsRepository", "❌ Exception fetching headlines", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Search for news articles with caching and rate limiting
     */
    suspend fun searchNews(query: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val apiKey = prefs.getString("news_api_key", "") ?: ""
            if (apiKey.isEmpty()) {
                return@withContext emptyList<NewsArticle>()
            }
            
            val language = prefs.getString("language", "de") ?: "de"
            val pageSize = prefs.getInt("max_articles", 10)
            
            Logger.i("NewsRepository", "Querying NewsAPI search: query='$query', language=$language, pageSize=$pageSize")
            
            // Check cache first
            val cacheKey = "search_${query}_${language}_${pageSize}"
            cacheManager.getCachedArticles(cacheKey)?.let { cached ->
                Logger.d("NewsRepository", "Returning ${cached.size} cached search results")
                return@withContext cached
            }
            
            // Check rate limit
            if (!rateLimiter.allowRequest("news_api")) {
                val remaining = rateLimiter.getRemainingRequests("news_api")
                Logger.w("NewsRepository", "Rate limit reached. Remaining requests: $remaining")
                return@withContext emptyList<NewsArticle>()
            }
            
            val response = newsApi.searchNews(
                apiKey = apiKey,
                query = query,
                language = language,
                pageSize = pageSize
            )
            
            if (response.isSuccessful && response.body() != null) {
                val articles = response.body()!!.articles.map { convertToNewsArticle(it) }
                
                // Cache the results
                cacheManager.cacheArticles(cacheKey, articles)
                
                articles
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun convertToNewsArticle(apiArticle: NewsApiArticle): NewsArticle {
        return NewsArticle(
            id = UUID.randomUUID().toString(),
            title = apiArticle.title,
            description = apiArticle.description,
            content = apiArticle.content,
            url = apiArticle.url,
            source = apiArticle.source.name,
            publishedAt = apiArticle.publishedAt,
            imageUrl = apiArticle.urlToImage,
            author = apiArticle.author
        )
    }
    
    /**
     * Search for news using GNews API (free tier with simple registration)
     * Requires gnews_api_token in preferences
     */
    suspend fun searchNewsFree(query: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val apiToken = prefs.getString("gnews_api_token", "") ?: ""
            if (apiToken.isEmpty()) {
                Logger.w("NewsRepository", "GNews API token not configured")
                return@withContext emptyList<NewsArticle>()
            }
            
            val language = prefs.getString("language", "de") ?: "de"
            val country = prefs.getString("country", "de") ?: "de"
            val pageSize = prefs.getInt("max_articles", 10)
            
            // Check cache first
            val cacheKey = "gnews_search_${query}_${language}_${country}_${pageSize}"
            cacheManager.getCachedArticles(cacheKey)?.let { cached ->
                Logger.d("NewsRepository", "Returning ${cached.size} cached GNews search results")
                return@withContext cached
            }
            
            // Check rate limit
            if (!rateLimiter.allowRequest("gnews_api")) {
                val remaining = rateLimiter.getRemainingRequests("gnews_api")
                Logger.w("NewsRepository", "GNews rate limit reached. Remaining requests: $remaining")
                return@withContext emptyList<NewsArticle>()
            }
            
            Logger.i("NewsRepository", "Querying GNews search: query='$query', language=$language, country=$country, maxResults=$pageSize")
            
            val response = freeNewsApi.searchGNews(
                query = query,
                apiToken = apiToken,
                language = language,
                maxResults = pageSize,
                country = country
            )
            
            if (response.isSuccessful && response.body() != null) {
                val articles = response.body()!!.articles.map { convertFromGNewsArticle(it) }
                Logger.i("NewsRepository", "Successfully fetched ${articles.size} articles (free search)")
                
                // Cache the results
                cacheManager.cacheArticles(cacheKey, articles)
                
                articles
            } else {
                Logger.e("NewsRepository", "Free API request failed: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Logger.e("NewsRepository", "Exception in free search", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Fetch top headlines using GNews API (free tier with simple registration)
     * Requires gnews_api_token in preferences
     */
    suspend fun fetchTopHeadlinesFree(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val apiToken = prefs.getString("gnews_api_token", "") ?: ""
            if (apiToken.isEmpty()) {
                Logger.w("NewsRepository", "⚠️ GNews API token not configured - returning 0 results")
                Logger.i("NewsRepository", "➡️ Bitte GNews API-Token in den Einstellungen konfigurieren")
                return@withContext emptyList<NewsArticle>()
            }
            
            val language = prefs.getString("language", "de") ?: "de"
            val country = prefs.getString("country", "de") ?: "de"
            val pageSize = prefs.getInt("max_articles", 10)
            
            // Check cache first
            val cacheKey = "gnews_headlines_${language}_${country}_${pageSize}"
            cacheManager.getCachedArticles(cacheKey)?.let { cached ->
                Logger.d("NewsRepository", "Returning ${cached.size} cached GNews headlines")
                return@withContext cached
            }
            
            // Check rate limit
            if (!rateLimiter.allowRequest("gnews_api")) {
                val remaining = rateLimiter.getRemainingRequests("gnews_api")
                Logger.w("NewsRepository", "⚠️ GNews rate limit reached. Remaining requests: $remaining - returning 0 results")
                Logger.i("NewsRepository", "➡️ Warten Sie bis das Rate Limit zurückgesetzt wird oder verwenden Sie RSS Feeds")
                return@withContext emptyList<NewsArticle>()
            }
            
            Logger.i("NewsRepository", "Querying GNews headlines: language=$language, country=$country, maxResults=$pageSize")
            
            val response = freeNewsApi.getGNewsHeadlines(
                apiToken = apiToken,
                language = language,
                maxResults = pageSize,
                country = country
            )
            
            if (response.isSuccessful && response.body() != null) {
                val articles = response.body()!!.articles.map { convertFromGNewsArticle(it) }
                Logger.i("NewsRepository", "✅ Successfully fetched ${articles.size} free headlines")
                
                // Cache the results
                cacheManager.cacheArticles(cacheKey, articles)
                
                articles
            } else {
                Logger.e("NewsRepository", "❌ Free API request failed: ${response.code()} - ${response.message()}")
                Logger.i("NewsRepository", "➡️ Überprüfen Sie Ihren GNews API-Token und Rate Limits")
                emptyList()
            }
        } catch (e: Exception) {
            Logger.e("NewsRepository", "❌ Exception fetching free headlines", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun convertFromGNewsArticle(article: GNewsArticle): NewsArticle {
        return NewsArticle(
            id = UUID.randomUUID().toString(),
            title = article.title,
            description = article.description,
            content = article.content,
            url = article.url,
            source = article.source.name,
            publishedAt = article.publishedAt,
            imageUrl = article.image,
            author = null
        )
    }
    
    /**
     * Fetch news from public RSS feeds (completely free, no API key needed)
     * Uses public German news RSS feeds like Tagesschau, Heise, etc.
     */
    suspend fun fetchRssNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val feedSources = getRssFeedSourceNames()
            Logger.i("NewsRepository", "📰 Querying RSS feeds (100% kostenlos): $feedSources")
            
            val parser = RssFeedParser()
            val allArticles = mutableListOf<NewsArticle>()
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            
            var successCount = 0
            var failCount = 0
            val failureDetails = mutableListOf<String>()
            
            // Fetch from multiple RSS feeds
            for ((sourceName, feedUrl) in RssFeedParser.GERMAN_RSS_FEEDS) {
                try {
                    Logger.d("NewsRepository", "Fetching from RSS source: $sourceName ($feedUrl)")
                    val request = okhttp3.Request.Builder()
                        .url(feedUrl)
                        .addHeader("User-Agent", "NewsAgent/1.0 (Android News Aggregator)")
                        .build()
                    
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val xmlContent = response.body?.string()
                        if (xmlContent.isNullOrEmpty()) {
                            failCount++
                            val msg = "$sourceName: Empty response body"
                            failureDetails.add(msg)
                            Logger.w("NewsRepository", "⚠️ $msg")
                            continue
                        }
                        
                        Logger.d("NewsRepository", "$sourceName: Received ${xmlContent.length} bytes, parsing...")
                        val rssArticles = parser.parseRssContent(xmlContent, sourceName)
                        
                        if (rssArticles.isEmpty()) {
                            failCount++
                            val msg = "$sourceName: No articles parsed from ${xmlContent.length} bytes of XML"
                            failureDetails.add(msg)
                            Logger.w("NewsRepository", "⚠️ $msg")
                            Logger.d("NewsRepository", "XML preview: ${xmlContent.take(500)}")
                        } else {
                            val newsArticles = rssArticles.map { convertFromRssArticle(it) }
                            allArticles.addAll(newsArticles)
                            successCount++
                            Logger.i("NewsRepository", "✅ $sourceName: ${newsArticles.size} articles fetched successfully")
                        }
                    } else {
                        failCount++
                        val msg = "$sourceName: HTTP ${response.code} - ${response.message}"
                        failureDetails.add(msg)
                        Logger.w("NewsRepository", "⚠️ $msg")
                    }
                } catch (e: Exception) {
                    failCount++
                    val msg = "$sourceName: ${e.javaClass.simpleName} - ${e.message}"
                    failureDetails.add(msg)
                    Logger.e("NewsRepository", "❌ $msg", e)
                }
            }
            
            Logger.i("NewsRepository", "RSS fetch complete: $successCount succeeded, $failCount failed (Total articles: ${allArticles.size})")
            
            if (allArticles.isEmpty()) {
                Logger.w("NewsRepository", "⚠️ No RSS articles fetched - alle ${RssFeedParser.GERMAN_RSS_FEEDS.size} Quellen haben fehlgeschlagen")
                Logger.w("NewsRepository", "Fehlerdetails:")
                failureDetails.forEach { detail ->
                    Logger.w("NewsRepository", "  - $detail")
                }
                Logger.i("NewsRepository", "➡️ Überprüfen Sie Ihre Internetverbindung und Firewall-Einstellungen")
            }
            
            val maxArticles = prefs.getInt("max_articles", 10)
            val limitedArticles = allArticles.take(maxArticles)
            Logger.i("NewsRepository", "Returning ${limitedArticles.size} RSS articles (limited from ${allArticles.size} total)")
            limitedArticles
        } catch (e: Exception) {
            Logger.e("NewsRepository", "❌ Exception fetching RSS news", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Search RSS news by keyword
     */
    suspend fun searchRssNews(query: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val feedSources = getRssFeedSourceNames()
            val maxArticles = prefs.getInt("max_articles", 10)
            
            // Check cache first - RSS search results are cached for faster retrieval
            val cacheKey = "rss_search_${query}_${maxArticles}"
            cacheManager.getCachedArticles(cacheKey)?.let { cached ->
                Logger.d("NewsRepository", "Returning ${cached.size} cached RSS search results for '$query'")
                return@withContext cached
            }
            
            Logger.i("NewsRepository", "Searching RSS feeds for query='$query' in sources: $feedSources")
            
            val allArticles = fetchRssNews()
            
            // Simple keyword search in title and description
            val filtered = allArticles.filter { article ->
                article.title.contains(query, ignoreCase = true) ||
                (article.description?.contains(query, ignoreCase = true) == true)
            }
            
            Logger.i("NewsRepository", "RSS search complete: Found ${filtered.size} articles matching '$query' (out of ${allArticles.size} total)")
            
            // Cache the search results
            cacheManager.cacheArticles(cacheKey, filtered)
            
            filtered
        } catch (e: Exception) {
            Logger.e("NewsRepository", "Exception searching RSS news", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun convertFromRssArticle(article: RssFeedParser.RssArticle): NewsArticle {
        return NewsArticle(
            id = UUID.randomUUID().toString(),
            title = article.title,
            description = article.description,
            content = null,
            url = article.link,
            source = article.source,
            publishedAt = article.pubDate ?: "",
            imageUrl = null,
            author = null
        )
    }
    
    /**
     * Search for news using Google Custom Search API
     * Requires:
     * - google_api_key in SharedPreferences
     * - google_search_engine_id in SharedPreferences
     * 
     * Free tier: 100 queries per day
     */
    suspend fun searchGoogleCustomSearch(query: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val apiKey = prefs.getString("google_api_key", "") ?: ""
            val searchEngineId = prefs.getString("google_search_engine_id", "") ?: ""
            
            if (apiKey.isEmpty() || searchEngineId.isEmpty()) {
                Logger.w("NewsRepository", "Google Custom Search API key or Search Engine ID not configured")
                return@withContext emptyList<NewsArticle>()
            }
            
            val maxArticles = prefs.getInt("max_articles", 10)
            
            // Check cache first
            val cacheKey = "google_search_${query}_${maxArticles}"
            cacheManager.getCachedArticles(cacheKey)?.let { cached ->
                Logger.d("NewsRepository", "Returning ${cached.size} cached Google Custom Search results")
                return@withContext cached
            }
            
            // Check rate limit
            if (!rateLimiter.allowRequest("google_custom_search")) {
                val remaining = rateLimiter.getRemainingRequests("google_custom_search")
                Logger.w("NewsRepository", "Google Custom Search rate limit reached. Remaining requests: $remaining")
                return@withContext emptyList<NewsArticle>()
            }
            
            Logger.i("NewsRepository", "Querying Google Custom Search: query='$query', maxResults=$maxArticles")
            
            val response = googleCustomSearchApi.search(
                query = query,
                apiKey = apiKey,
                cx = searchEngineId,
                num = maxArticles
            )
            
            if (response.isSuccessful && response.body() != null) {
                val items = response.body()!!.items ?: emptyList()
                val articles = items.map { convertFromGoogleSearchItem(it) }
                Logger.i("NewsRepository", "Successfully fetched ${articles.size} articles from Google Custom Search")
                
                // Cache the results
                cacheManager.cacheArticles(cacheKey, articles)
                
                articles
            } else {
                Logger.e("NewsRepository", "Google Custom Search request failed: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Logger.e("NewsRepository", "Exception in Google Custom Search", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun convertFromGoogleSearchItem(item: GoogleSearchItem): NewsArticle {
        // Extract image URL from pagemap if available
        val imageUrl = item.pagemap?.cseImage?.firstOrNull()?.src
        
        // Try to extract published date from metatags
        val publishedAt = item.pagemap?.metatags?.firstOrNull()?.get("article:published_time")
            ?: item.pagemap?.metatags?.firstOrNull()?.get("datePublished")
            ?: ""
        
        // Extract source from metatags or URL
        val source = item.pagemap?.metatags?.firstOrNull()?.get("og:site_name")
            ?: item.link.substringAfter("://").substringBefore("/")
        
        return NewsArticle(
            id = UUID.randomUUID().toString(),
            title = item.title,
            description = item.snippet,
            content = null,
            url = item.link,
            source = source,
            publishedAt = publishedAt,
            imageUrl = imageUrl,
            author = null
        )
    }
    
    /**
     * Get filter preferences from SharedPreferences
     */
    fun getFilterPreferences(): NewsFilterPreferences {
        val categoriesString = prefs.getString("selected_categories", "") ?: ""
        val keywordsString = prefs.getString("filter_keywords", "") ?: ""
        val showOnlyUnread = prefs.getBoolean("show_only_unread", false)
        val sortOrderString = prefs.getString("sort_order", "RECENT") ?: "RECENT"
        
        val categories = if (categoriesString.isNotEmpty()) {
            categoriesString.split(",").map { it.trim() }.toSet()
        } else {
            emptySet()
        }
        
        val keywords = if (keywordsString.isNotEmpty()) {
            keywordsString.split(",").map { it.trim() }.toSet()
        } else {
            emptySet()
        }
        
        val sortOrder = try {
            SortOrder.valueOf(sortOrderString)
        } catch (e: Exception) {
            SortOrder.RECENT
        }
        
        return NewsFilterPreferences(
            selectedCategories = categories,
            keywords = keywords,
            showOnlyUnread = showOnlyUnread,
            sortBy = sortOrder
        )
    }
    
    /**
     * Save filter preferences to SharedPreferences
     */
    fun saveFilterPreferences(preferences: NewsFilterPreferences) {
        prefs.edit().apply {
            putString("selected_categories", preferences.selectedCategories.joinToString(","))
            putString("filter_keywords", preferences.keywords.joinToString(","))
            putBoolean("show_only_unread", preferences.showOnlyUnread)
            putString("sort_order", preferences.sortBy.name)
            apply()
        }
    }
    
    /**
     * Fetch top headlines with filtering and sorting applied
     */
    suspend fun fetchTopHeadlinesFiltered(): List<NewsArticle> = withContext(Dispatchers.IO) {
        // Fetch articles from the selected source
        val articles = fetchTopHeadlines()
        
        // Enrich articles with categories and tags if not already set
        val enrichedArticles = articles.map { article ->
            if (article.category == null) {
                article.category = ArticleFilterHelper.inferCategory(article)
            }
            if (article.tags.isEmpty()) {
                article.copy(tags = ArticleFilterHelper.extractTags(article))
            } else {
                article
            }
        }
        
        // Get filter preferences
        val preferences = getFilterPreferences()
        
        // Apply filters
        val filteredArticles = ArticleFilterHelper.filterArticles(enrichedArticles, preferences)
        
        // Apply sorting
        val sortedArticles = ArticleFilterHelper.sortArticles(filteredArticles, preferences.sortBy)
        
        Logger.i("NewsRepository", "Filtered ${articles.size} articles to ${sortedArticles.size} based on preferences")
        
        sortedArticles
    }
    
    /**
     * Get available categories for filtering
     */
    fun getAvailableCategories(): List<String> {
        return listOf(
            "Allgemein",
            "Technologie",
            "Politik",
            "Wirtschaft",
            "Sport",
            "Wissenschaft",
            "Kultur",
            "Umwelt"
        )
    }
}
