# Architecture Documentation

## NewsAgent Android App - Technical Overview

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  MainActivity│  │NewsDetailAct │  │SettingsAct   │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                 │                  │              │
└─────────┼─────────────────┼──────────────────┼──────────────┘
          │                 │                  │
┌─────────┼─────────────────┼──────────────────┼──────────────┐
│         │    Service Layer│                  │              │
│  ┌──────▼────────┐ ┌─────▼──────┐  ┌────────▼─────┐       │
│  │NewsRepository │ │AiSummary   │  │Credibility   │       │
│  │               │ │Service     │  │CheckService  │       │
│  └──────┬────────┘ └─────┬──────┘  └────────┬─────┘       │
│         │                │                   │              │
│  ┌──────▼────────────────▼───────────────────▼─────┐       │
│  │          NewsUpdateWorker (Background)          │       │
│  └──────┬──────────────────────────────────────────┘       │
│         │                                                   │
└─────────┼───────────────────────────────────────────────────┘
          │
┌─────────┼───────────────────────────────────────────────────┐
│         │         API Layer                                 │
│  ┌──────▼──────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  NewsApi    │  │OpenRouterApi │  │CredibilityApi│      │
│  └─────────────┘  └──────────────┘  └──────────────┘      │
│         │                │                  │               │
└─────────┼────────────────┼──────────────────┼───────────────┘
          │                │                  │
          ▼                ▼                  ▼
    [NewsAPI.org]   [OpenRouter.ai]   [Custom API]
```

### Data Flow

#### 1. News Fetching Flow
```
User Action / Periodic Update
        │
        ▼
   MainActivity / NewsUpdateWorker
        │
        ▼
   NewsRepository.fetchTopHeadlines()
        │
        ▼
   NewsApi.getTopHeadlines()
        │
        ▼
   Parse Response → NewsArticle objects
        │
        ▼
   Display in RecyclerView / Send Notification
```

#### 2. Summary Generation Flow
```
NewsArticle loaded
        │
        ▼
   AiSummaryService.generateSummary()
        │
        ▼
   Build prompt for article
        │
        ▼
   OpenRouterApi.generateCompletion()
        │
        ▼
   Parse AI response
        │
        ▼
   Create NewsSummary object
        │
        ▼
   Attach to NewsArticle
```

#### 3. Credibility Check Flow
```
NewsArticle loaded
        │
        ▼
   CredibilityCheckService.checkCredibility()
        │
        ├─── API Available? ───┐
        │                      │
        ▼                      ▼
   CredibilityApi.check()   Heuristic Check
        │                      │
        ├──────────────────────┘
        │
        ▼
   Create CredibilityScore
        │
        ▼
   Attach to NewsArticle
```

#### 5. Caching Flow
```
User Request
     │
     ▼
Check CacheManager
     │
     ├─── Cache Hit ────┐
     │                  │
     ▼                  │
API Call              │
     │                  │
     ▼                  │
Store in Cache        │
     │                  │
     └──────────────────┘
     │
     ▼
