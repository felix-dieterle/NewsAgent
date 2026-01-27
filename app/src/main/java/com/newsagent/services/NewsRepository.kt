package com.newsagent.services

import android.content.Context
import android.content.SharedPreferences
import com.newsagent.api.*
import com.newsagent.models.NewsArticle
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
            level = HttpLoggingInterceptor.Level.BODY
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
    
    /**
     * Fetch top headlines
     */
    suspend fun fetchTopHeadlines(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val apiKey = prefs.getString("news_api_key", "") ?: ""
            if (apiKey.isEmpty()) {
                return@withContext emptyList<NewsArticle>()
            }
            
            val country = prefs.getString("country", "de") ?: "de"
            val pageSize = prefs.getInt("max_articles", 10)
            
            val response = newsApi.getTopHeadlines(
                apiKey = apiKey,
                country = country,
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
}
