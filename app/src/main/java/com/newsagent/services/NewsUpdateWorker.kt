package com.newsagent.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.newsagent.models.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background worker for periodic news updates
 */
class NewsUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val newsRepository = NewsRepository(applicationContext)
    private val aiSummaryService = AiSummaryService(applicationContext)
    private val credibilityService = CredibilityCheckService(applicationContext)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences("newsagent_prefs", Context.MODE_PRIVATE)
            
            // Fetch latest news
            val articles = newsRepository.fetchTopHeadlines()
            
            if (articles.isEmpty()) {
                return@withContext Result.success()
            }
            
            // Process articles
            val processedArticles = mutableListOf<NewsArticle>()
            
            for (article in articles.take(prefs.getInt("max_articles", 10))) {
                // Generate summary if enabled
                if (prefs.getBoolean("enable_auto_summary", true)) {
                    article.summary = aiSummaryService.generateSummary(article)
                }
                
                // Check credibility if enabled
                if (prefs.getBoolean("enable_credibility_check", true)) {
                    article.credibilityScore = credibilityService.checkCredibility(article)
                }
                
                processedArticles.add(article)
            }
            
            // Show notification if enabled
            if (prefs.getBoolean("enable_notifications", true)) {
                showNotification(processedArticles.size)
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
    
    private fun showNotification(articleCount: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "News Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new news articles"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Neue Nachrichten")
            .setContentText("$articleCount neue Artikel verfügbar")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    companion object {
        private const val CHANNEL_ID = "news_updates"
        private const val NOTIFICATION_ID = 1
        private const val WORK_NAME = "news_update_work"
        
        /**
         * Schedule periodic news updates
         */
        fun schedule(context: Context, intervalMinutes: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<NewsUpdateWorker>(
                intervalMinutes,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
        
        /**
         * Cancel periodic news updates
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
