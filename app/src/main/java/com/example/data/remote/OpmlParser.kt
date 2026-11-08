package com.example.data.remote

import android.util.Xml
import com.example.data.model.Feed
import com.example.data.model.OpmlOutline
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object OpmlParser {

    val SAMPLE_PRESETS = listOf(
        OpmlOutline(
            title = "Android Developers Blog",
            xmlUrl = "https://android-developers.googleblog.com/feeds/posts/default?alt=rss",
            htmlUrl = "https://android-developers.googleblog.com",
            category = "Technology"
        ),
        OpmlOutline(
            title = "The Verge",
            xmlUrl = "https://www.theverge.com/rss/index.xml",
            htmlUrl = "https://www.theverge.com",
            category = "Technology"
        ),
        OpmlOutline(
            title = "Ars Technica",
            xmlUrl = "https://feeds.arstechnica.com/arstechnica/index",
            htmlUrl = "https://arstechnica.com",
            category = "Technology"
        ),
        OpmlOutline(
            title = "NASA Breaking News",
            xmlUrl = "https://www.nasa.gov/rss/dyn/breaking_news.rss",
            htmlUrl = "https://www.nasa.gov",
            category = "Science"
        ),
        OpmlOutline(
            title = "BBC World News",
            xmlUrl = "https://feeds.bbci.co.uk/news/world/rss.xml",
            htmlUrl = "https://www.bbc.com/news",
            category = "World"
        ),
        OpmlOutline(
            title = "ScienceDaily Top News",
            xmlUrl = "https://www.sciencedaily.com/rss/top/science.xml",
            htmlUrl = "https://www.sciencedaily.com",
            category = "Science"
        )
    )

    fun parse(opmlContent: String): List<OpmlOutline> {
        val outlines = mutableListOf<OpmlOutline>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(opmlContent))

        var currentCategory = "General"
        val categoryStack = ArrayDeque<String>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            val eventType = parser.eventType
            val tagName = parser.name ?: continue

            if (eventType == XmlPullParser.START_TAG) {
                if (tagName.equals("outline", ignoreCase = true)) {
                    val title = parser.getAttributeValue(null, "title")
                        ?: parser.getAttributeValue(null, "text")
                        ?: "Feed"
                    val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                        ?: parser.getAttributeValue(null, "xmlurl")
                    val htmlUrl = parser.getAttributeValue(null, "htmlUrl")
                        ?: parser.getAttributeValue(null, "htmlurl")
                        ?: ""

                    if (!xmlUrl.isNullOrBlank()) {
                        outlines.add(
                            OpmlOutline(
                                title = title.trim(),
                                xmlUrl = xmlUrl.trim(),
                                htmlUrl = htmlUrl.trim(),
                                category = if (categoryStack.isNotEmpty()) categoryStack.first() else currentCategory
                            )
                        )
                    } else {
                        // Category outline node
                        categoryStack.addFirst(title.trim())
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (tagName.equals("outline", ignoreCase = true)) {
                    if (categoryStack.isNotEmpty()) {
                        categoryStack.removeFirstOrNull()
                    }
                }
            }
        }
        return outlines
    }

    fun generateOpml(feeds: List<Feed>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<opml version=\"2.0\">\n")
        sb.append("  <head>\n")
        sb.append("    <title>PulseFeed Subscriptions</title>\n")
        sb.append("  </head>\n")
        sb.append("  <body>\n")

        val grouped = feeds.groupBy { it.category }
        for ((category, feedList) in grouped) {
            sb.append("    <outline text=\"${escapeXml(category)}\">\n")
            for (feed in feedList) {
                sb.append("      <outline type=\"rss\" ")
                sb.append("text=\"${escapeXml(feed.title)}\" ")
                sb.append("title=\"${escapeXml(feed.title)}\" ")
                sb.append("xmlUrl=\"${escapeXml(feed.url)}\" ")
                if (feed.siteUrl.isNotEmpty()) {
                    sb.append("htmlUrl=\"${escapeXml(feed.siteUrl)}\" ")
                }
                sb.append("/>\n")
            }
            sb.append("    </outline>\n")
        }

        sb.append("  </body>\n")
        sb.append("</opml>")
        return sb.toString()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
