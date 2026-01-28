package com.newsagent.api

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import java.net.URL
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
     * Parse RSS feed from a URL
     */
    fun parseRssFeed(feedUrl: String, sourceName: String): List<RssArticle> {
        return try {
            val xmlContent = URL(feedUrl).readText()
            parseRssXml(xmlContent, sourceName)
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
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
            "Heise Online" to "https://www.heise.de/rss/heise-atom.xml",
            "Spiegel Online" to "https://www.spiegel.de/schlagzeilen/index.rss",
            "Zeit Online" to "https://newsfeed.zeit.de/index"
        )
    }
}
