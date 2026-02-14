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
import com.newsagent.models.NewsArticle
import com.newsagent.services.*
import com.newsagent.utils.ArticleDeduplicator
import com.newsagent.utils.Logger
import com.newsagent.utils.RateLimiter
import com.newsagent.utils.SearchThrottler
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
    private lateinit var searchStrategySelector: SearchStrategySelector
    private lateinit var updateService: UpdateService
    
    private var updateDownloadReceiver: android.content.BroadcastReceiver? = null
    private var updateProgressDialog: AlertDialog? = null
    
    private val searchThrottler = SearchThrottler.getInstance()
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
            searchStrategySelector = SearchStrategySelector(this)
            updateService = UpdateService(this)
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
            
            // Check for app updates
            Logger.d("MainActivity", "Checking for app updates...")
            checkForAppUpdate()
            
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
                
                Logger.d("MainActivity", "Fetching filtered headlines...")
                val newArticles = newsRepository.fetchTopHeadlinesFiltered()
                Logger.i("MainActivity", "Fetched ${newArticles.size} articles after filtering")
                
                if (newArticles.isEmpty()) {
                    Logger.w("MainActivity", "No articles fetched - possibly missing API key or filters too restrictive")
                    Toast.makeText(
                        this@MainActivity,
                        "Keine Nachrichten gefunden. Bitte API-Schlüssel in Einstellungen konfigurieren oder Filter anpassen.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                
                // Process articles with parallel execution and deduplication
                val processedArticles = processArticles(newArticles)
                
                articles.clear()
                articles.addAll(processedArticles)
                adapter.notifyDataSetChanged()
                
                Toast.makeText(
                    this@MainActivity,
                    "${processedArticles.size} Artikel geladen",
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
        // Mark article as read
        article.isRead = true
        adapter.notifyDataSetChanged()
        
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
        menu?.add(0, 6, 0, "ℹ️ Info & Hilfe")
        
        // Add rate limit indicators menu item
        val rateLimitItem = menu?.add(0, 5, 0, "API Limits")
        rateLimitItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        rateLimitItem?.actionView = createRateLimitIndicatorsView()
        
        // Add search view
        val searchItem = menu?.add(0, 3, 0, "Suchen")
        searchItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
        
        val searchView = SearchView(this)
        searchItem?.actionView = searchView
        searchView.queryHint = "Nachrichten suchen..."
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    // Use smart search instead of direct RSS search
                    performSmartSearch(query)
                }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                // Throttled search as user types
                if (!newText.isNullOrBlank() && newText.length >= SearchThrottler.MIN_QUERY_LENGTH) {
                    lifecycleScope.launch {
                        searchThrottler.executeSearch(
                            searchId = "main_search",
                            query = newText,
                            searchAction = { query ->
                                performSmartSearch(query)
                            }
                        )
                    }
                }
                return true
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
            5 -> {
                showRateLimitDetails()
                true
            }
            6 -> {
                showInfoDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun createRateLimitIndicatorsView(): android.view.View {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(8, 0, 8, 0)
        }
        
        val rateLimiter = RateLimiter.getInstance()
        
        // NewsAPI indicator
        val newsApiStats = rateLimiter.getStats("news_api")
        val newsApiIndicator = RateLimitIndicatorView(this)
        newsApiStats?.let {
            val usagePercent = it.currentRequests.toFloat() / it.maxRequests.toFloat()
            newsApiIndicator.setUsagePercent(usagePercent)
        }
        container.addView(newsApiIndicator)
        
        // Spacing
        container.addView(android.view.View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(8, 1)
        })
        
        // GNews indicator
        val gnewsStats = rateLimiter.getStats("gnews_api")
        val gnewsIndicator = RateLimitIndicatorView(this)
        gnewsStats?.let {
            val usagePercent = it.currentRequests.toFloat() / it.maxRequests.toFloat()
            gnewsIndicator.setUsagePercent(usagePercent)
        }
        container.addView(gnewsIndicator)
        
        // Spacing
        container.addView(android.view.View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(8, 1)
        })
        
        // OpenRouter (AI) indicator
        val openRouterStats = rateLimiter.getStats("openrouter_api")
        val openRouterIndicator = RateLimitIndicatorView(this)
        openRouterStats?.let {
            val usagePercent = it.currentRequests.toFloat() / it.maxRequests.toFloat()
            openRouterIndicator.setUsagePercent(usagePercent)
        }
        container.addView(openRouterIndicator)
        
        container.setOnClickListener {
            showRateLimitDetails()
        }
        
        return container
    }
    
    private fun showRateLimitDetails() {
        val rateLimiter = RateLimiter.getInstance()
        
        val newsApiStats = rateLimiter.getStats("news_api")
        val gnewsStats = rateLimiter.getStats("gnews_api")
        val openRouterStats = rateLimiter.getStats("openrouter_api")
        
        val message = buildString {
            appendLine("API Rate Limits:")
            appendLine()
            
            newsApiStats?.let {
                val percent = (it.currentRequests.toFloat() / it.maxRequests * 100).toInt()
                appendLine("📰 NewsAPI: ${it.remainingRequests}/${it.maxRequests} remaining ($percent% used)")
                if (it.timeUntilReset > 0) {
                    appendLine("   Reset in: ${formatTimeRemaining(it.timeUntilReset)}")
                }
                appendLine()
            }
            
            gnewsStats?.let {
                val percent = (it.currentRequests.toFloat() / it.maxRequests * 100).toInt()
                appendLine("📰 GNews: ${it.remainingRequests}/${it.maxRequests} remaining ($percent% used)")
                if (it.timeUntilReset > 0) {
                    appendLine("   Reset in: ${formatTimeRemaining(it.timeUntilReset)}")
                }
                appendLine()
            }
            
            openRouterStats?.let {
                val percent = (it.currentRequests.toFloat() / it.maxRequests * 100).toInt()
                appendLine("🤖 AI (OpenRouter): ${it.remainingRequests}/${it.maxRequests} remaining ($percent% used)")
                if (it.timeUntilReset > 0) {
                    appendLine("   Reset in: ${formatTimeRemaining(it.timeUntilReset)}")
                }
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("API Rate Limits")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun formatTimeRemaining(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
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
     * Show information dialog explaining how the app works
     */
    private fun showInfoDialog() {
        val scrollView = android.widget.ScrollView(this)
        val textView = android.widget.TextView(this).apply {
            setPadding(24, 16, 24, 16)
            textSize = 14f
            text = buildString {
                appendLine("📰 NewsAgent - Intelligente Nachrichten-App")
                appendLine()
                appendLine("═══════════════════════════════")
                appendLine()
                appendLine("🔍 FUNKTIONEN:")
                appendLine()
                appendLine("• Nachrichten aus verschiedenen Quellen")
                appendLine("  - NewsAPI.org (100 Anfragen/Tag)")
                appendLine("  - GNews.io (100 Anfragen/Tag)")
                appendLine("  - RSS Feeds (unbegrenzt & kostenlos)")
                appendLine()
                appendLine("• KI-Zusammenfassungen")
                appendLine("  Automatische Zusammenfassung mit Kernpunkten")
                appendLine()
                appendLine("• Glaubwürdigkeitsprüfung")
                appendLine("  Bewertung der Quellen-Zuverlässigkeit")
                appendLine()
                appendLine("• Intelligente Suche")
                appendLine("  Optimierte Suchstrategie basierend auf API-Limits")
                appendLine()
                appendLine("═══════════════════════════════")
                appendLine()
                appendLine("⚙️ BEDIENUNG:")
                appendLine()
                appendLine("1. Einstellungen konfigurieren")
                appendLine("   • API-Schlüssel eingeben (optional)")
                appendLine("   • Nachrichtenquelle wählen")
                appendLine("   • Update-Intervall festlegen")
                appendLine()
                appendLine("2. Nachrichten laden")
                appendLine("   • Automatisch beim Start")
                appendLine("   • Manuell über Aktualisieren-Button")
                appendLine("   • Per Suche nach Themen")
                appendLine()
                appendLine("3. Artikel lesen")
                appendLine("   • Tippen für Details")
                appendLine("   • Zusammenfassung anzeigen")
                appendLine("   • Glaubwürdigkeit prüfen")
                appendLine()
                appendLine("═══════════════════════════════")
                appendLine()
                appendLine("💡 MODI:")
                appendLine()
                appendLine("• Standard-Modus")
                appendLine("  Bevorzugt kostenlose RSS-Feeds")
                appendLine("  Ideal für täglichen Gebrauch")
                appendLine()
                appendLine("• KI-Modus")
                appendLine("  Nutzt kostenpflichtige APIs")
                appendLine("  Bessere Qualität & mehr Features")
                appendLine()
                appendLine("═══════════════════════════════")
                appendLine()
                appendLine("🔄 AUTOMATISCHE UPDATES:")
                appendLine()
                appendLine("Die App prüft beim Start auf neue Versionen")
                appendLine("von GitHub. Sie können Updates in den")
                appendLine("Einstellungen aktivieren/deaktivieren.")
                appendLine()
                appendLine("═══════════════════════════════")
                appendLine()
                appendLine("📊 API RATE LIMITS:")
                appendLine()
                appendLine("Beachten Sie die farbigen Indikatoren:")
                appendLine("🟢 Grün: < 70% genutzt")
                appendLine("🟡 Gelb: 70-99% genutzt")
                appendLine("🔴 Rot: Limit erreicht")
                appendLine("⚪ Grau: Kein API-Key konfiguriert")
                appendLine()
                appendLine("═══════════════════════════════")
                appendLine()
                appendLine("ℹ️ WEITERE HILFE:")
                appendLine()
                appendLine("Bei Problemen:")
                appendLine("• Logs in Einstellungen überprüfen")
                appendLine("• GitHub Issues melden")
                appendLine("• API-Keys validieren")
            }
        }
        scrollView.addView(textView)
        
        AlertDialog.Builder(this)
            .setTitle("ℹ️ Info & Hilfe")
            .setView(scrollView)
            .setPositiveButton("Verstanden", null)
            .setNeutralButton("Einstellungen öffnen") { _, _ ->
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .show()
    }
    
    /**
     * Helper method to process articles with summaries and credibility checks
     * Uses parallel processing with concurrency limit for better performance
     * Respects AI mode settings for optimal resource usage
     */
    private suspend fun processArticles(articlesList: List<NewsArticle>): List<NewsArticle> {
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        val enableSummary = prefs.getBoolean("enable_auto_summary", true)
        val enableCredibility = prefs.getBoolean("enable_credibility_check", true)
        val aiModeEnabled = prefs.getBoolean("ai_mode_enabled", false)
        
        // Deduplicate articles to avoid redundant processing
        val deduplicated = ArticleDeduplicator.deduplicateByUrl(articlesList)
        val stats = ArticleDeduplicator.getDeduplicationStats(articlesList, deduplicated)
        if (stats.duplicatesRemoved > 0) {
            Logger.i("MainActivity", "Deduplication: Removed ${stats.duplicatesRemoved} duplicates (${stats.reductionPercentage}% reduction)")
        }
        
        // Adjust concurrency based on AI mode
        // AI mode: more aggressive processing (up to 5 concurrent)
        // Non-AI mode: conservative processing (up to 3 concurrent)
        val concurrencyLimit = if (aiModeEnabled) 5 else 3
        val chunkedArticles = deduplicated.chunked(concurrencyLimit)
        
        Logger.i("MainActivity", "Processing ${deduplicated.size} articles (AI mode=$aiModeEnabled, concurrency=$concurrencyLimit)")
        
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
        
        return deduplicated
    }
    
    private fun performFreeSearch(query: String) {
        lifecycleScope.launch {
            try {
                Logger.d("MainActivity", "Performing free search for: $query")
                
                Toast.makeText(
                    this@MainActivity,
                    newsRepository.getGNewsSearchStatusMessage(query),
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
                
                // Process articles with deduplication
                val processedArticles = processArticles(newArticles)
                
                articles.clear()
                articles.addAll(processedArticles)
                adapter.notifyDataSetChanged()
                
                Toast.makeText(
                    this@MainActivity,
                    "${processedArticles.size} Artikel gefunden",
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
                
                // Process articles with deduplication
                val processedArticles = processArticles(newArticles)
                
                articles.clear()
                articles.addAll(processedArticles)
                adapter.notifyDataSetChanged()
                
                Toast.makeText(
                    this@MainActivity,
                    "${processedArticles.size} kostenlose Artikel geladen",
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
                
                Toast.makeText(
                    this@MainActivity,
                    newsRepository.getRssLoadStatusMessage(),
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
                
                // Process articles with deduplication
                val processedArticles = processArticles(newArticles)
                
                articles.clear()
                articles.addAll(processedArticles)
                adapter.notifyDataSetChanged()
                
                Toast.makeText(
                    this@MainActivity,
                    "${processedArticles.size} RSS-Artikel geladen",
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
                
                Toast.makeText(
                    this@MainActivity,
                    newsRepository.getRssSearchStatusMessage(query),
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
                
                // Process articles with deduplication
                val processedArticles = processArticles(newArticles)
                
                articles.clear()
                articles.addAll(processedArticles)
                adapter.notifyDataSetChanged()
                
                Toast.makeText(
                    this@MainActivity,
                    "${processedArticles.size} Artikel gefunden",
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
    
    /**
     * Perform intelligent search using SearchStrategySelector
     * Automatically chooses the best source based on availability and rate limits
     */
    private fun performSmartSearch(query: String) {
        lifecycleScope.launch {
            try {
                Logger.d("MainActivity", "Performing smart search for: $query")
                
                Toast.makeText(
                    this@MainActivity,
                    "Intelligente Suche nach '$query'...",
                    Toast.LENGTH_SHORT
                ).show()
                
                val result = searchStrategySelector.smartSearch(query)
                Logger.i("MainActivity", "Smart search returned ${result.articles.size} articles from ${result.source}")
                
                if (result.articles.isEmpty()) {
                    Logger.w("MainActivity", "No articles found for query: $query")
                    Toast.makeText(
                        this@MainActivity,
                        "Keine Artikel für '$query' gefunden.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                
                // Process articles with deduplication
                val processedArticles = processArticles(result.articles)
                
                articles.clear()
                articles.addAll(processedArticles)
                adapter.notifyDataSetChanged()
                
                // Show source in toast
                val sourceName = when (result.source) {
                    SearchStrategySelector.SearchSource.RSS -> "RSS"
                    SearchStrategySelector.SearchSource.GNEWS -> "GNews"
                    SearchStrategySelector.SearchSource.NEWSAPI -> "NewsAPI"
                    SearchStrategySelector.SearchSource.GOOGLE_CUSTOM -> "Google Search"
                    SearchStrategySelector.SearchSource.CACHE -> "Cache"
                }
                
                Toast.makeText(
                    this@MainActivity,
                    "${processedArticles.size} Artikel gefunden (Quelle: $sourceName)",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error in smart search", e)
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Fehler bei der Suche: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * Check for app updates from GitHub releases
     * Shows a dialog if an update is available
     */
    private fun checkForAppUpdate() {
        // Check if auto-update is enabled
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        val autoUpdateEnabled = prefs.getBoolean("auto_update_enabled", true)
        
        if (!autoUpdateEnabled) {
            Logger.d("MainActivity", "Auto-update is disabled in settings")
            return
        }
        
        // Check if we should perform an update check based on interval
        if (!updateService.shouldCheckForUpdate()) {
            val timeUntilNextCheck = updateService.getTimeUntilNextCheck()
            val minutesRemaining = (timeUntilNextCheck / 60000).toInt()
            Logger.d("MainActivity", "Skipping update check - too soon since last check ($minutesRemaining minutes remaining)")
            Toast.makeText(
                this@MainActivity,
                "Update-Prüfung übersprungen. Nächste Prüfung in $minutesRemaining Minuten.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // Show user that update check is starting
                Toast.makeText(
                    this@MainActivity,
                    "Suche nach App-Updates...",
                    Toast.LENGTH_SHORT
                ).show()
                
                val updateInfo = updateService.checkForUpdate()
                updateService.updateLastCheckTime()
                
                if (updateInfo == null) {
                    Logger.d("MainActivity", "Could not check for updates")
                    Toast.makeText(
                        this@MainActivity,
                        "Update-Prüfung fehlgeschlagen",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                
                if (updateInfo.isUpdateAvailable && updateInfo.downloadUrl != null) {
                    Logger.i("MainActivity", "Update available: ${updateInfo.latestVersion}")
                    showUpdateDialog(updateInfo)
                } else {
                    Logger.d("MainActivity", "No update available. Current: ${updateInfo.currentVersion} (${updateInfo.currentVersionCode})")
                    Toast.makeText(
                        this@MainActivity,
                        "App ist auf dem neuesten Stand\nVersion: ${updateInfo.currentVersion} (Build ${updateInfo.currentVersionCode})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error checking for updates", e)
                Toast.makeText(
                    this@MainActivity,
                    "Fehler bei der Update-Prüfung. Bitte später erneut versuchen.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * Show update dialog to the user
     */
    private fun showUpdateDialog(updateInfo: UpdateService.UpdateInfo) {
        val message = buildString {
            appendLine("Eine neue Version ist verfügbar!")
            appendLine()
            appendLine("Aktuelle Version: ${updateInfo.currentVersion} (Build ${updateInfo.currentVersionCode})")
            appendLine("Neue Version: ${updateInfo.latestVersion} (Build ${updateInfo.latestVersionCode})")
            appendLine()
            if (updateInfo.releaseNotes.isNotBlank()) {
                appendLine("Was ist neu:")
                appendLine(updateInfo.releaseNotes.take(200))
                if (updateInfo.releaseNotes.length > 200) {
                    appendLine("...")
                }
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Update verfügbar")
            .setMessage(message)
            .setPositiveButton("Aktualisieren") { _, _ ->
                updateInfo.downloadUrl?.let { url ->
                    downloadUpdate(url)
                }
            }
            .setNegativeButton("Später") { _, _ ->
                Logger.d("MainActivity", "User postponed update")
            }
            .setNeutralButton("Diese Version überspringen") { _, _ ->
                updateService.skipVersion(updateInfo.latestVersion)
                Logger.d("MainActivity", "User skipped version ${updateInfo.latestVersion}")
            }
            .setCancelable(true)
            .show()
    }
    
    /**
     * Download the update APK
     */
    private fun downloadUpdate(downloadUrl: String) {
        try {
            Logger.i("MainActivity", "Starting update download")
            
            // Show progress dialog
            val progressView = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(50, 40, 50, 40)
                
                // Progress bar
                addView(android.widget.ProgressBar(this@MainActivity).apply {
                    isIndeterminate = true
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                })
                
                // Status text
                addView(android.widget.TextView(this@MainActivity).apply {
                    text = "Update wird heruntergeladen...\n\nBitte warten Sie."
                    textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                    setPadding(0, 30, 0, 0)
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                })
            }
            
            updateProgressDialog = AlertDialog.Builder(this)
                .setTitle("Update Download")
                .setView(progressView)
                .setCancelable(false)
                .create()
            
            updateProgressDialog?.show()
            
            // Start download
            val downloadId = updateService.downloadAndInstallUpdate(downloadUrl)
            
            // Register broadcast receiver to handle download completion
            registerDownloadReceiver(downloadId)
            
        } catch (e: Exception) {
            Logger.e("MainActivity", "Error downloading update", e)
            
            // Dismiss progress dialog on error
            updateProgressDialog?.dismiss()
            updateProgressDialog = null
            
            Toast.makeText(
                this,
                "Fehler beim Herunterladen des Updates: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Register receiver to handle download completion
     */
    private fun registerDownloadReceiver(downloadId: Long) {
        // Unregister any existing receiver first
        updateDownloadReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Logger.w("MainActivity", "Error unregistering previous receiver", e)
            }
        }
        
        // Create a proper BroadcastReceiver object
        val broadcastReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    Logger.i("MainActivity", "Download completed")
                    
                    // Get the downloaded file
                    val downloadManager = getSystemService(DOWNLOAD_SERVICE) as android.app.DownloadManager
                    val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS))
                        
                        if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                            // Use DownloadManager API to get the proper file URI
                            val fileUri = downloadManager.getUriForDownloadedFile(downloadId)
                            
                            if (fileUri != null) {
                                // Try to get the file path from the URI
                                val apkFile = try {
                                    // For file:// URIs, get the path directly
                                    if (fileUri.scheme == "file") {
                                        val path = fileUri.path
                                        if (path != null) java.io.File(path) else null
                                    } else {
                                        // For content:// URIs, try to get the filename from the cursor
                                        val localFilenameIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_LOCAL_FILENAME)
                                        if (localFilenameIndex >= 0) {
                                            val filename = cursor.getString(localFilenameIndex)
                                            if (filename != null) java.io.File(filename) else null
                                        } else null
                                    }
                                } catch (e: Exception) {
                                    Logger.e("MainActivity", "Error getting file path from URI", e)
                                    null
                                }
                                
                                if (apkFile != null && apkFile.exists()) {
                                    // Dismiss progress dialog
                                    updateProgressDialog?.dismiss()
                                    updateProgressDialog = null
                                    
                                    // Install the APK
                                    updateService.installApk(apkFile)
                                } else {
                                    Logger.e("MainActivity", "Downloaded APK file not found or URI not accessible")
                                    
                                    // Dismiss progress dialog
                                    updateProgressDialog?.dismiss()
                                    updateProgressDialog = null
                                    
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Update-Datei nicht gefunden",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                Logger.e("MainActivity", "Could not get URI for downloaded file")
                                
                                // Dismiss progress dialog
                                updateProgressDialog?.dismiss()
                                updateProgressDialog = null
                                
                                Toast.makeText(
                                    this@MainActivity,
                                    "Update-Datei nicht gefunden",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Logger.e("MainActivity", "Download failed with status: $status")
                            
                            // Dismiss progress dialog
                            updateProgressDialog?.dismiss()
                            updateProgressDialog = null
                            
                            Toast.makeText(
                                this@MainActivity,
                                "Update-Download fehlgeschlagen",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    cursor.close()
                    
                    // Unregister receiver
                    try {
                        unregisterReceiver(this)
                        updateDownloadReceiver = null
                    } catch (e: Exception) {
                        Logger.w("MainActivity", "Error unregistering receiver", e)
                    }
                }
            }
        }
        
        updateDownloadReceiver = broadcastReceiver
        registerReceiver(
            broadcastReceiver,
            android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up the progress dialog if still showing
        updateProgressDialog?.dismiss()
        updateProgressDialog = null
        
        // Clean up the download receiver if still registered
        updateDownloadReceiver?.let {
            try {
                unregisterReceiver(it)
                updateDownloadReceiver = null
            } catch (e: Exception) {
                Logger.w("MainActivity", "Error unregistering receiver in onDestroy", e)
            }
        }
    }
}
