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
     */
    private fun parseRssXml(xmlContent: String, sourceName: String): List<RssArticle> {
        val articles = mutableListOf<RssArticle>()
        
        try {
            val factory = DocumentBuilderFactory.newInstance()
            
            // Security: Disable XXE (XML External Entity) attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            factory.isXIncludeAware = false
            factory.isExpandEntityReferences = false
            
            val builder = factory.newDocumentBuilder()
            val inputSource = InputSource(StringReader(xmlContent))
            val doc: Document = builder.parse(inputSource)
            
            doc.documentElement.normalize()
            
            val itemList = doc.getElementsByTagName("item")
            
            for (i in 0 until itemList.length) {
                val itemNode = itemList.item(i)
                
                if (itemNode.nodeType == Node.ELEMENT_NODE) {
                    val element = itemNode as Element
                    
                    val title = getElementText(element, "title") ?: continue
                    val description = getElementText(element, "description")
                    val link = getElementText(element, "link") ?: continue
                    val pubDate = getElementText(element, "pubDate")
                    
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
