package com.newsagent.services

import android.content.Context
import android.content.SharedPreferences
import com.newsagent.api.*
import com.newsagent.models.NewsArticle
import com.newsagent.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

/**
 * Repository for fetching and managing news articles
 */
class NewsRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("newsagent_prefs", Context.MODE_PRIVATE)
    
    private val newsApi: NewsApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // Use BASIC level to avoid logging request/response bodies and sensitive headers
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
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
            .addInterceptor(logging)
            .build()
        
        Retrofit.Builder()
            .baseUrl("https://gnews.io/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FreeNewsApi::class.java)
    }
    
    /**
     * Fetch top headlines
     */
    suspend fun fetchTopHeadlines(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val apiKey = prefs.getString("news_api_key", "") ?: ""
            if (apiKey.isEmpty()) {
                Logger.w("NewsRepository", "News API key not configured")
                return@withContext emptyList<NewsArticle>()
            }
            
            val country = prefs.getString("country", "de") ?: "de"
            val pageSize = prefs.getInt("max_articles", 10)
            
            Logger.d("NewsRepository", "Fetching top headlines for country=$country, pageSize=$pageSize")
            
            val response = newsApi.getTopHeadlines(
                apiKey = apiKey,
                country = country,
                pageSize = pageSize
            )
            
            if (response.isSuccessful && response.body() != null) {
                val articles = response.body()!!.articles.map { convertToNewsArticle(it) }
                Logger.i("NewsRepository", "Successfully fetched ${articles.size} headlines")
                articles
            } else {
                Logger.e("NewsRepository", "API request failed: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Logger.e("NewsRepository", "Exception fetching headlines", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Search for news articles
     */
    suspend fun searchNews(query: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val apiKey = prefs.getString("news_api_key", "") ?: ""
            if (apiKey.isEmpty()) {
                return@withContext emptyList<NewsArticle>()
            }
            
            val language = prefs.getString("language", "de") ?: "de"
            val pageSize = prefs.getInt("max_articles", 10)
            
            val response = newsApi.searchNews(
                apiKey = apiKey,
                query = query,
                language = language,
                pageSize = pageSize
            )
            
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.articles.map { convertToNewsArticle(it) }
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
            
            Logger.d("NewsRepository", "Searching free news for query='$query', language=$language")
            
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
                Logger.w("NewsRepository", "GNews API token not configured")
                return@withContext emptyList<NewsArticle>()
            }
            
            val language = prefs.getString("language", "de") ?: "de"
            val country = prefs.getString("country", "de") ?: "de"
            val pageSize = prefs.getInt("max_articles", 10)
            
            Logger.d("NewsRepository", "Fetching free top headlines for country=$country, language=$language")
            
            val response = freeNewsApi.getGNewsHeadlines(
                apiToken = apiToken,
                language = language,
                maxResults = pageSize,
                country = country
            )
            
            if (response.isSuccessful && response.body() != null) {
                val articles = response.body()!!.articles.map { convertFromGNewsArticle(it) }
                Logger.i("NewsRepository", "Successfully fetched ${articles.size} free headlines")
                articles
            } else {
                Logger.e("NewsRepository", "Free API request failed: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Logger.e("NewsRepository", "Exception fetching free headlines", e)
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
            Logger.d("NewsRepository", "Fetching RSS news from public feeds")
            val parser = RssFeedParser()
            val allArticles = mutableListOf<NewsArticle>()
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            var successCount = 0
            var failCount = 0
            
            // Fetch from multiple RSS feeds
            for ((sourceName, feedUrl) in RssFeedParser.GERMAN_RSS_FEEDS) {
                try {
                    val request = okhttp3.Request.Builder()
                        .url(feedUrl)
                        .build()
                    
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val xmlContent = response.body?.string() ?: continue
                        val rssArticles = parser.parseRssContent(xmlContent, sourceName)
                        val newsArticles = rssArticles.map { convertFromRssArticle(it) }
                        allArticles.addAll(newsArticles)
                        successCount++
                        Logger.d("NewsRepository", "Fetched ${newsArticles.size} articles from $sourceName")
                    } else {
                        failCount++
                        Logger.w("NewsRepository", "Failed to fetch from $sourceName: ${response.code}")
                    }
                } catch (e: Exception) {
                    failCount++
                    Logger.e("NewsRepository", "Exception fetching from $sourceName", e)
                }
            }
            
            Logger.i("NewsRepository", "RSS fetch complete: $successCount succeeded, $failCount failed")
            
            val maxArticles = prefs.getInt("max_articles", 10)
            val limitedArticles = allArticles.take(maxArticles)
            Logger.i("NewsRepository", "Successfully fetched ${limitedArticles.size} RSS articles")
            limitedArticles
        } catch (e: Exception) {
            Logger.e("NewsRepository", "Exception fetching RSS news", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Search RSS news by keyword
     */
    suspend fun searchRssNews(query: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            Logger.d("NewsRepository", "Searching RSS news for: $query")
            val allArticles = fetchRssNews()
            
            // Simple keyword search in title and description
            val filtered = allArticles.filter { article ->
                article.title.contains(query, ignoreCase = true) ||
                (article.description?.contains(query, ignoreCase = true) == true)
            }
            
            Logger.i("NewsRepository", "Found ${filtered.size} articles matching '$query'")
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
}
