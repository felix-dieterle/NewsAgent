package com.newsagent.utils

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * SearchThrottler prevents excessive search queries through debouncing and rate limiting
 * 
 * Features:
 * - Debouncing: Waits for user to stop typing before executing search
 * - Rate limiting: Prevents too many searches in a short time
 * - Duplicate prevention: Skips searches that are already in progress
 */
class SearchThrottler private constructor() {
    
    private data class SearchState(
        var lastSearchTime: Long = 0,
        var pendingJob: Job? = null,
        var inProgressQueries: MutableSet<String> = mutableSetOf()
    )
    
    private val searchStates = ConcurrentHashMap<String, SearchState>()
    
    companion object {
        @Volatile
        private var instance: SearchThrottler? = null
        
        fun getInstance(): SearchThrottler {
            return instance ?: synchronized(this) {
                instance ?: SearchThrottler().also { instance = it }
            }
        }
        
        // Configuration constants
        const val DEFAULT_DEBOUNCE_MS = 500L // Wait 500ms after user stops typing
        const val MIN_SEARCH_INTERVAL_MS = 1000L // Minimum 1 second between searches
        const val MIN_QUERY_LENGTH = 2 // Minimum characters to trigger search
    }
    
    /**
     * Execute a search with debouncing
     * 
     * @param searchId Unique identifier for this search context (e.g., "main_search")
     * @param query The search query
     * @param debounceMs Milliseconds to wait after last keystroke (default: 500ms)
     * @param minInterval Minimum milliseconds between searches (default: 1000ms)
     * @param searchAction The actual search function to execute
     */
    suspend fun executeSearch(
        searchId: String,
        query: String,
        debounceMs: Long = DEFAULT_DEBOUNCE_MS,
        minInterval: Long = MIN_SEARCH_INTERVAL_MS,
        searchAction: suspend (String) -> Unit
    ) {
        // Validate query length
        if (query.trim().length < MIN_QUERY_LENGTH) {
            Logger.d("SearchThrottler", "Query too short, skipping: '$query'")
            return
        }
        
        val state = searchStates.getOrPut(searchId) { SearchState() }
        
        synchronized(state) {
            // Cancel any pending debounced search
            state.pendingJob?.cancel()
            
            // Check if this exact query is already being searched
            if (state.inProgressQueries.contains(query)) {
                Logger.d("SearchThrottler", "Search already in progress for: '$query'")
                return
            }
            
            // Create new debounced job
            state.pendingJob = CoroutineScope(Dispatchers.Main).launch {
                // Debounce: wait for user to stop typing
                delay(debounceMs)
                
                // Check rate limit
                val now = System.currentTimeMillis()
                val timeSinceLastSearch = now - state.lastSearchTime
                if (timeSinceLastSearch < minInterval) {
                    val waitTime = minInterval - timeSinceLastSearch
                    Logger.d("SearchThrottler", "Rate limiting: waiting ${waitTime}ms before search")
                    delay(waitTime)
                }
                
                // Mark query as in progress
                state.inProgressQueries.add(query)
                state.lastSearchTime = System.currentTimeMillis()
                
                try {
                    Logger.i("SearchThrottler", "Executing search for: '$query'")
                    searchAction(query)
                } catch (e: CancellationException) {
                    Logger.d("SearchThrottler", "Search cancelled: '$query'")
                    throw e
                } catch (e: Exception) {
                    Logger.e("SearchThrottler", "Error executing search for '$query'", e)
                } finally {
                    // Remove from in-progress set
                    synchronized(state) {
                        state.inProgressQueries.remove(query)
                    }
                }
            }
        }
    }
    
    /**
     * Cancel any pending searches for a given search context
     */
    fun cancelPendingSearches(searchId: String) {
        searchStates[searchId]?.let { state ->
            synchronized(state) {
                state.pendingJob?.cancel()
                state.pendingJob = null
                Logger.d("SearchThrottler", "Cancelled pending searches for: $searchId")
            }
        }
    }
    
    /**
     * Check if a search can be executed immediately (without debouncing/rate limiting)
     * Useful for user-initiated actions like clicking a search button
     */
    fun canSearchImmediately(searchId: String, query: String): Boolean {
        val state = searchStates[searchId] ?: return true
        
        synchronized(state) {
            // Don't allow if same query is already in progress
            if (state.inProgressQueries.contains(query)) {
                return false
            }
            
            // Allow if enough time has passed since last search
            val timeSinceLastSearch = System.currentTimeMillis() - state.lastSearchTime
            return timeSinceLastSearch >= MIN_SEARCH_INTERVAL_MS
        }
    }
    
    /**
     * Get statistics for monitoring
     */
    fun getStats(searchId: String): SearchStats? {
        val state = searchStates[searchId] ?: return null
        
        synchronized(state) {
            return SearchStats(
                searchId = searchId,
                lastSearchTime = state.lastSearchTime,
                inProgressCount = state.inProgressQueries.size,
                hasPendingSearch = state.pendingJob?.isActive == true
            )
        }
    }
    
    data class SearchStats(
        val searchId: String,
        val lastSearchTime: Long,
        val inProgressCount: Int,
        val hasPendingSearch: Boolean
    )
}
