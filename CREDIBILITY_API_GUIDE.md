# Credibility API Integration Guide

Dieses Dokument beschreibt, wie Sie eine externe Glaubwürdigkeitsprüfungs-API in die NewsAgent-App integrieren können.

## Übersicht

NewsAgent bietet eine flexible Schnittstelle zur Integration von Fact-Checking- und Glaubwürdigkeitsprüfungs-APIs. Die App ist vorkonfiguriert für newscheck123.de (noch nicht online) und kann sowohl mit externen APIs als auch mit heuristischen lokalen Checks arbeiten.

## API-Interface

Die `CredibilityApi`-Schnittstelle definiert den Standardvertrag:

```kotlin
interface CredibilityApi {
    @POST("api/v1/check")
    suspend fun checkCredibility(
        @Body request: CredibilityRequest
    ): Response<CredibilityResponse>
}
```

## Request Format

```json
{
  "title": "Artikel-Titel",
  "content": "Vollständiger Artikeltext",
  "source": "Quellenname",
  "url": "https://example.com/article"
}
```

## Response Format

```json
{
  "score": 0.85,
  "factors": {
    "source_reputation": 0.9,
    "fact_check_status": 0.8,
    "cross_reference": 0.85
  },
  "verified": true,
  "concerns": [],
  "details": "Artikel wurde verifiziert"
}
```

## Empfohlene Fact-Checking APIs

### 1. NewsCheck123 (Geplant)
- **URL**: https://newscheck123.de/
- **Status**: Noch nicht online - wird derzeit entwickelt
- **Features**: Deutsche Nachrichtenprüfung, Fact-Checking
- **Hinweis**: Die App ist bereits für diese API vorkonfiguriert

### 2. ClaimBuster API
- **URL**: https://idir.uta.edu/claimbuster/
- **Features**: Claim-Erkennung, Fact-Checking
- **Preis**: Akademisch kostenlos, kommerzielle Lizenz verfügbar

### 2. Google Fact Check Tools API
- **URL**: https://toolbox.google.com/factcheck/apis
- **Features**: Überprüfung von Behauptungen gegen Fact-Check-Datenbank
- **Preis**: Kostenlos mit Limits

### 3. Full Fact API (UK)
- **URL**: https://fullfact.org/
- **Features**: Automated fact checking
- **Preis**: Auf Anfrage

### 4. Custom API Integration

Für eine eigene API-Implementierung:

```kotlin
// 1. Update die Base URL in SharedPreferences
val prefs = context.getSharedPreferences("newsagent_prefs", Context.MODE_PRIVATE)
prefs.edit().putString("credibility_api_url", "https://newscheck123.de/").apply()

// 2. Stellen Sie sicher, dass Ihre API das erwartete Format zurückgibt
// 3. Optional: Erweitern Sie CredibilityApi für zusätzliche Endpoints
```

## Heuristische Fallback-Prüfung

Wenn keine API verfügbar ist, verwendet NewsAgent folgende Heuristiken:

### 1. Quellen-Reputation
Bekannte vertrauenswürdige deutsche Quellen:
- Öffentlich-rechtliche: Tagesschau, ZDF, ARD
- Etablierte Print: Spiegel, Zeit, FAZ, Süddeutsche
- Nachrichtenagenturen: DPA

### 2. Sensationalismus-Erkennung
Prüfung auf reißerische Wortwahl:
- "Schock", "Unglaublich", "Skandal"
- "Sensation", "Horror", "Wahnsinn"
- Mehrfache Verwendung senkt den Score

### 3. Autoren-Verifizierung
- Artikel mit benanntem Autor: höherer Score
- Anonyme Artikel: neutraler Score

## Integration in eigene Services

### Beispiel: Integration einer Custom API

```kotlin
class CustomCredibilityService(private val context: Context) {
    
    private val api: CustomCredibilityApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://newscheck123.de/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CustomCredibilityApi::class.java)
    }
    
    suspend fun checkArticle(article: NewsArticle): CredibilityScore {
        val response = api.analyze(
            CustomRequest(
                text = article.content ?: "",
                source = article.source,
                url = article.url
            )
        )
        
        return CredibilityScore(
            articleId = article.id,
            score = response.trustScore,
            factors = mapOf(
                "ai_analysis" to response.aiScore,
                "source_check" to response.sourceScore
            ),
            verified = response.verified,
            concerns = response.warnings,
            checkedAt = System.currentTimeMillis()
        )
    }
}
```

