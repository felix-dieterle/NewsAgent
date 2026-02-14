package com.newsagent.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.newsagent.models.NewsArticle

/**
 * Adapter for displaying news articles in RecyclerView
 */
class NewsAdapter(
    private val articles: List<NewsArticle>,
    private val onItemClick: (NewsArticle) -> Unit
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {
    
    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = TextView(view.context).apply {
            textSize = 18f
            setPadding(16, 16, 16, 8)
        }
        
        val source: TextView = TextView(view.context).apply {
            textSize = 14f
            setPadding(16, 0, 16, 8)
            setTextColor(0xFF666666.toInt())
        }
        
        val credibility: TextView = TextView(view.context).apply {
            textSize = 12f
            setPadding(16, 0, 16, 16)
            setTextColor(0xFF888888.toInt())
        }
        
        init {
            (view as android.widget.LinearLayout).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                addView(title)
                addView(source)
                addView(credibility)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val layout = android.widget.LinearLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundResource(android.R.drawable.list_selector_background)
            isClickable = true
            isFocusable = true
        }
        
        return NewsViewHolder(layout)
    }
    
    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article = articles[position]
        
        // Add category prefix and read indicator to title
        val titlePrefix = buildString {
            if (article.isRead) {
                append("✓ ")  // Checkmark for read articles
            }
            article.category?.let { category ->
                append("[$category] ")
            }
        }
        holder.title.text = titlePrefix + article.title
        
        // Dim read articles
        if (article.isRead) {
            holder.title.alpha = 0.6f
            holder.source.alpha = 0.6f
            holder.credibility.alpha = 0.6f
        } else {
            holder.title.alpha = 1.0f
            holder.source.alpha = 1.0f
            holder.credibility.alpha = 1.0f
        }
        
        holder.source.text = "${article.source} • ${article.publishedAt}"
        
        val credibilityText = article.credibilityScore?.let { score ->
            val percentage = (score.score * 100).toInt()
            val status = if (score.verified) "✓" else "⚠"
            "$status Glaubwürdigkeit: $percentage%"
        } ?: ""
        holder.credibility.text = credibilityText
        
        holder.itemView.setOnClickListener {
            onItemClick(article)
        }
    }
    
    override fun getItemCount() = articles.size
}
