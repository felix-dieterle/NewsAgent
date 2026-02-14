# RSS Feed Fix - Testing Guide

## Problem
RSS feeds were returning 0 results due to overly strict XML security settings.

## Root Cause
The `RssFeedParser` had the `disallow-doctype-decl` XML security feature enabled, which caused the parser to reject any RSS feed with a DOCTYPE declaration. Since most RSS feeds include DOCTYPE declarations, this resulted in all feeds failing silently.

## Changes Made

### 1. RssFeedParser.kt
- **Removed DOCTYPE restriction**: Commented out `disallow-doctype-decl` feature while maintaining XXE protection
- **Added namespace awareness**: Enabled `isNamespaceAware` for better RDF/RSS 1.0 support
- **Enhanced logging**: Added debug logs to track:
  - Root element name
  - Number of items found
  - Successful parsing count
  - Detailed error messages
- **Added fallback date parsing**: Support both `pubDate` (RSS 2.0) and `dc:date` (RSS 1.0/RDF)

### 2. NewsRepository.kt
- **Increased timeout**: Changed from 10s to 15s for slow feeds
- **Added User-Agent header**: Some servers block requests without proper User-Agent
- **Enabled redirects**: Added `followRedirects(true)` and `followSslRedirects(true)`
- **Enhanced error reporting**:
  - Check for empty response bodies
  - Log XML content length
  - Show XML preview when parsing fails
  - Collect and display all failure details
  - Count success/failure per feed

## Testing Instructions

### 1. Enable Debug Logging
In Android Studio, set up logcat filter:
```
Tag: NewsRepository|RssFeedParser
```

### 2. Test RSS Feed Loading
1. Open the NewsAgent app
2. Navigate to menu → "RSS Nachrichten"
3. Observe the logs in logcat

### Expected Log Output (Success Case)
```
I/NewsRepository: 📰 Querying RSS feeds (100% kostenlos): Tagesschau, Heise Online, Spiegel Online
D/NewsRepository: Fetching from RSS source: Tagesschau (https://www.tagesschau.de/xml/rss2/)
D/RssFeedParser: Parsing Tagesschau: Root element = rss
D/RssFeedParser: Found 15 items in Tagesschau
D/RssFeedParser: Successfully parsed 15 articles from Tagesschau
I/NewsRepository: ✅ Tagesschau: 15 articles fetched successfully
D/NewsRepository: Fetching from RSS source: Heise Online (https://www.heise.de/rss/heise.rdf)
D/RssFeedParser: Parsing Heise Online: Root element = rdf:RDF
D/RssFeedParser: Found 20 items in Heise Online
D/RssFeedParser: Successfully parsed 20 articles from Heise Online
I/NewsRepository: ✅ Heise Online: 20 articles fetched successfully
D/NewsRepository: Fetching from RSS source: Spiegel Online (https://www.spiegel.de/schlagzeilen/index.rss)
D/RssFeedParser: Parsing Spiegel Online: Root element = rss
D/RssFeedParser: Found 10 items in Spiegel Online
D/RssFeedParser: Successfully parsed 10 articles from Spiegel Online
I/NewsRepository: ✅ Spiegel Online: 10 articles fetched successfully
I/NewsRepository: RSS fetch complete: 3 succeeded, 0 failed (Total articles: 45)
I/NewsRepository: Returning 10 RSS articles (limited from 45 total)
```

### Expected Log Output (Failure Case - Network Issue)
```
I/NewsRepository: 📰 Querying RSS feeds (100% kostenlos): Tagesschau, Heise Online, Spiegel Online
D/NewsRepository: Fetching from RSS source: Tagesschau (https://www.tagesschau.de/xml/rss2/)
W/NewsRepository: ⚠️ Tagesschau: HTTP 403 - Forbidden
D/NewsRepository: Fetching from RSS source: Heise Online (https://www.heise.de/rss/heise.rdf)
W/NewsRepository: ⚠️ Heise Online: SocketTimeoutException - timeout
I/NewsRepository: RSS fetch complete: 0 succeeded, 3 failed (Total articles: 0)
W/NewsRepository: ⚠️ No RSS articles fetched - alle 3 Quellen haben fehlgeschlagen
W/NewsRepository: Fehlerdetails:
W/NewsRepository:   - Tagesschau: HTTP 403 - Forbidden
W/NewsRepository:   - Heise Online: SocketTimeoutException - timeout
W/NewsRepository:   - Spiegel Online: UnknownHostException - Unable to resolve host
I/NewsRepository: ➡️ Überprüfen Sie Ihre Internetverbindung und Firewall-Einstellungen
```

### 3. Test RSS Search
1. Use the search function in the app
2. Enter a query (e.g., "Deutschland", "Politik", "Technologie")
3. Observe that results are returned when articles match

### 4. Verify Each Feed Individually
The logs will show which feeds succeed/fail:
- ✅ = Success with article count
- ⚠️ = Partial failure (HTTP error)
- ❌ = Complete failure (Exception)

## Troubleshooting

### If all feeds still return 0 results:
1. **Check Internet Connection**: Ensure the device has internet access
2. **Check Firewall**: Some corporate/school networks block RSS feeds
3. **Check Logs**: Look for specific error messages in logcat
4. **Try Individual Feeds**: Use curl/browser to test feed URLs directly:
   ```bash
   curl -v https://www.tagesschau.de/xml/rss2/
   curl -v https://www.heise.de/rss/heise.rdf
   curl -v https://www.spiegel.de/schlagzeilen/index.rss
   ```

### If only some feeds fail:
- This is expected - not all feeds may be accessible from all networks
- The app will return results from successful feeds
- Check the detailed error logs to see which specific feeds failed and why

### If parsing fails:
Look for logs showing:
- "Empty response body" - The server returned nothing
- "No articles parsed from X bytes of XML" - XML received but couldn't parse
- "XML preview: ..." - Shows first 500 chars of received XML for debugging

## Security Note
The changes maintain security against XXE (XML External Entity) attacks by:
- Disabling external general entities
- Disabling external parameter entities  
- Disabling external DTD loading
- Disabling XInclude
- Disabling entity expansion

We only removed the `disallow-doctype-decl` restriction because:
1. RSS feeds legitimately use DOCTYPE declarations
2. DOCTYPE alone is not a security risk (external entities are)
3. We're fetching from known, trusted RSS feed URLs
4. All other XXE protections remain in place

## Next Steps
1. Test with actual device/emulator
2. Monitor logs for 24 hours to see success rates
3. If issues persist, check specific feed URLs
4. Consider adding retry logic or fallback feeds if needed
