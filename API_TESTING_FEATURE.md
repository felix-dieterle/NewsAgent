# API Testing Feature

## Overview

A new API testing feature has been added to NewsAgent to help users validate their API configurations. This addresses the common question about Google Custom Search API requiring two parameters and provides a way to test all configured APIs.

## What's New

### 1. "Test All APIs" Button

A new button **"Alle APIs testen"** (Test All APIs) has been added to the Settings screen under the Troubleshooting section. This button tests all configured APIs and provides detailed feedback.

**Location**: Settings > Troubleshooting > "Alle APIs testen"

### 2. API Tests Performed

The test validates the following APIs:

#### NewsAPI.org
- Tests if the API key is valid
- Attempts to fetch articles
- Reports success with article count or error message

#### GNews.io
- Tests if the API key is valid
- Attempts to fetch articles
- Reports success with article count or error message

#### OpenRouter AI
- Tests if the AI API key is valid
- Sends a test request with a sample article
- Verifies the AI responds with a summary

#### Google Custom Search
- Tests if BOTH API key AND Search Engine ID are configured
- Verifies both parameters work together
- Reports success with article count or error message

### 3. Test Results

The test provides clear status indicators:
- ✅ **Erfolgreich** - API is working correctly
- ❌ **Fehler** - API test failed (check credentials or logs)
- ⚠️ **Warnung** - Partial configuration or no results found
- ⚪ **Nicht konfiguriert** - API key not configured

## Google Custom Search API Clarification

### The Two-Parameter Question

Users have asked why Google Custom Search requires TWO fields when other services like SerpApi only need one API key.

**Answer**: NewsAgent uses **Google Custom Search API** (Google's official API), which is designed to require:
1. **API Key** - From Google Cloud Console (for authentication)
2. **Search Engine ID (cx)** - From Programmable Search Engine (defines what to search)

### Why Both Are Needed

The Search Engine ID is not just an identifier - it defines:
- What websites to search
- Search engine behavior and settings
- Image search settings
- Safe search configuration

Google's design separates authentication (API key) from configuration (Search Engine ID).

### Alternative: SerpApi

**SerpApi** is a third-party service that provides a simplified interface:
- Only ONE API key needed
- Different pricing structure
- Not currently integrated in NewsAgent

To use SerpApi would require code changes. The current implementation uses Google's official API.

## UI Changes

### Settings Screen

#### Before:
```
Google Custom Search Engine ID
[Input field]
Programmable Search Engine ID - https://programmablesearchengine.google.com/
```

#### After:
```
Google Custom Search Engine ID (cx)
[Input field]
Programmable Search Engine ID - https://programmablesearchengine.google.com/

Hinweis: Google Custom Search API benötigt BEIDE Felder (API Key + Engine ID). 
Alternativ können Sie SerpApi verwenden, das nur einen Key benötigt.
```

### Troubleshooting Section

A new button has been added:
```
[Alle APIs testen]  ← NEW!
[Quelle testen (alle Artikel anzeigen)]
```

## Documentation Updates

### GOOGLE_CUSTOM_SEARCH_SETUP.md

Added new sections:
1. **Google Custom Search API vs SerpApi** - Explains the difference
2. **Why Two Parameters?** - Clarifies the design requirement
3. **Alternative: SerpApi** - Mentions the simpler alternative
4. **Test Your Configuration** - How to use the new testing feature

## Technical Implementation

### New Functions in SettingsActivity.kt

```kotlin
fun testAllApis() 
    // Main coordinator function that tests all APIs

suspend fun testNewsApi(apiKey: String): String
    // Tests NewsAPI.org connectivity

suspend fun testGNewsApi(apiKey: String): String
    // Tests GNews.io connectivity

suspend fun testOpenRouterApi(apiKey: String): String
    // Tests OpenRouter AI API connectivity

suspend fun testGoogleCustomSearchApi(apiKey: String, searchEngineId: String): String
    // Tests Google Custom Search with both required parameters
```

### Testing Strategy

All tests:
1. Temporarily save the test API key
2. Make a real API call
3. Restore the original configuration
4. Return status message with results
5. Log errors for troubleshooting

This ensures:
- No permanent changes to user settings
- Real validation of API credentials
- Helpful error messages
- Full logging for support

## Usage Instructions

### For Users

1. Go to **Settings**
2. Scroll to **Troubleshooting** section
3. Click **"Alle APIs testen"**
4. Wait for the test to complete (may take 10-30 seconds)
5. Review the results in the dialog
6. If any tests fail, click "Logs anzeigen" to see details
7. Fix any configuration issues
8. Re-test to verify

### For Google Custom Search

If Google Custom Search test fails:
1. Verify BOTH fields are filled in
2. Check for extra spaces in the values
3. Ensure Custom Search API is enabled in Google Cloud Console
4. Verify Search Engine ID is from Programmable Search Engine
5. Check that your Search Engine is configured to search the entire web

## Benefits

1. **Self-Service Validation** - Users can verify their setup without trial and error
2. **Clear Error Messages** - Specific feedback about what's wrong
3. **Time Savings** - Quick validation of all APIs at once
4. **Better Support** - Easier to diagnose configuration issues
5. **Educational** - Helps users understand the requirements

## Future Enhancements

Potential improvements:
1. Add Credibility API testing
2. Show rate limit status during tests
3. Export test results
4. Add SerpApi integration as an alternative
5. Test additional API features (not just connectivity)