Return Cached Data
```

### Components Description

#### Models
- **NewsArticle**: Core data model representing a news article
  - Contains article metadata (title, content, source, etc.)
  - References to summary and credibility score
  
- **NewsSummary**: AI-generated summary
  - Summary text
  - Key points list
  - Audio URL (optional)
  - Generation metadata

- **CredibilityScore**: Credibility assessment
  - Overall score (0.0-1.0)
  - Factor breakdown
  - Verification status
  - Concerns list

#### Services

**NewsRepository**
- Fetches news from News API, GNews API, and RSS feeds
- Converts API responses to domain models
- Implements HTTP response caching (10 MB)
- Application-level caching for articles (15 min TTL)
- Rate limiting protection for API calls
- Manages API configuration (keys, preferences)

**AiSummaryService**
- Integrates with OpenRouter API
- Generates German summaries
- Extracts key points from articles
- Uses free/cheap AI models (Gemini Flash)
- Aggressive caching (24h TTL) to minimize costs
- Rate limiting to prevent quota exhaustion

**CredibilityCheckService**
- Primary: API-based credibility checking
- Fallback: Heuristic analysis
  - Source reputation scoring
  - Sensationalism detection
  - Author verification
- Caching of credibility scores (24h TTL)
- Rate limiting for API calls

**TextToSpeechService**
- Android TTS integration
- Speaks summaries in German
- Lifecycle management

**NewsUpdateWorker**
- WorkManager-based background job
- Periodic news fetching
- Notification generation
- Respects network constraints
- Uses cached data when available

#### Utilities

**CacheManager**
- Singleton in-memory cache
- Supports articles, summaries, and credibility scores
- TTL-based expiration
- Thread-safe concurrent operations
- Cache statistics and monitoring

**RateLimiter**
- Prevents API quota exhaustion
- Configurable limits per service
- Sliding window algorithm
- Statistics and remaining quota tracking
- Automatic request tracking

#### UI Components

**MainActivity**
- Lists news articles in RecyclerView
- Manual refresh capability
- Navigation to settings and details

**NewsDetailActivity**
- Shows full article details
- Displays AI summary and key points
- Credibility score visualization
- Audio playback button

**SettingsActivity**
- API key configuration
- Update interval settings
- Feature toggles
- Saves to SharedPreferences

### Configuration Management

**SharedPreferences Keys:**
```kotlin
news_api_key: String              // News API authentication
openrouter_api_key: String        // OpenRouter authentication
credibility_api_url: String       // Optional credibility API
update_interval_minutes: Int      // Background update frequency
enable_notifications: Boolean     // Notification toggle
enable_auto_summary: Boolean      // Auto-generate summaries
enable_credibility_check: Boolean // Auto-check credibility
max_articles: Int                 // Limit per update
country: String                   // News country filter
language: String                  // News language filter
```

### External APIs

#### 1. News API (newsapi.org)
- **Purpose**: News aggregation
- **Endpoints**: 
  - `/v2/top-headlines` - Get top news
  - `/v2/everything` - Search news
- **Rate Limits**: 100 requests/day (free tier)
- **Authentication**: API key required

#### 2. GNews API (gnews.io) - Free Search
- **Purpose**: Free news search with simple registration
- **Endpoints**:
  - `/api/v4/search` - Search news articles
  - `/api/v4/top-headlines` - Get top headlines
- **Rate Limits**: 100 requests/day (free tier)
- **Authentication**: API token (free tier available)
- **Features**:
  - Search by keyword
  - Filter by language and country
  - Simple registration process

#### 3. RSS Feeds - Completely Free
- **Purpose**: 100% free news without any registration
- **Sources**:
  - Tagesschau (https://www.tagesschau.de/xml/rss2/)
  - Heise Online
  - Spiegel Online
  - Zeit Online
- **Rate Limits**: None (public RSS feeds)
- **Authentication**: None required
- **Features**:
  - Real-time German news
  - No registration needed
  - Keyword search within feeds

#### 4. OpenRouter (openrouter.ai)
- **Purpose**: AI text generation
- **Endpoint**: `/api/v1/chat/completions`
- **Models Used**: 
  - `google/gemini-flash-1.5` (free)
  - Others configurable
- **Authentication**: Bearer token

#### 5. Credibility API (Custom)
- **Purpose**: Fact-checking and verification
- **Endpoint**: `/api/v1/check`
- **Fallback**: Heuristic checking if unavailable

### Security Considerations

1. **API Keys**: Stored in SharedPreferences (device-encrypted)
2. **Network**: HTTPS only for all API calls
3. **Permissions**: Minimal required permissions
4. **ProGuard**: Protects model classes in release builds

### Scalability

**Current Optimizations:**
- HTTP response caching for network efficiency
- Application-level caching with TTL management
- Parallel article processing with coroutines
- Rate limiting to prevent quota exhaustion
- Smart cache keys based on stable identifiers

**Current Limitations:**
- In-memory storage only (volatile)
- No offline support beyond HTTP cache
- Limited to API free tiers (100 news/day, 50 AI/hour)

**Future Improvements:**
- Room database for persistent offline storage
- Advanced caching policies (LRU, size-based eviction)
- Multiple news sources with intelligent fallback
- Advanced rate limiting with exponential backoff
- Network request batching
- Background sync optimization with WorkManager constraints

### Testing Strategy

**Unit Tests:**
- Model parsing
- Heuristic credibility checks
- Summary parsing logic

**Integration Tests:**
- API integrations
- Service layer

**UI Tests:**
- Navigation flows
- Settings persistence
- List display

### Build Configuration

**Gradle Modules:**
- App module (main application)

**Dependencies:**
- AndroidX libraries
- Retrofit + OkHttp
- Kotlin Coroutines
- WorkManager
- Material Components

**Build Types:**
- Debug (logging enabled)
- Release (ProGuard, optimized)

### Performance Considerations

1. **Network**: All API calls on IO dispatcher
2. **UI**: RecyclerView for efficient list rendering
3. **Background**: WorkManager respects battery optimization
4. **Memory**: In-memory caching with TTL, minimal RAM footprint
5. **Caching**: Multi-level caching strategy
   - HTTP response caching (10 MB OkHttp cache)
   - Application-level caching (articles, summaries, credibility)
   - Cache TTLs: Articles (15 min), Summaries (24h), Credibility (24h)
6. **Parallel Processing**: Coroutines for concurrent article processing
7. **Rate Limiting**: Protects against API quota exhaustion
   - News API: 95 requests/day
   - GNews API: 95 requests/day
   - OpenRouter: 50 requests/hour
8. **Logging**: BASIC level in production (not BODY) to reduce overhead

### Accessibility

- Text-to-Speech for audio summaries
- Material Design for standard accessibility
- Readable font sizes
- High contrast credibility indicators

---

Last Updated: January 2026