## Erweiterte Features

### Batch-Processing
Für effiziente Überprüfung mehrerer Artikel:

```kotlin
interface CredibilityApi {
    @POST("api/v1/check-batch")
    suspend fun checkCredibilityBatch(
        @Body requests: List<CredibilityRequest>
    ): Response<List<CredibilityResponse>>
}
```

### Caching
Implementieren Sie Caching, um API-Aufrufe zu reduzieren:

```kotlin
class CachedCredibilityService(
    private val apiService: CredibilityCheckService,
    private val context: Context
) {
    private val cache = mutableMapOf<String, CredibilityScore>()
    
    suspend fun checkCredibility(article: NewsArticle): CredibilityScore {
        // Check cache first
        cache[article.url]?.let { 
            if (isStillValid(it)) return it 
        }
        
        // Fetch from API
        val score = apiService.checkCredibility(article)
        cache[article.url] = score
        return score
    }
    
    private fun isStillValid(score: CredibilityScore): Boolean {
        val ageHours = (System.currentTimeMillis() - score.checkedAt) / (1000 * 60 * 60)
        return ageHours < 24 // Cache für 24 Stunden
    }
}
```

## Rate Limiting

Implementieren Sie Rate Limiting für API-Aufrufe:

```kotlin
class RateLimitedCredibilityService(
    private val apiService: CredibilityCheckService
) {
    private val rateLimiter = RateLimiter.create(10.0) // 10 requests per second
    
    suspend fun checkCredibility(article: NewsArticle): CredibilityScore {
        rateLimiter.acquire()
        return apiService.checkCredibility(article)
    }
}
```

## Error Handling

Robuste Fehlerbehandlung:

```kotlin
suspend fun checkCredibilitySafe(article: NewsArticle): CredibilityScore {
    return try {
        credibilityService.checkCredibility(article)
    } catch (e: IOException) {
        // Network error - use heuristic fallback
        performBasicCredibilityCheck(article)
    } catch (e: HttpException) {
        when (e.code()) {
            429 -> {
                // Rate limit exceeded - wait and retry
                delay(1000)
                checkCredibilitySafe(article)
            }
            else -> performBasicCredibilityCheck(article)
        }
    }
}
```

## Testing

### Mock API für Tests

```kotlin
class MockCredibilityApi : CredibilityApi {
    override suspend fun checkCredibility(
        request: CredibilityRequest
    ): Response<CredibilityResponse> {
        return Response.success(
            CredibilityResponse(
                score = 0.85f,
                factors = mapOf("test" to 0.85f),
                verified = true,
                concerns = emptyList(),
                details = "Mock response"
            )
        )
    }
}
```

## Konfiguration in der App

1. Öffnen Sie die Einstellungen in der App
2. Scrollen Sie zu "Erweiterte Einstellungen"
3. Geben Sie Ihre Credibility API URL ein
4. Optional: Konfigurieren Sie API-Schlüssel in SharedPreferences

```kotlin
// Programmatisch konfigurieren
val prefs = context.getSharedPreferences("newsagent_prefs", Context.MODE_PRIVATE)
prefs.edit().apply {
    putString("credibility_api_url", "https://newscheck123.de/")
    putString("credibility_api_key", "your-api-key")
    putBoolean("enable_credibility_check", true)
    apply()
}
```

## Best Practices

1. **API-Schlüssel Sicherheit**: Speichern Sie API-Schlüssel niemals im Code
2. **Rate Limiting**: Respektieren Sie API-Limits
3. **Caching**: Cachen Sie Ergebnisse, um API-Aufrufe zu minimieren
4. **Fallback**: Haben Sie immer einen Fallback-Mechanismus
5. **Monitoring**: Überwachen Sie API-Fehlerquoten
6. **Privacy**: Senden Sie nur notwendige Daten an die API

## Weitere Ressourcen

- [Fact-Checking APIs Übersicht](https://www.poynter.org/fact-checking/)
- [IFCN Code of Principles](https://www.poynter.org/ifcn-code-of-principles/)
- [ClaimBuster Documentation](https://idir.uta.edu/claimbuster/api/)
- [Google Fact Check Tools](https://toolbox.google.com/factcheck/explorer)

---

Für weitere Fragen oder Support, erstellen Sie bitte ein Issue auf GitHub.
