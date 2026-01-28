# GitHub Copilot Instructions for NewsAgent

## Project Overview
NewsAgent is an Android news aggregation app with AI-powered summaries and credibility checking. The app focuses on cost-efficiency, performance, and minimizing redundant API calls.

## Architecture Patterns

### 1. Repository Pattern
- All data fetching goes through `NewsRepository`
- Services layer handles business logic (AI summaries, credibility checks)
- UI layer only handles presentation

### 2. Caching Strategy
- **ALWAYS** check cache before making API calls
- Use `CacheManager.getInstance()` for all caching operations
- Cache keys should be based on stable identifiers (URL hash, query params)
- Cache TTLs:
  - Articles: 15 minutes
  - Summaries: 24 hours
  - Credibility: 24 hours

### 3. Cost-Efficiency Goals
- Minimize OpenRouter API calls (AI summaries are expensive)
- Cache summaries aggressively - article content rarely changes
- Use HTTP response caching for network requests
- Avoid redundant credibility checks for the same article

## Code Style Guidelines

### Performance Best Practices
1. **Parallel Processing**: Use coroutines for parallel operations
   ```kotlin
   // Good - parallel processing
   articles.map { article ->
       async {
           article.summary = generateSummary(article)
       }
   }.forEach { it.await() }
   
   // Bad - sequential processing
   for (article in articles) {
       article.summary = generateSummary(article)
   }
   ```

2. **Cache First**: Always check cache before API calls
   ```kotlin
   // Good - check cache first
   cacheManager.getCachedSummary(key)?.let { return it }
   val result = expensiveApiCall()
   cacheManager.cacheSummary(key, result)
   
   // Bad - no caching
   val result = expensiveApiCall()
   ```

3. **Efficient Logging**: Use appropriate log levels
   ```kotlin
   // Good - minimal production logging
   Logger.d("Tag", "Debug info") // Only in debug
   Logger.i("Tag", "Important info")
   Logger.e("Tag", "Error", exception)
   
   // Bad - verbose production logging
   HttpLoggingInterceptor.Level.BODY // Too verbose for production
   ```

4. **HTTP Caching**: Configure OkHttp cache for network responses
   ```kotlin
   val cache = Cache(
       directory = File(context.cacheDir, "http_cache"),
       maxSize = 10L * 1024L * 1024L // 10 MB
   )
   OkHttpClient.Builder().cache(cache).build()
   ```

### Kotlin Conventions
- Use data classes for models
- Prefer `suspend fun` for async operations
- Use `withContext(Dispatchers.IO)` for IO operations
- Use coroutines instead of callbacks
- Leverage lazy initialization for expensive objects

### Error Handling
```kotlin
try {
    val result = apiCall()
    Logger.i("Tag", "Success")
    result
} catch (e: Exception) {
    Logger.e("Tag", "Operation failed", e)
    fallbackValue
}
```

### Resource Management
- Close resources properly (TTS, HTTP clients)
- Use `lazy` for singletons
- Implement proper lifecycle management in Activities/Services

## API Integration Guidelines

### 1. News APIs
- NewsAPI.org: 100 requests/day limit
- GNews API: 100 requests/day limit
- RSS Feeds: No limits, completely free
- **Prefer RSS feeds** when API limits are a concern

### 2. OpenRouter (AI Summaries)
- **Most expensive service** - cache aggressively
- Use `google/gemini-flash-1.5` (free tier model)
- Set reasonable timeouts (60s connect, 60s read)
- Hash article URL for stable cache keys

### 3. Credibility API
- Fallback to heuristic checks if API unavailable
- Cache credibility scores for 24 hours
- Article credibility rarely changes

## Testing Strategy

### When Adding New Features
1. Add unit tests for business logic
2. Test caching behavior
3. Test error handling and fallbacks
4. Verify no regression in existing features

### Performance Testing
1. Monitor cache hit rates
2. Track API call frequency
3. Measure response times
4. Check memory usage

## Common Patterns

### Adding a New API Endpoint
```kotlin
// 1. Define in API interface
@GET("endpoint")
suspend fun getData(): Response<DataModel>

// 2. Add to repository with caching
suspend fun fetchData(): DataModel = withContext(Dispatchers.IO) {
    val cacheKey = "data_key"
    cacheManager.getCachedData(cacheKey)?.let { return@withContext it }
    
    val response = api.getData()
    if (response.isSuccessful) {
        val data = response.body()!!
        cacheManager.cacheData(cacheKey, data)
        data
    } else {
        emptyData
    }
}
```

### Adding a New Service
```kotlin
class NewService(private val context: Context) {
    private val prefs = context.getSharedPreferences("newsagent_prefs", Context.MODE_PRIVATE)
    private val cacheManager = CacheManager.getInstance()
    
    private val api: NewApi by lazy {
        // Configure with timeouts and basic logging
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(client)
            .build()
            .create(NewApi::class.java)
    }
}
```

## Optimization Checklist

When reviewing or writing code, ensure:
- [ ] Cache is checked before API calls
- [ ] Expensive operations use parallel processing
- [ ] HTTP cache is configured for network clients
- [ ] Timeouts are set appropriately
- [ ] Logging level is BASIC or lower (not BODY)
- [ ] Error handling includes fallbacks
- [ ] Resources are properly closed
- [ ] Coroutines use appropriate dispatchers
- [ ] No redundant API calls in loops
- [ ] Cache keys are stable and predictable

## Anti-Patterns to Avoid

### ❌ Don't Do This
```kotlin
// Sequential processing - slow
for (article in articles) {
    article.summary = generateSummary(article) // API call
}

// No caching - expensive
fun getSummary(article: NewsArticle) = generateSummary(article)

// Verbose logging in production
HttpLoggingInterceptor.Level.BODY

// Missing error handling
val result = apiCall() // Could crash
```

### ✅ Do This Instead
```kotlin
// Parallel processing - fast
articles.map { article ->
    async { generateSummary(article) }
}.forEach { it.await() }

// With caching - efficient
fun getSummary(article: NewsArticle): Summary? {
    return cache.get(article.id) ?: generateSummary(article)?.also {
        cache.put(article.id, it)
    }
}

// Minimal logging
HttpLoggingInterceptor.Level.BASIC

// With error handling
try {
    val result = apiCall()
} catch (e: Exception) {
    Logger.e("Tag", "Failed", e)
    fallback
}
```

## Performance Goals
- API calls: < 100/day per service (stay within free tiers)
- Cache hit rate: > 70% for summaries and credibility
- Article processing: < 5 seconds for 10 articles
- Memory usage: < 50 MB for typical operation

## Security Considerations
- Never log API keys or sensitive data
- Use HTTPS for all network calls
- Store API keys in SharedPreferences (encrypted by Android)
- Validate all user inputs
- Use ProGuard in release builds

## Future Optimization Opportunities
1. Room database for persistent offline storage
2. WorkManager for background sync optimization
3. Image caching with Glide/Coil
4. Network request batching
5. Compression for API payloads
6. Analytics for cache performance monitoring

---

**Remember**: The primary goal is cost-efficiency. Always prefer caching over API calls, and RSS feeds over paid APIs when possible.
