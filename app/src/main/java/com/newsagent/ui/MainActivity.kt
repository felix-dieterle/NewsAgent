package com.newsagent.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.newsagent.api.RssFeedParser
import com.newsagent.models.NewsArticle
import com.newsagent.services.*
import com.newsagent.utils.Logger
import kotlinx.coroutines.async
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
                // Show search status to user
                val statusMessage = newsRepository.getSearchStatusMessage()
                Toast.makeText(
                    this@MainActivity,
                    "Suche wird gestartet...\n$statusMessage",
                    Toast.LENGTH_LONG
                ).show()
                
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
                
                // Process articles with parallel execution
                processArticles(newArticles)
                
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
        menu?.add(0, 2, 0, "Kostenlose Suche")
        menu?.add(0, 4, 0, "RSS Nachrichten")
        
        // Add search view
        val searchItem = menu?.add(0, 3, 0, "Suchen")
        searchItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
        
        val searchView = SearchView(this)
        searchItem?.actionView = searchView
        searchView.queryHint = "Nachrichten suchen..."
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    performRssSearch(query)
                }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            2 -> {
                showFreeSearchDialog()
                true
            }
            4 -> {
                loadRssNews()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showFreeSearchDialog() {
        val searchInput = android.widget.EditText(this).apply {
            hint = "Suchbegriff eingeben..."
        }
        
        AlertDialog.Builder(this)
            .setTitle("Kostenlose Nachrichtensuche")
            .setMessage("Suche ohne API-Schlüssel (GNews)")
            .setView(searchInput)
            .setPositiveButton("Suchen") { _, _ ->
                val query = searchInput.text.toString()
                if (query.isNotBlank()) {
                    performFreeSearch(query)
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
    
    /**
     * Helper method to process articles with summaries and credibility checks
     * Uses parallel processing with concurrency limit for better performance
     */
    private suspend fun processArticles(articlesList: List<NewsArticle>) {
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        val enableSummary = prefs.getBoolean("enable_auto_summary", true)
        val enableCredibility = prefs.getBoolean("enable_credibility_check", true)
        
        // Limit concurrency to avoid overwhelming the device (max 5 concurrent)
        val chunkedArticles = articlesList.chunked(5)
        
        for (chunk in chunkedArticles) {
            chunk.map { article ->
                lifecycleScope.async {
                    if (enableSummary) {
                        article.summary = aiSummaryService.generateSummary(article)
                    }
                    
                    if (enableCredibility) {
                        article.credibilityScore = credibilityService.checkCredibility(article)
                    }
                }
            }.forEach { it.await() }
        }
    }
    
    private fun performFreeSearch(query: String) {
        lifecycleScope.launch {
            try {
                Logger.d("MainActivity", "Performing free search for: $query")
                
                val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
                val language = prefs.getString("language", "de") ?: "de"
                val country = prefs.getString("country", "de") ?: "de"
                val maxArticles = prefs.getInt("max_articles", 10)
                
                Toast.makeText(
                    this@MainActivity,
                    "Suche nach '$query'...\nQuelle: GNews | Land: $country | Sprache: $language | Max: $maxArticles",
                    Toast.LENGTH_LONG
                ).show()
                
                val newArticles = newsRepository.searchNewsFree(query)
                Logger.i("MainActivity", "Free search returned ${newArticles.size} articles")
                
                if (newArticles.isEmpty()) {
                    Logger.w("MainActivity", "No articles found for query: $query")
                    Toast.makeText(
                        this@MainActivity,
                        "Keine Artikel für '$query' gefunden.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                
                // Process articles
                processArticles(newArticles)
                
                articles.clear()
                articles.addAll(newArticles)
                adapter.notifyDataSetChanged()
                
                Toast.makeText(
                    this@MainActivity,
                    "${newArticles.size} Artikel gefunden",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error in free search", e)
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Fehler bei der Suche: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun loadFreeHeadlines() {
        lifecycleScope.launch {
            try {
                Logger.d("MainActivity", "Fetching free headlines...")
                Toast.makeText(
                    this@MainActivity,
                    "Lade kostenlose Schlagzeilen...",
                    Toast.LENGTH_SHORT
                ).show()
                
                val newArticles = newsRepository.fetchTopHeadlinesFree()
                Logger.i("MainActivity", "Free headlines returned ${newArticles.size} articles")
                
                if (newArticles.isEmpty()) {
                    Logger.w("MainActivity", "No free headlines available")
                    Toast.makeText(
                        this@MainActivity,
                        "Keine kostenlosen Schlagzeilen verfügbar.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                
                // Process articles
                processArticles(newArticles)
                
                articles.clear()
                articles.addAll(newArticles)
                adapter.notifyDataSetChanged()
                
                Toast.makeText(
                    this@MainActivity,
                    "${newArticles.size} kostenlose Artikel geladen",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error loading free headlines", e)
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Fehler beim Laden: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun loadRssNews() {
        lifecycleScope.launch {
            try {
                Logger.d("MainActivity", "Fetching RSS news...")
                
                val feedSources = newsRepository.getRssFeedSourceNames()
                val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
                val maxArticles = prefs.getInt("max_articles", 10)
                
                Toast.makeText(
                    this@MainActivity,
                    "Lade RSS-Nachrichten (100% kostenlos)...\nQuellen: $feedSources\nMax: $maxArticles",
                    Toast.LENGTH_LONG
                ).show()
                
                val newArticles = newsRepository.fetchRssNews()
                Logger.i("MainActivity", "RSS fetch returned ${newArticles.size} articles")
                
                if (newArticles.isEmpty()) {
                    Logger.w("MainActivity", "No RSS articles available")
                    Toast.makeText(
                        this@MainActivity,
                        "Keine RSS-Artikel verfügbar.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                
                // Process articles
                processArticles(newArticles)
                
                articles.clear()
                articles.addAll(newArticles)
                adapter.notifyDataSetChanged()
                
                Toast.makeText(
                    this@MainActivity,
                    "${newArticles.size} RSS-Artikel geladen",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error loading RSS news", e)
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Fehler beim Laden: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun performRssSearch(query: String) {
        lifecycleScope.launch {
            try {
                Logger.d("MainActivity", "Performing RSS search for: $query")
                
                val feedSources = newsRepository.getRssFeedSourceNames()
                val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
                val maxArticles = prefs.getInt("max_articles", 10)
                
                Toast.makeText(
                    this@MainActivity,
                    "Suche in RSS-Feeds nach '$query'...\nQuellen: $feedSources\nMax: $maxArticles",
                    Toast.LENGTH_LONG
                ).show()
                
                val newArticles = newsRepository.searchRssNews(query)
                Logger.i("MainActivity", "RSS search returned ${newArticles.size} articles")
                
                if (newArticles.isEmpty()) {
                    Logger.w("MainActivity", "No articles found for query: $query")
                    Toast.makeText(
                        this@MainActivity,
                        "Keine Artikel für '$query' in RSS-Feeds gefunden.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                
                // Process articles
                processArticles(newArticles)
                
                articles.clear()
                articles.addAll(newArticles)
                adapter.notifyDataSetChanged()
                
                Toast.makeText(
                    this@MainActivity,
                    "${newArticles.size} Artikel gefunden",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error in RSS search", e)
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Fehler bei der RSS-Suche: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
