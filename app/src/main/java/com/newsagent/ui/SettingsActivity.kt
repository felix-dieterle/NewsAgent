package com.newsagent.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.newsagent.services.NewsUpdateWorker
import com.newsagent.utils.Logger
import com.newsagent.utils.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Activity for configuring app settings
 */
class SettingsActivity : AppCompatActivity() {
    
    companion object {
        /**
         * Maximum length for error messages displayed in API test results
         */
        private const val ERROR_MESSAGE_MAX_LENGTH = 50
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        
        val scrollView = ScrollView(this).apply {
            // Add top padding to prevent overlap with action bar
            // Add bottom padding to prevent overlap with navigation bar (120dp)
            val bottomPaddingDp = 120
            val bottomPaddingPx = (bottomPaddingDp * resources.displayMetrics.density).toInt()
            setPadding(0, 16, 0, bottomPaddingPx)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        // News Source Selection
        layout.addView(TextView(this).apply {
            text = "Nachrichtenquelle"
            textSize = 18f
            setPadding(0, 16, 0, 8)
        })
        
        val newsSourceSpinner = Spinner(this).apply {
            val sources = arrayOf("NewsAPI.org", "GNews.io", "RSS Feeds (kostenlos)")
            val adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_item, sources)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            setAdapter(adapter)
            
            // Set current selection
            val currentSource = prefs.getString("news_source", "newsapi") ?: "newsapi"
            setSelection(when (currentSource) {
                "gnews" -> 1
                "rss" -> 2
                else -> 0
            })
        }
        layout.addView(newsSourceSpinner)
        
        layout.addView(TextView(this).apply {
            text = "Wählen Sie Ihre bevorzugte Nachrichtenquelle"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 16)
        })
        
        // News API Token
        layout.addView(TextView(this).apply {
            text = "News API Token"
            textSize = 18f
            setPadding(0, 16, 0, 8)
        })
        
        val newsApiTokenInput = EditText(this).apply {
            hint = "Token für die gewählte Quelle (nicht für RSS benötigt)"
            setText(prefs.getString("news_api_token", ""))
        }
        layout.addView(newsApiTokenInput)
        
