package com.newsagent.ui

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.newsagent.services.TextToSpeechService
import com.newsagent.models.NewsSummary
import kotlinx.coroutines.launch

/**
 * Activity for displaying detailed news article
 */
class NewsDetailActivity : AppCompatActivity() {
    
    private lateinit var ttsService: TextToSpeechService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ttsService = TextToSpeechService(this)
        
        // Get article data from intent
        val title = intent.getStringExtra("article_title") ?: ""
        val content = intent.getStringExtra("article_content") ?: ""
        val source = intent.getStringExtra("article_source") ?: ""
        val author = intent.getStringExtra("article_author")
        val url = intent.getStringExtra("article_url") ?: ""
        
        val summaryText = intent.getStringExtra("summary_text")
        val summaryPoints = intent.getStringArrayExtra("summary_points")
        
        val credibilityScore = intent.getFloatExtra("credibility_score", 0f)
        val credibilityVerified = intent.getBooleanExtra("credibility_verified", false)
        val credibilityConcerns = intent.getStringArrayExtra("credibility_concerns")
        
        // Create UI
        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        // Title
        layout.addView(TextView(this).apply {
            text = title
            textSize = 24f
            setPadding(0, 0, 0, 16)
        })
        
        // Source and author
        layout.addView(TextView(this).apply {
            text = buildString {
                append(source)
                author?.let { append(" • $it") }
            }
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 0, 0, 16)
        })
        
        // Credibility score
        if (credibilityScore > 0) {
            layout.addView(TextView(this).apply {
                val percentage = (credibilityScore * 100).toInt()
                val status = if (credibilityVerified) "✓ Verifiziert" else "⚠ Nicht verifiziert"
                text = "Glaubwürdigkeit: $percentage% - $status"
                textSize = 16f
                setTextColor(if (credibilityVerified) 0xFF4CAF50.toInt() else 0xFFFFA726.toInt())
                setPadding(0, 0, 0, 8)
            })
            
            credibilityConcerns?.let { concerns ->
                if (concerns.isNotEmpty()) {
                    layout.addView(TextView(this).apply {
                        text = "Bedenken: ${concerns.joinToString(", ")}"
                        textSize = 12f
                        setTextColor(0xFFFF5722.toInt())
                        setPadding(0, 0, 0, 16)
                    })
                }
            }
        }
        
        // Summary section
        if (summaryText != null) {
            layout.addView(TextView(this).apply {
                text = "Zusammenfassung"
                textSize = 20f
                setPadding(0, 16, 0, 8)
            })
            
            layout.addView(TextView(this).apply {
                text = summaryText
                textSize = 16f
                setPadding(0, 0, 0, 16)
            })
            
            summaryPoints?.let { points ->
                if (points.isNotEmpty()) {
                    layout.addView(TextView(this).apply {
                        text = "Wichtige Punkte:"
                        textSize = 16f
                        setPadding(0, 0, 0, 8)
                    })
                    
                    points.forEach { point ->
                        layout.addView(TextView(this).apply {
                            text = "• $point"
                            textSize = 14f
                            setPadding(16, 4, 0, 4)
                        })
                    }
                }
            }
            
            // Audio playback button
            val articleId = intent.getStringExtra("article_id") ?: ""
            layout.addView(Button(this).apply {
                text = "Zusammenfassung anhören"
                setPadding(0, 16, 0, 0)
                setOnClickListener {
                    playAudioSummary(articleId, summaryText, summaryPoints?.toList() ?: emptyList())
                }
            })
        }
        
        // Content
        layout.addView(TextView(this).apply {
            text = "Vollständiger Artikel"
            textSize = 20f
            setPadding(0, 24, 0, 8)
        })
        
        layout.addView(TextView(this).apply {
            text = content
            textSize = 16f
            setPadding(0, 0, 0, 16)
        })
        
        scrollView.addView(layout)
        setContentView(scrollView)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun playAudioSummary(articleId: String, summaryText: String, keyPoints: List<String>) {
        lifecycleScope.launch {
            if (ttsService.initialize()) {
                val summary = NewsSummary(
                    articleId = articleId,
                    summary = summaryText,
                    keyPoints = keyPoints,
                    generatedAt = System.currentTimeMillis()
                )
                ttsService.speakSummary(summary)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ttsService.shutdown()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
