package com.newsagent.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.newsagent.services.NewsUpdateWorker

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
        
        // News API Key
        layout.addView(TextView(this).apply {
            text = "News API Schlüssel"
            textSize = 18f
            setPadding(0, 16, 0, 8)
        })
        
        val newsApiKeyInput = EditText(this).apply {
            hint = "Geben Sie Ihren News API Schlüssel ein"
            setText(prefs.getString("news_api_key", ""))
        }
        layout.addView(newsApiKeyInput)
        
        layout.addView(TextView(this).apply {
            text = "Erhalten Sie einen kostenlosen Schlüssel auf https://newsapi.org"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 16)
        })
        
        // OpenRouter API Key
        layout.addView(TextView(this).apply {
            text = "OpenRouter API Schlüssel"
            textSize = 18f
            setPadding(0, 16, 0, 8)
        })
        
        val openRouterApiKeyInput = EditText(this).apply {
            hint = "Geben Sie Ihren OpenRouter API Schlüssel ein"
            setText(prefs.getString("openrouter_api_key", ""))
        }
        layout.addView(openRouterApiKeyInput)
        
        layout.addView(TextView(this).apply {
            text = "Erhalten Sie einen Schlüssel auf https://openrouter.ai"
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
        
        // Save Button
        layout.addView(Button(this).apply {
            text = "Einstellungen speichern"
            setPadding(0, 32, 0, 0)
            setOnClickListener {
                saveSettings(
                    newsApiKeyInput.text.toString(),
                    openRouterApiKeyInput.text.toString(),
                    intervalInput.text.toString().toIntOrNull() ?: 60,
                    notificationsCheckbox.isChecked,
                    autoSummaryCheckbox.isChecked,
                    credibilityCheckbox.isChecked,
                    maxArticlesInput.text.toString().toIntOrNull() ?: 10
                )
            }
        })
        
        scrollView.addView(layout)
        setContentView(scrollView)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun saveSettings(
        newsApiKey: String,
        openRouterApiKey: String,
        intervalMinutes: Int,
        enableNotifications: Boolean,
        enableAutoSummary: Boolean,
        enableCredibilityCheck: Boolean,
        maxArticles: Int
    ) {
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("news_api_key", newsApiKey)
            putString("openrouter_api_key", openRouterApiKey)
            putInt("update_interval_minutes", intervalMinutes)
            putBoolean("enable_notifications", enableNotifications)
            putBoolean("enable_auto_summary", enableAutoSummary)
            putBoolean("enable_credibility_check", enableCredibilityCheck)
            putInt("max_articles", maxArticles)
            apply()
        }
        
        // Reschedule worker with new interval
        NewsUpdateWorker.schedule(this, intervalMinutes.toLong())
        
        Toast.makeText(this, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