        layout.addView(TextView(this).apply {
            text = "NewsAPI.org oder GNews.io - holen Sie Token von der gewählten Quelle"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 16)
        })
        
        // AI API Token (OpenRouter for summaries)
        layout.addView(TextView(this).apply {
            text = "AI API Token"
            textSize = 18f
            setPadding(0, 16, 0, 8)
        })
        
        val aiApiTokenInput = EditText(this).apply {
            hint = "Geben Sie Ihren AI API Token ein"
            setText(prefs.getString("openrouter_api_key", ""))
        }
        layout.addView(aiApiTokenInput)
        
        layout.addView(TextView(this).apply {
            text = "OpenRouter für KI-Zusammenfassungen - https://openrouter.ai"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 16)
        })
        
        // Google Custom Search API Key
        layout.addView(TextView(this).apply {
            text = "Google Custom Search API Key"
            textSize = 18f
            setPadding(0, 16, 0, 8)
        })
        
        val googleApiKeyInput = EditText(this).apply {
            hint = "Geben Sie Ihren Google API Key ein"
            setText(prefs.getString("google_api_key", ""))
        }
        layout.addView(googleApiKeyInput)
        
        layout.addView(TextView(this).apply {
            text = "Google Cloud Console API Key - https://console.cloud.google.com/apis/credentials"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 8)
        })
        
        // Google Custom Search Engine ID
        layout.addView(TextView(this).apply {
            text = "Google Custom Search Engine ID (cx)"
            textSize = 18f
            setPadding(0, 8, 0, 8)
        })
        
        val googleSearchEngineIdInput = EditText(this).apply {
            hint = "Geben Sie Ihre Search Engine ID (cx) ein"
            setText(prefs.getString("google_search_engine_id", ""))
        }
        layout.addView(googleSearchEngineIdInput)
        
        layout.addView(TextView(this).apply {
            text = "Programmable Search Engine ID - https://programmablesearchengine.google.com/\n\nHinweis: Google Custom Search API benötigt BEIDE Felder (API Key + Engine ID). Alternativ können Sie SerpApi verwenden, das nur einen Key benötigt."
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 16)
        })
        
        // Update Interval
        layout.addView(TextView(this).apply {
            text = "Update-Intervall (Minuten)"
            textSize = 18f
            setPadding(0, 16, 0, 8)
        })
        
        val intervalInput = EditText(this).apply {
            hint = "60"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("update_interval_minutes", 60).toString())
        }
        layout.addView(intervalInput)
        
        // Enable Notifications
        val notificationsCheckbox = CheckBox(this).apply {
            text = "Benachrichtigungen aktivieren"
            isChecked = prefs.getBoolean("enable_notifications", true)
            setPadding(0, 16, 0, 0)
        }
        layout.addView(notificationsCheckbox)
        
        // Enable Auto Summary
        val autoSummaryCheckbox = CheckBox(this).apply {
            text = "Automatische Zusammenfassungen"
            isChecked = prefs.getBoolean("enable_auto_summary", true)
            setPadding(0, 8, 0, 0)
        }
        layout.addView(autoSummaryCheckbox)
        
        // Enable Credibility Check
        val credibilityCheckbox = CheckBox(this).apply {
            text = "Glaubwürdigkeitsprüfung aktivieren"
            isChecked = prefs.getBoolean("enable_credibility_check", true)
            setPadding(0, 8, 0, 0)
        }
        layout.addView(credibilityCheckbox)
        
        // AI Mode Toggle
        val aiModeCheckbox = CheckBox(this).apply {
            text = "AI-Modus (bevorzugt Qualität über Kosten)"
            isChecked = prefs.getBoolean("ai_mode_enabled", false)
            setPadding(0, 8, 0, 0)
        }
        layout.addView(aiModeCheckbox)
        
        layout.addView(TextView(this).apply {
            text = "Im AI-Modus werden kostenpflichtige APIs für bessere Ergebnisse priorisiert. Im Standard-Modus werden kostenlose RSS-Feeds bevorzugt."
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 0)
        })
        
        // Auto Update Toggle
        val autoUpdateCheckbox = CheckBox(this).apply {
            text = "Automatische Updates aktivieren"
            isChecked = prefs.getBoolean("auto_update_enabled", true)
            setPadding(0, 8, 0, 0)
        }
        layout.addView(autoUpdateCheckbox)
        
        layout.addView(TextView(this).apply {
            text = "Prüft beim App-Start auf neue Versionen von GitHub"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 0)
        })
        
        // Max Articles
        layout.addView(TextView(this).apply {
            text = "Maximale Artikel pro Update"
            textSize = 18f
            setPadding(0, 16, 0, 8)
        })
        
        val maxArticlesInput = EditText(this).apply {
            hint = "10"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("max_articles", 10).toString())
        }
        layout.addView(maxArticlesInput)
        
        // API Rate Limits Section
        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                setMargins(0, 32, 0, 16)
            }
            setBackgroundColor(0xFFCCCCCC.toInt())
        })
        
        layout.addView(TextView(this).apply {
            text = "API Rate Limits"
            textSize = 20f
            setPadding(0, 8, 0, 16)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        
        // Add rate limit indicators and details
        addRateLimitStatus(layout)
        
        // Divider
        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                setMargins(0, 32, 0, 16)
            }
            setBackgroundColor(0xFFCCCCCC.toInt())
        })
        
        // Troubleshooting Section Header
        layout.addView(TextView(this).apply {
            text = "Fehlerbehebung"
            textSize = 20f
            setPadding(0, 8, 0, 16)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        
        // View Logs Button
        layout.addView(Button(this).apply {
            text = "App-Logs anzeigen"
            setPadding(0, 8, 0, 0)
            setOnClickListener {
                showLogs()
            }
        })
        
        // Clear Logs Button
        layout.addView(Button(this).apply {
            text = "Logs löschen"
            setPadding(0, 8, 0, 0)
            setOnClickListener {
                clearLogs()
            }
        })
        
        // Share Logs Button
        layout.addView(Button(this).apply {
            text = "Logs teilen (für Support)"
            setPadding(0, 8, 0, 0)
            setOnClickListener {
                shareLogs()
            }
        })
        
        // Test All APIs Button
        layout.addView(Button(this).apply {
            text = "Alle APIs testen"
            setPadding(0, 8, 0, 0)
            setOnClickListener {
                testAllApis()
            }
        })
        
        // Test Source Connection Button
        layout.addView(Button(this).apply {
            text = "Quelle testen (alle Artikel anzeigen)"
            setPadding(0, 8, 0, 0)
            setOnClickListener {
                testSourceConnection()
            }
        })
        
        // Save Button
        layout.addView(Button(this).apply {
            text = "Einstellungen speichern"
            setPadding(0, 32, 0, 0)
            setOnClickListener {
                val selectedSource = when (newsSourceSpinner.selectedItemPosition) {
                    1 -> "gnews"
                    2 -> "rss"
                    else -> "newsapi"
                }
                saveSettings(
                    newsApiTokenInput.text.toString(),
                    aiApiTokenInput.text.toString(),
                    googleApiKeyInput.text.toString(),
                    googleSearchEngineIdInput.text.toString(),
                    selectedSource,
                    intervalInput.text.toString().toIntOrNull() ?: 60,
                    notificationsCheckbox.isChecked,
                    autoSummaryCheckbox.isChecked,
                    credibilityCheckbox.isChecked,
                    aiModeCheckbox.isChecked,
                    autoUpdateCheckbox.isChecked,
                    maxArticlesInput.text.toString().toIntOrNull() ?: 10
                )
            }
        })
        
        scrollView.addView(layout)
        setContentView(scrollView)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun addRateLimitStatus(layout: LinearLayout) {
        val rateLimiter = RateLimiter.getInstance()
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        
        // NewsAPI
        val newsApiStats = rateLimiter.getStats("news_api")
        newsApiStats?.let { stats ->
            val newsApiKey = prefs.getString("news_api_key", "") ?: ""
            val hasApiKey = newsApiKey.isNotEmpty()
            
            val usagePercent = stats.currentRequests.toFloat() / stats.maxRequests
            val percentInt = (usagePercent * 100).toInt()
            
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            
            val indicator = RateLimitIndicatorView(this).apply {
                if (hasApiKey) {
                    setUsagePercent(usagePercent)
                } else {
                    setColor(Color.GRAY)
                }
            }
            container.addView(indicator)
            
            container.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(16, 1)
            })
            
            container.addView(TextView(this).apply {
                text = if (hasApiKey) {
                    "NewsAPI: ${stats.remainingRequests}/${stats.maxRequests} ($percentInt%)"
                } else {
                    "NewsAPI: Kein API-Key konfiguriert"
                }
                textSize = 14f
            })
            
            layout.addView(container)
        }
        
        // GNews
        val gnewsStats = rateLimiter.getStats("gnews_api")
        gnewsStats?.let { stats ->
            val gnewsApiKey = prefs.getString("gnews_api_token", "") ?: ""
            val hasApiKey = gnewsApiKey.isNotEmpty()
            
            val usagePercent = stats.currentRequests.toFloat() / stats.maxRequests
            val percentInt = (usagePercent * 100).toInt()
            
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            
            val indicator = RateLimitIndicatorView(this).apply {
                if (hasApiKey) {
                    setUsagePercent(usagePercent)
                } else {
                    setColor(Color.GRAY)
                }
            }
            container.addView(indicator)
            
            container.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(16, 1)
            })
            
            container.addView(TextView(this).apply {
                text = if (hasApiKey) {
                    "GNews: ${stats.remainingRequests}/${stats.maxRequests} ($percentInt%)"
                } else {
                    "GNews: Kein API-Key konfiguriert"
                }
                textSize = 14f
            })
            
            layout.addView(container)
        }
        
        // OpenRouter (AI)
        val openRouterStats = rateLimiter.getStats("openrouter_api")
        openRouterStats?.let { stats ->
            val aiApiKey = prefs.getString("openrouter_api_key", "") ?: ""
            val hasApiKey = aiApiKey.isNotEmpty()
            
            val usagePercent = stats.currentRequests.toFloat() / stats.maxRequests
            val percentInt = (usagePercent * 100).toInt()
            
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            
            val indicator = RateLimitIndicatorView(this).apply {
                if (hasApiKey) {
                    setUsagePercent(usagePercent)
                } else {
                    setColor(Color.GRAY)
                }
            }
            container.addView(indicator)
            
            container.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(16, 1)
            })
            
            container.addView(TextView(this).apply {
                text = if (hasApiKey) {
                    "AI API: ${stats.remainingRequests}/${stats.maxRequests} ($percentInt%)"
                } else {
                    "AI API: Kein API-Key konfiguriert"
                }
                textSize = 14f
            })
            
            layout.addView(container)
        }
        
        // Google Custom Search
        val googleStats = rateLimiter.getStats("google_custom_search")
        googleStats?.let { stats ->
            val googleApiKey = prefs.getString("google_api_key", "") ?: ""
            val googleSearchEngineId = prefs.getString("google_search_engine_id", "") ?: ""
            val hasApiKey = googleApiKey.isNotEmpty() && googleSearchEngineId.isNotEmpty()
            
            val usagePercent = stats.currentRequests.toFloat() / stats.maxRequests
            val percentInt = (usagePercent * 100).toInt()
            
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            
            val indicator = RateLimitIndicatorView(this).apply {
                if (hasApiKey) {
                    setUsagePercent(usagePercent)
                } else {
                    setColor(Color.GRAY)
                }
            }
            container.addView(indicator)
            
            container.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(16, 1)
            })
            
            container.addView(TextView(this).apply {
                text = if (hasApiKey) {
                    "Google Search: ${stats.remainingRequests}/${stats.maxRequests} ($percentInt%)"
                } else {
                    "Google Search: Kein API-Key konfiguriert"
                }
                textSize = 14f
            })
            
            layout.addView(container)
        }
        
        layout.addView(TextView(this).apply {
            text = "⚪ Grau: Kein API-Key | 🟢 Grün: < 70% | 🟡 Gelb: 70-99% | 🔴 Rot: Limit erreicht"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 16, 0, 0)
        })
    }
    
    private fun saveSettings(
        newsApiToken: String,
        aiApiToken: String,
        googleApiKey: String,
        googleSearchEngineId: String,
        newsSource: String,
        intervalMinutes: Int,
        enableNotifications: Boolean,
        enableAutoSummary: Boolean,
        enableCredibilityCheck: Boolean,
        aiModeEnabled: Boolean,
        autoUpdateEnabled: Boolean,
        maxArticles: Int
    ) {
        Logger.d("SettingsActivity", "Saving settings...")
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            // Save news source selection
            putString("news_source", newsSource)
            
            // Save token based on selected source
            when (newsSource) {
                "newsapi" -> {
                    putString("news_api_key", newsApiToken)
                    putString("news_api_token", newsApiToken)
                }
                "gnews" -> {
                    putString("gnews_api_token", newsApiToken)
                    putString("news_api_token", newsApiToken)
                }
                "rss" -> {
                    // RSS doesn't need a token, but store the selection
                    putString("news_api_token", "")
                }
            }
            
            putString("openrouter_api_key", aiApiToken)
            putString("google_api_key", googleApiKey)
            putString("google_search_engine_id", googleSearchEngineId)
            putInt("update_interval_minutes", intervalMinutes)
            putBoolean("enable_notifications", enableNotifications)
            putBoolean("enable_auto_summary", enableAutoSummary)
            putBoolean("enable_credibility_check", enableCredibilityCheck)
            putBoolean("ai_mode_enabled", aiModeEnabled)
            putBoolean("auto_update_enabled", autoUpdateEnabled)
            putInt("max_articles", maxArticles)
            apply()
        }
        
        Logger.i("SettingsActivity", "Settings saved: interval=$intervalMinutes, notifications=$enableNotifications")
        
        // Reschedule worker with new interval
        NewsUpdateWorker.schedule(this, intervalMinutes.toLong())
        
        Toast.makeText(this, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    /**
     * Show logs in a dialog
     */
    private fun showLogs() {
        Logger.d("SettingsActivity", "Showing logs dialog")
        val logs = Logger.readLogs(this)
        
        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            text = logs
            setPadding(16, 16, 16, 16)
            textSize = 10f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        scrollView.addView(textView)
        
        AlertDialog.Builder(this)
            .setTitle("App-Logs")
            .setView(scrollView)
            .setPositiveButton("Schließen", null)
            .setNegativeButton("Teilen") { _, _ ->
                shareLogs()
            }
            .show()
    }
    
    /**
     * Clear all logs
     */
    private fun clearLogs() {
        Logger.d("SettingsActivity", "Clearing logs")
        Logger.clearLogs(this)
        Toast.makeText(this, "Logs gelöscht", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Share logs via Intent using FileProvider for large files
     */
    private fun shareLogs() {
        Logger.d("SettingsActivity", "Sharing logs")
        
        try {
            // Create a temporary text file with the logs
            val logsFile = File(filesDir, "newsagent_logs_export.txt")
            logsFile.writeText(Logger.readLogs(this))
            
            // Use FileProvider to share the file
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "com.newsagent.fileprovider",
                logsFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "NewsAgent App Logs")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Logs teilen via"))
        } catch (e: Exception) {
            Logger.e("SettingsActivity", "Failed to share logs", e)
            Toast.makeText(this, "Fehler beim Teilen der Logs: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Test source connection and display all available articles
     */
    private fun testSourceConnection() {
        Logger.d("SettingsActivity", "Testing source connection")
        
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        val currentSource = prefs.getString("news_source", "newsapi") ?: "newsapi"
        
        val sourceName = when (currentSource) {
            "gnews" -> "GNews.io"
            "rss" -> "RSS Feeds"
            else -> "NewsAPI.org"
        }
        
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("Quelle wird getestet...")
            .setMessage("Lade alle verfügbaren Artikel von $sourceName...\n\nBitte warten Sie.")
            .setCancelable(false)
            .show()
        
        // Fetch articles in background using coroutines
        lifecycleScope.launch {
            try {
                val newsRepository = com.newsagent.services.NewsRepository(this@SettingsActivity)
                val articles = withContext(Dispatchers.IO) {
                    when (currentSource) {
                        "gnews" -> newsRepository.fetchTopHeadlinesFree()
                        "rss" -> newsRepository.fetchRssNews()
                        else -> newsRepository.fetchTopHeadlines()
                    }
                }
                
                loadingDialog.dismiss()
                
                if (articles.isEmpty()) {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("⚠️ Keine Artikel gefunden")
                        .setMessage(
                            "Quelle: $sourceName\n\n" +
                            "Es wurden keine Artikel gefunden.\n\n" +
                            "Mögliche Ursachen:\n" +
                            "• API-Schlüssel fehlt oder ist ungültig\n" +
                            "• Rate Limit erreicht\n" +
                            "• Netzwerkverbindung fehlt\n" +
                            "• RSS-Feeds sind nicht verfügbar\n\n" +
                            "Überprüfen Sie die Logs für Details."
                        )
                        .setPositiveButton("Logs anzeigen") { _, _ ->
                            showLogs()
                        }
                        .setNegativeButton("Schließen", null)
                        .show()
                } else {
                    // Show article list
                    val articleTitles = articles.mapIndexed { index, article ->
                        "${index + 1}. ${article.title}\n   Quelle: ${article.source}\n   ${article.publishedAt ?: "Kein Datum"}"
                    }
                    
                    val scrollView = ScrollView(this@SettingsActivity)
                    val textView = TextView(this@SettingsActivity).apply {
                        text = buildString {
                            appendLine("✅ Erfolgreich geladen!")
                            appendLine()
                            appendLine("Quelle: $sourceName")
                            appendLine("Anzahl: ${articles.size} Artikel")
                            appendLine()
                            appendLine("═══════════════════════════════")
                            appendLine()
                            articleTitles.forEach { title ->
                                appendLine(title)
                                appendLine()
                            }
                        }
                        setPadding(32, 32, 32, 32)
                        textSize = 14f
                        setTextIsSelectable(true)
                    }
                    scrollView.addView(textView)
                    
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Artikel von $sourceName")
                        .setView(scrollView)
                        .setPositiveButton("Schließen", null)
                        .setNeutralButton("Logs anzeigen") { _, _ ->
                            showLogs()
                        }
                        .show()
                }
            } catch (e: Exception) {
                Logger.e("SettingsActivity", "Error testing source connection", e)
                loadingDialog.dismiss()
                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("❌ Fehler beim Testen")
                    .setMessage(
                        "Quelle: $sourceName\n\n" +
                        "Fehler: ${e.message}\n\n" +
                        "Überprüfen Sie die Logs für Details."
                    )
                    .setPositiveButton("Logs anzeigen") { _, _ ->
                        showLogs()
                    }
                    .setNegativeButton("Schließen", null)
                    .show()
            }
        }
    }
    
    /**
     * Test all configured APIs
     */
    private fun testAllApis() {
        Logger.d("SettingsActivity", "Testing all APIs")
        
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        
        // Get all API keys
        val newsApiKey = prefs.getString("news_api_key", "") ?: ""
        val gnewsApiKey = prefs.getString("gnews_api_token", "") ?: ""
        val openRouterKey = prefs.getString("openrouter_api_key", "") ?: ""
        val googleApiKey = prefs.getString("google_api_key", "") ?: ""
        val googleSearchEngineId = prefs.getString("google_search_engine_id", "") ?: ""
        
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("APIs werden getestet...")
            .setMessage("Teste alle konfigurierten APIs...\n\nBitte warten Sie.")
            .setCancelable(false)
            .show()
        
        // Test APIs in background
        lifecycleScope.launch {
            val results = mutableListOf<String>()
            
            try {
                // Test NewsAPI
                if (newsApiKey.isNotEmpty()) {
                    val newsApiResult = testNewsApi(newsApiKey)
                    results.add("NewsAPI.org: $newsApiResult")
                } else {
                    results.add("NewsAPI.org: ⚪ Kein API-Key konfiguriert")
                }
                
                // Test GNews
                if (gnewsApiKey.isNotEmpty()) {
                    val gnewsResult = testGNewsApi(gnewsApiKey)
                    results.add("GNews.io: $gnewsResult")
                } else {
                    results.add("GNews.io: ⚪ Kein API-Key konfiguriert")
                }
                
                // Test OpenRouter (AI)
                if (openRouterKey.isNotEmpty()) {
                    val aiResult = testOpenRouterApi(openRouterKey)
                    results.add("OpenRouter AI: $aiResult")
                } else {
                    results.add("OpenRouter AI: ⚪ Kein API-Key konfiguriert")
                }
                
                // Test Google Custom Search
                if (googleApiKey.isNotEmpty() && googleSearchEngineId.isNotEmpty()) {
                    val googleResult = testGoogleCustomSearchApi(googleApiKey, googleSearchEngineId)
                    results.add("Google Custom Search: $googleResult")
                } else if (googleApiKey.isEmpty() && googleSearchEngineId.isEmpty()) {
                    results.add("Google Custom Search: ⚪ Kein API-Key konfiguriert")
                } else if (googleApiKey.isEmpty()) {
                    results.add("Google Custom Search: ⚠️ API-Key fehlt")
                } else {
                    results.add("Google Custom Search: ⚠️ Search Engine ID fehlt")
                }
                
                loadingDialog.dismiss()
                
                // Show results dialog
                val scrollView = ScrollView(this@SettingsActivity)
                val textView = TextView(this@SettingsActivity).apply {
                    text = buildString {
                        appendLine("API Test Ergebnisse")
                        appendLine("═══════════════════════════════")
                        appendLine()
                        results.forEach { result ->
                            appendLine(result)
                            appendLine()
                        }
                        appendLine("═══════════════════════════════")
                        appendLine()
                        appendLine("✅ = Erfolgreich")
                        appendLine("❌ = Fehler")
                        appendLine("⚠️ = Warnung/Teilweise konfiguriert")
                        appendLine("⚪ = Nicht konfiguriert")
                    }
                    setPadding(32, 32, 32, 32)
                    textSize = 14f
                    setTextIsSelectable(true)
                }
                scrollView.addView(textView)
                
                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("API Test Ergebnisse")
                    .setView(scrollView)
                    .setPositiveButton("Schließen", null)
                    .setNeutralButton("Logs anzeigen") { _, _ ->
                        showLogs()
                    }
                    .show()
                    
            } catch (e: Exception) {
                Logger.e("SettingsActivity", "Error testing APIs", e)
                loadingDialog.dismiss()
                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("❌ Fehler beim Testen")
                    .setMessage("Fehler: ${e.message}\n\nÜberprüfen Sie die Logs für Details.")
                    .setPositiveButton("Logs anzeigen") { _, _ ->
                        showLogs()
                    }
                    .setNegativeButton("Schließen", null)
                    .show()
            }
        }
    }
    
    /**
     * Test NewsAPI.org API
     */
    private suspend fun testNewsApi(apiKey: String): String = withContext(Dispatchers.IO) {
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        val originalKey = prefs.getString("news_api_key", "")
        
        try {
            // Save temp API key synchronously
            prefs.edit().putString("news_api_key", apiKey).commit()
            
            val newsRepository = com.newsagent.services.NewsRepository(this@SettingsActivity)
            val articles = newsRepository.fetchTopHeadlines()
            
            val result = if (articles.isNotEmpty()) {
                "✅ Erfolgreich (${articles.size} Artikel)"
            } else {
                "⚠️ Keine Artikel gefunden"
            }
            
            // Restore original key synchronously
            prefs.edit().putString("news_api_key", originalKey).commit()
            result
        } catch (e: Exception) {
            // Restore original key synchronously on error
            prefs.edit().putString("news_api_key", originalKey).commit()
            Logger.e("SettingsActivity", "NewsAPI test failed", e)
            "❌ Fehler: ${e.message?.take(ERROR_MESSAGE_MAX_LENGTH)}"
        }
    }
    
    /**
     * Test GNews.io API
     */
    private suspend fun testGNewsApi(apiKey: String): String = withContext(Dispatchers.IO) {
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        val originalKey = prefs.getString("gnews_api_token", "")
        
        try {
            // Save temp API key synchronously
            prefs.edit().putString("gnews_api_token", apiKey).commit()
            
            val newsRepository = com.newsagent.services.NewsRepository(this@SettingsActivity)
            val articles = newsRepository.fetchTopHeadlinesFree()
            
            val result = if (articles.isNotEmpty()) {
                "✅ Erfolgreich (${articles.size} Artikel)"
            } else {
                "⚠️ Keine Artikel gefunden"
            }
            
            // Restore original key synchronously
            prefs.edit().putString("gnews_api_token", originalKey).commit()
            result
        } catch (e: Exception) {
            // Restore original key synchronously on error
            prefs.edit().putString("gnews_api_token", originalKey).commit()
            Logger.e("SettingsActivity", "GNews test failed", e)
            "❌ Fehler: ${e.message?.take(ERROR_MESSAGE_MAX_LENGTH)}"
        }
    }
    
    /**
     * Test OpenRouter AI API
     */
    private suspend fun testOpenRouterApi(apiKey: String): String = withContext(Dispatchers.IO) {
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        val originalKey = prefs.getString("openrouter_api_key", "")
        
        try {
            // Save temp API key synchronously
            prefs.edit().putString("openrouter_api_key", apiKey).commit()
            
            val aiService = com.newsagent.services.AiSummaryService(this@SettingsActivity)
            
            // Create a test article with unique URL to avoid cache hits
            val testArticle = com.newsagent.models.NewsArticle(
                id = "test",
                title = "Test Article",
                description = "This is a test article to verify API connectivity",
                content = "Test content for API verification",
                url = "https://example.com/test-${System.currentTimeMillis()}",
                source = "Test",
                publishedAt = "",
                imageUrl = null,
                author = null
            )
            
            val summary = aiService.generateSummary(testArticle)
            
            val result = if (summary != null) {
                "✅ Erfolgreich (AI antwortet)"
            } else {
                "⚠️ Keine Antwort von AI"
            }
            
            // Restore original key synchronously
            prefs.edit().putString("openrouter_api_key", originalKey).commit()
            result
        } catch (e: Exception) {
            // Restore original key synchronously on error
            prefs.edit().putString("openrouter_api_key", originalKey).commit()
            Logger.e("SettingsActivity", "OpenRouter test failed", e)
            "❌ Fehler: ${e.message?.take(ERROR_MESSAGE_MAX_LENGTH)}"
        }
    }
    
    /**
     * Test Google Custom Search API
     */
    private suspend fun testGoogleCustomSearchApi(apiKey: String, searchEngineId: String): String = withContext(Dispatchers.IO) {
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        val originalKey = prefs.getString("google_api_key", "")
        val originalEngineId = prefs.getString("google_search_engine_id", "")
        
        try {
            // Save temp API keys synchronously
            prefs.edit()
                .putString("google_api_key", apiKey)
                .putString("google_search_engine_id", searchEngineId)
                .commit()
            
            val newsRepository = com.newsagent.services.NewsRepository(this@SettingsActivity)
            val articles = newsRepository.searchGoogleCustomSearch("news")
            
            val result = if (articles.isNotEmpty()) {
                "✅ Erfolgreich (${articles.size} Artikel)"
            } else {
                "⚠️ Keine Artikel gefunden"
            }
            
            // Restore original keys synchronously
            prefs.edit()
                .putString("google_api_key", originalKey)
                .putString("google_search_engine_id", originalEngineId)
                .commit()
            result
        } catch (e: Exception) {
            // Restore original keys synchronously on error
            prefs.edit()
                .putString("google_api_key", originalKey)
                .putString("google_search_engine_id", originalEngineId)
                .commit()
            Logger.e("SettingsActivity", "Google Custom Search test failed", e)
            "❌ Fehler: ${e.message?.take(ERROR_MESSAGE_MAX_LENGTH)}"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
