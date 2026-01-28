package com.newsagent.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.newsagent.models.NewsArticle
import com.newsagent.services.*
import com.newsagent.utils.Logger
import kotlinx.coroutines.launch

/**
 * Main activity displaying the list of news articles
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NewsAdapter
    private lateinit var fab: FloatingActionButton
    
    private lateinit var newsRepository: NewsRepository
    private lateinit var aiSummaryService: AiSummaryService
    private lateinit var credibilityService: CredibilityCheckService
    
    private val articles = mutableListOf<NewsArticle>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Logger.i("MainActivity", "onCreate started")
        
        try {
            // Initialize services
            Logger.d("MainActivity", "Initializing services...")
            newsRepository = NewsRepository(this)
            aiSummaryService = AiSummaryService(this)
            credibilityService = CredibilityCheckService(this)
            Logger.d("MainActivity", "Services initialized successfully")
            
            // Setup UI
            Logger.d("MainActivity", "Setting up UI...")
            setupRecyclerView()
            setupFab()
            Logger.d("MainActivity", "UI setup completed")
            
            // Load news
            Logger.i("MainActivity", "Loading initial news...")
            loadNews()
            
            // Schedule periodic updates
            Logger.d("MainActivity", "Scheduling periodic updates...")
            scheduleNewsUpdates()
            
            Logger.i("MainActivity", "onCreate completed successfully")
        } catch (e: Exception) {
            Logger.e("MainActivity", "Fatal error in onCreate", e)
            // Show error to user instead of crashing
            Toast.makeText(
                this,
                "Fehler beim Starten der App. Bitte überprüfen Sie die Logs in den Einstellungen.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun setupRecyclerView() {
        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            id = android.R.id.list
        }
        
        adapter = NewsAdapter(articles) { article ->
            openNewsDetail(article)
        }
        recyclerView.adapter = adapter
        
        setContentView(recyclerView)
    }
    
    private fun setupFab() {
        fab = FloatingActionButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_rotate)
            setOnClickListener {
                loadNews()
            }
        }
    }
    
    private fun loadNews() {
        lifecycleScope.launch {
            try {
                Logger.d("MainActivity", "Fetching headlines...")
                val newArticles = newsRepository.fetchTopHeadlines()
                Logger.i("MainActivity", "Fetched ${newArticles.size} articles")
                
                if (newArticles.isEmpty()) {
                    Logger.w("MainActivity", "No articles fetched - possibly missing API key")
                    Toast.makeText(
                        this@MainActivity,
                        "Keine Nachrichten gefunden. Bitte API-Schlüssel in Einstellungen konfigurieren.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                
                // Process articles
                for (article in newArticles) {
                    val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
                    
                    if (prefs.getBoolean("enable_auto_summary", true)) {
                        article.summary = aiSummaryService.generateSummary(article)
                    }
                    
                    if (prefs.getBoolean("enable_credibility_check", true)) {
                        article.credibilityScore = credibilityService.checkCredibility(article)
                    }
                }
                
                articles.clear()
                articles.addAll(newArticles)
                adapter.notifyDataSetChanged()
                
                Toast.makeText(
                    this@MainActivity,
                    "${newArticles.size} Artikel geladen",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error loading news", e)
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Fehler beim Laden der Nachrichten: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun openNewsDetail(article: NewsArticle) {
        val intent = Intent(this, NewsDetailActivity::class.java).apply {
            putExtra("article_id", article.id)
            putExtra("article_title", article.title)
            putExtra("article_content", article.content ?: article.description)
            putExtra("article_url", article.url)
            putExtra("article_source", article.source)
            putExtra("article_author", article.author)
            putExtra("article_image", article.imageUrl)
            article.summary?.let {
                putExtra("summary_text", it.summary)
                putExtra("summary_points", it.keyPoints.toTypedArray())
            }
            article.credibilityScore?.let {
                putExtra("credibility_score", it.score)
                putExtra("credibility_verified", it.verified)
                putExtra("credibility_concerns", it.concerns.toTypedArray())
            }
        }
        startActivity(intent)
    }
    
    private fun scheduleNewsUpdates() {
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        val intervalMinutes = prefs.getInt("update_interval_minutes", 60).toLong()
        NewsUpdateWorker.schedule(this, intervalMinutes)
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "Einstellungen")
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
