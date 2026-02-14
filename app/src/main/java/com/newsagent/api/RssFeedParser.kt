package com.newsagent.api

import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Simple RSS feed parser for truly free news sources
 * No API keys required - uses public RSS feeds
 */
class RssFeedParser {
    
    data class RssArticle(
        val title: String,
        val description: String?,
        val link: String,
        val pubDate: String?,
        val source: String
    )
    
    /**
     * Parse RSS feed from XML content
     * Note: Feed fetching should be done separately using proper HTTP client (e.g., OkHttp)
     */
    fun parseRssContent(xmlContent: String, sourceName: String): List<RssArticle> {
        return try {
            parseRssXml(xmlContent, sourceName)
        } catch (e: Exception) {
            Log.e("RssFeedParser", "Error parsing RSS content from $sourceName", e)
            emptyList()
        }
    }
    
    /**
     * Parse RSS XML content
     * Supports both RSS 2.0 (<item>) and RSS 1.0/RDF (<item>) formats
     */
    private fun parseRssXml(xmlContent: String, sourceName: String): List<RssArticle> {
        val articles = mutableListOf<RssArticle>()
        
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true  // Enable namespace awareness for RDF
            
            // Security: Disable XXE (XML External Entity) attacks
            // Note: We allow DOCTYPE declarations (needed for RSS feeds) but disable external entities
            try {
                // Don't disallow DOCTYPE - RSS feeds need it
                // factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                
                // But disable external entities to prevent XXE attacks
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                factory.isXIncludeAware = false
                factory.isExpandEntityReferences = false
                Log.d("RssFeedParser", "XXE protection features enabled for $sourceName")
            } catch (e: Exception) {
                // Log which features failed - this is important for security
                Log.e("RssFeedParser", "WARNING: Failed to set XXE protection features for $sourceName - parser may be vulnerable", e)
                // Continue anyway as basic features like isXIncludeAware are always available
            }
            
            val builder = factory.newDocumentBuilder()
            val inputSource = InputSource(StringReader(xmlContent))
            val doc: Document = builder.parse(inputSource)
            
            doc.documentElement.normalize()
            
            Log.d("RssFeedParser", "Parsing $sourceName: Root element = ${doc.documentElement.tagName}")
            
            // Try to find items - works for both RSS 2.0 and RSS 1.0/RDF
            val itemList = doc.getElementsByTagName("item")
            
            Log.d("RssFeedParser", "Found ${itemList.length} items in $sourceName")
            
            for (i in 0 until itemList.length) {
                val itemNode = itemList.item(i)
                
                if (itemNode.nodeType == Node.ELEMENT_NODE) {
                    val element = itemNode as Element
                    
                    val title = getElementText(element, "title") ?: continue
                    val description = getElementText(element, "description")
                    val link = getElementText(element, "link") ?: continue
                    // Try RSS 2.0 pubDate first, then fall back to Dublin Core (dc:date) for RSS 1.0/RDF
                    val pubDate = getElementText(element, "pubDate") ?: getElementText(element, "dc:date")
                    
                    articles.add(
                        RssArticle(
                            title = title,
                            description = description,
                            link = link,
                            pubDate = pubDate,
                            source = sourceName
                        )
                    )
                }
            }
            
            Log.d("RssFeedParser", "Successfully parsed ${articles.size} articles from $sourceName")
        } catch (e: Exception) {
            Log.e("RssFeedParser", "Error in parseRssXml for $sourceName", e)
        }
        
        return articles
    }
    
    private fun getElementText(element: Element, tagName: String): String? {
        val nodeList = element.getElementsByTagName(tagName)
        if (nodeList.length > 0) {
            val node = nodeList.item(0)
            return node?.textContent?.trim()
        }
        return null
    }
    
    companion object {
        // Public German news RSS feeds that don't require authentication
        val GERMAN_RSS_FEEDS = mapOf(
            "Tagesschau" to "https://www.tagesschau.de/xml/rss2/",
            "Heise Online" to "https://www.heise.de/rss/heise.rdf",  // Using RDF format instead of Atom
            "Spiegel Online" to "https://www.spiegel.de/schlagzeilen/index.rss"
            // Zeit Online feed removed as it may require special handling
        )
    }
}
