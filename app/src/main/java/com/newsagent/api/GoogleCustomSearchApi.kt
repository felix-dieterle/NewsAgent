package com.newsagent.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API interface for Google Custom Search API
 * Requires:
 * 1. API Key from Google Cloud Console
 * 2. Custom Search Engine ID (cx parameter)
 * 
 * Free tier: 100 queries per day
 * Documentation: https://developers.google.com/custom-search/v1/overview
 */
interface GoogleCustomSearchApi {
    
    /**
     * Search using Google Custom Search API
     * @param query The search query
     * @param apiKey Google API key
     * @param cx Custom Search Engine ID
     * @param num Number of results (1-10, default 10)
     */
    @GET("customsearch/v1")
    suspend fun search(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("cx") cx: String,
        @Query("num") num: Int = 10
    ): Response<GoogleSearchResponse>
}

/**
 * Response from Google Custom Search API
 */
data class GoogleSearchResponse(
    @SerializedName("kind")
    val kind: String,
    
    @SerializedName("items")
    val items: List<GoogleSearchItem>?
)

/**
 * Single search result item
 */
data class GoogleSearchItem(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("link")
    val link: String,
    
    @SerializedName("snippet")
    val snippet: String?,
    
    @SerializedName("pagemap")
    val pagemap: GooglePageMap?
)

/**
 * Page metadata from Google search result
 */
data class GooglePageMap(
    @SerializedName("metatags")
    val metatags: List<Map<String, String>>?,
    
    @SerializedName("cse_image")
    val cseImage: List<GoogleImage>?
)

/**
 * Image from search result
 */
data class GoogleImage(
    @SerializedName("src")
    val src: String
)
