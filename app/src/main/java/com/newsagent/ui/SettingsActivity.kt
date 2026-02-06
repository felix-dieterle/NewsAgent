package com.newsagent.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.newsagent.services.NewsUpdateWorker
import com.newsagent.utils.Logger
import com.newsagent.utils.RateLimiter
import java.io.File

/**
 * Activity for configuring app settings
 */
class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        
        val scrollView = ScrollView(this)
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
                    selectedSource,
                    intervalInput.text.toString().toIntOrNull() ?: 60,
                    notificationsCheckbox.isChecked,
                    autoSummaryCheckbox.isChecked,
                    credibilityCheckbox.isChecked,
                    aiModeCheckbox.isChecked,
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
        
        // NewsAPI
        val newsApiStats = rateLimiter.getStats("news_api")
        newsApiStats?.let { stats ->
            val usagePercent = stats.currentRequests.toFloat() / stats.maxRequests
            val percentInt = (usagePercent * 100).toInt()
            
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            
            val indicator = RateLimitIndicatorView(this).apply {
                setUsagePercent(usagePercent)
            }
            container.addView(indicator)
            
            container.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(16, 1)
            })
            
            container.addView(TextView(this).apply {
                text = "NewsAPI: ${stats.remainingRequests}/${stats.maxRequests} ($percentInt%)"
                textSize = 14f
            })
            
            layout.addView(container)
        }
        
        // GNews
        val gnewsStats = rateLimiter.getStats("gnews_api")
        gnewsStats?.let { stats ->
            val usagePercent = stats.currentRequests.toFloat() / stats.maxRequests
            val percentInt = (usagePercent * 100).toInt()
            
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            
            val indicator = RateLimitIndicatorView(this).apply {
                setUsagePercent(usagePercent)
            }
            container.addView(indicator)
            
            container.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(16, 1)
            })
            
            container.addView(TextView(this).apply {
                text = "GNews: ${stats.remainingRequests}/${stats.maxRequests} ($percentInt%)"
                textSize = 14f
            })
            
            layout.addView(container)
        }
        
        // OpenRouter (AI)
        val openRouterStats = rateLimiter.getStats("openrouter_api")
        openRouterStats?.let { stats ->
            val usagePercent = stats.currentRequests.toFloat() / stats.maxRequests
            val percentInt = (usagePercent * 100).toInt()
            
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            
            val indicator = RateLimitIndicatorView(this).apply {
                setUsagePercent(usagePercent)
            }
            container.addView(indicator)
            
            container.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(16, 1)
            })
            
            container.addView(TextView(this).apply {
                text = "AI API: ${stats.remainingRequests}/${stats.maxRequests} ($percentInt%)"
                textSize = 14f
            })
            
            layout.addView(container)
        }
        
        layout.addView(TextView(this).apply {
            text = "🟢 Grün: < 70% | 🟡 Gelb: 70-99% | 🔴 Rot: Limit erreicht"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 16, 0, 0)
        })
    }
    
    private fun saveSettings(
        newsApiToken: String,
        aiApiToken: String,
        newsSource: String,
        intervalMinutes: Int,
        enableNotifications: Boolean,
        enableAutoSummary: Boolean,
        enableCredibilityCheck: Boolean,
        aiModeEnabled: Boolean,
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
            putInt("update_interval_minutes", intervalMinutes)
            putBoolean("enable_notifications", enableNotifications)
            putBoolean("enable_auto_summary", enableAutoSummary)
            putBoolean("enable_credibility_check", enableCredibilityCheck)
            putBoolean("ai_mode_enabled", aiModeEnabled)
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
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
