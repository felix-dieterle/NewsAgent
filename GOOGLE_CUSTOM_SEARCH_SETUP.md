# Google Custom Search API Setup Guide

This guide explains how to set up and configure Google Custom Search API as a fallback search option in NewsAgent.

## Overview

Google Custom Search API has been integrated as a fallback search mechanism in the intelligent search strategy. It provides:
- **100 free queries per day**
- High-quality search results from Google
- Automatic fallback when other news APIs are unavailable or rate-limited

## Setup Steps

### 1. Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable billing (required for API access, but free tier is available)

### 2. Enable Custom Search API

1. Navigate to **APIs & Services** > **Library**
2. Search for "Custom Search API"
3. Click **Enable**

### 3. Create API Credentials

1. Go to **APIs & Services** > **Credentials**
2. Click **Create Credentials** > **API Key**
3. Copy the generated API key
4. (Optional but recommended) Restrict the key:
   - Click on the key to edit
   - Under "API restrictions", select "Restrict key"
   - Choose "Custom Search API"
   - Save

### 4. Create a Custom Search Engine

1. Go to [Programmable Search Engine](https://programmablesearchengine.google.com/)
2. Click **Get Started** or **Add**
3. Configure your search engine:
   - **Search engine name**: NewsAgent Search (or any name you prefer)
   - **What to search**: Select "Search the entire web"
   - **Search settings**: Enable "Image search" and "Safe search" as needed
4. Click **Create**
5. Copy the **Search engine ID** (cx parameter) from the overview page

### 5. Configure NewsAgent

1. Open NewsAgent app
2. Go to **Settings**
3. Scroll to the Google Custom Search section
4. Enter your credentials:
   - **Google Custom Search API Key**: Paste the API key from step 3
   - **Google Custom Search Engine ID**: Paste the search engine ID from step 4
5. Click **Save Settings**

## How It Works

### Search Strategy

The intelligent search selector uses Google Custom Search as a fallback with the following priority:

1. **RSS Feeds** (if `preferFree` is true) - Free, unlimited
2. **GNews API** (if configured and rate limit available)
3. **NewsAPI** (if configured and rate limit available)
4. **Google Custom Search** (if configured and rate limit available) ← **NEW**
5. **RSS Feeds** (final fallback)

### Rate Limiting

- **Daily Limit**: 100 queries per day
- **Buffer**: 5 requests kept in reserve
- **Effective Limit**: 95 queries per day
- **Window**: 24 hours (rolling)
- **Monitoring**: Real-time usage displayed in Settings > API Rate Limits

### Caching

Search results are cached for 15 minutes to minimize API calls:
- Cache key format: `google_search_{query}_{maxArticles}`
- Reduces redundant queries for the same search term
- Helps stay within the free tier limit

## API Response Mapping

Google Custom Search results are converted to `NewsArticle` objects:

| Google Search Field | NewsArticle Field | Notes |
|---------------------|-------------------|-------|
| `title` | `title` | Direct mapping |
| `snippet` | `description` | Summary of the page |
| `link` | `url` | Direct mapping |
| `pagemap.cse_image[0].src` | `imageUrl` | First image if available |
| `pagemap.metatags.article:published_time` | `publishedAt` | Extracted from metadata |
| `pagemap.metatags.og:site_name` | `source` | Site name or domain |

## Cost Considerations

### Free Tier
- 100 queries per day at no cost
- Perfect for personal use or testing
- No credit card required for free tier

### Paid Tier
- If you exceed 100 queries/day, additional queries cost $5 per 1,000 queries
- Billing must be enabled on your Google Cloud project
- NewsAgent's rate limiter prevents accidental overuse

### Optimization Tips
1. **Use AI Mode sparingly**: When disabled, NewsAgent prefers free RSS feeds
2. **Monitor usage**: Check Settings > API Rate Limits regularly
3. **Leverage caching**: Repeated searches use cached results
4. **Combine with other sources**: Configure multiple APIs for redundancy

## Troubleshooting

### "Google Custom Search API not fully configured"
- Ensure both API key and Search Engine ID are entered in Settings
- Check that the values don't have extra spaces

### "Google Custom Search rate limit reached"
- You've used 95+ queries today
- Wait 24 hours for the limit to reset
- Check Settings > API Rate Limits for reset time

### "Google Custom Search failed"
- Verify API key is valid and not restricted
- Ensure Custom Search API is enabled in Google Cloud Console
- Check that Search Engine ID is correct
- Review app logs in Settings > Troubleshooting

### No Results Returned
- Google Custom Search may not find news-specific results
- Try a different search query
- Verify your Custom Search Engine is configured to search the entire web
- The app will automatically fall back to RSS feeds if no results are found

## Privacy & Security

- API keys are stored securely in Android SharedPreferences (encrypted by Android)
- Keys are never logged or transmitted except to Google APIs
- HTTPS is used for all API calls
- Logging level is set to BASIC (no request/response bodies logged)

## API Documentation

- [Custom Search JSON API Overview](https://developers.google.com/custom-search/v1/overview)
- [API Reference](https://developers.google.com/custom-search/v1/reference/rest/v1/cse/list)
- [Pricing](https://developers.google.com/custom-search/v1/overview#pricing)

## Support

For issues specific to NewsAgent's Google Custom Search integration:
1. Check app logs: Settings > Troubleshooting > View App Logs
2. Look for lines containing "Google Custom Search" or "SearchStrategySelector"
3. Share logs via Settings > Share Logs (for Support)

For Google API issues:
- [Google Cloud Support](https://cloud.google.com/support)
- [Stack Overflow - google-custom-search tag](https://stackoverflow.com/questions/tagged/google-custom-search)
