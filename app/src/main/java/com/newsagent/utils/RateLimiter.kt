package com.newsagent.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Rate limiter to prevent exceeding API rate limits
 * Protects against accidental overuse of paid/limited APIs
 */
class RateLimiter private constructor() {
    
    private data class RateLimit(
        val maxRequests: Int,
        val windowMillis: Long,
        val requests: MutableList<Long> = mutableListOf()
    )
    
    private val limits = ConcurrentHashMap<String, RateLimit>()
    
    companion object {
        @Volatile
        private var instance: RateLimiter? = null
        
        fun getInstance(): RateLimiter {
            return instance ?: synchronized(this) {
                instance ?: RateLimiter().also { instance = it }
            }
        }
        
        // Default rate limits for different services
        const val NEWS_API_DAILY_LIMIT = 95 // Leave 5 requests buffer
        const val GNEWS_API_DAILY_LIMIT = 95
        const val OPENROUTER_HOURLY_LIMIT = 50 // Conservative limit for free tier
        const val GOOGLE_CUSTOM_SEARCH_DAILY_LIMIT = 95 // 100 queries per day, keep 5 buffer
    }
    
    init {
        // Configure default rate limits
        setLimit("news_api", NEWS_API_DAILY_LIMIT, TimeUnit.DAYS.toMillis(1))
        setLimit("gnews_api", GNEWS_API_DAILY_LIMIT, TimeUnit.DAYS.toMillis(1))
        setLimit("openrouter_api", OPENROUTER_HOURLY_LIMIT, TimeUnit.HOURS.toMillis(1))
        setLimit("google_custom_search", GOOGLE_CUSTOM_SEARCH_DAILY_LIMIT, TimeUnit.DAYS.toMillis(1))
    }
    
    /**
     * Set a rate limit for a specific service
     */
    fun setLimit(serviceId: String, maxRequests: Int, windowMillis: Long) {
        limits[serviceId] = RateLimit(maxRequests, windowMillis)
    }
    
    /**
     * Check if a request is allowed under the rate limit
     * @return true if request is allowed, false if rate limit would be exceeded
     */
    fun allowRequest(serviceId: String): Boolean {
        val limit = limits[serviceId] ?: return true // No limit configured, allow
        
        synchronized(limit) {
            val now = System.currentTimeMillis()
            val windowStart = now - limit.windowMillis
            
            // Remove requests outside the current window
            limit.requests.removeAll { it < windowStart }
            
            // Check if we can make another request
            if (limit.requests.size >= limit.maxRequests) {
                return false
            }
            
            // Record this request
            limit.requests.add(now)
            return true
        }
    }
    
    /**
     * Record a request for rate limiting
     * Use this when you want to manually track requests
     */
    fun recordRequest(serviceId: String) {
        val limit = limits[serviceId] ?: return
        
        synchronized(limit) {
            val now = System.currentTimeMillis()
            limit.requests.add(now)
        }
    }
    
    /**
     * Get remaining requests in the current window
     */
    fun getRemainingRequests(serviceId: String): Int {
        val limit = limits[serviceId] ?: return Int.MAX_VALUE
        
        synchronized(limit) {
            val now = System.currentTimeMillis()
            val windowStart = now - limit.windowMillis
            
            // Remove requests outside the current window
            limit.requests.removeAll { it < windowStart }
            
            return maxOf(0, limit.maxRequests - limit.requests.size)
        }
    }
    
    /**
     * Get time until next request is allowed (in milliseconds)
     * Returns 0 if requests are currently allowed
     */
    fun getTimeUntilNextRequest(serviceId: String): Long {
        val limit = limits[serviceId] ?: return 0
        
        synchronized(limit) {
            if (limit.requests.size < limit.maxRequests) {
                return 0 // Requests are allowed now
            }
            
            // Find the oldest request in the window
            val oldestRequest = limit.requests.minOrNull() ?: return 0
            val now = System.currentTimeMillis()
            
            // Time until the oldest request expires from the window
            return maxOf(0, (oldestRequest + limit.windowMillis) - now)
        }
    }
    
    /**
     * Reset rate limits for a service (useful for testing or manual reset)
     */
    fun reset(serviceId: String) {
        limits[serviceId]?.let { limit ->
            synchronized(limit) {
                limit.requests.clear()
            }
        }
    }
    
    /**
     * Get rate limit statistics
     */
    fun getStats(serviceId: String): RateLimitStats? {
        val limit = limits[serviceId] ?: return null
        
        synchronized(limit) {
            val now = System.currentTimeMillis()
            val windowStart = now - limit.windowMillis
            limit.requests.removeAll { it < windowStart }
            
            return RateLimitStats(
                serviceId = serviceId,
                maxRequests = limit.maxRequests,
                currentRequests = limit.requests.size,
                remainingRequests = maxOf(0, limit.maxRequests - limit.requests.size),
                windowMillis = limit.windowMillis,
                timeUntilReset = if (limit.requests.isNotEmpty()) {
                    maxOf(0, (limit.requests.minOrNull()!! + limit.windowMillis) - now)
                } else 0
            )
        }
    }
    
    data class RateLimitStats(
        val serviceId: String,
        val maxRequests: Int,
        val currentRequests: Int,
        val remainingRequests: Int,
        val windowMillis: Long,
        val timeUntilReset: Long
    )
}
