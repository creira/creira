package com.example.data.remote

import android.util.Xml
import com.example.data.model.Article
import com.example.data.model.Feed
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

object RssParser {

    private val DATE_PATTERNS = listOf(
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, d MMM yyyy HH:mm:ss z",
        "EEE, d MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss z",
        "dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )

    private val IMG_TAG_PATTERN = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
    private val HTML_TAG_PATTERN = Pattern.compile("<[^>]+>")

    data class ParsedFeedResult(
        val feed: Feed,
        val articles: List<Article>
    )

    fun parse(feedUrl: String, xmlContent: String, defaultCategory: String = "General"): ParsedFeedResult {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xmlContent))

        var eventType = parser.eventType
        while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
            eventType = parser.next()
        }

        return if (parser.name.equals("feed", ignoreCase = true)) {
            parseAtom(feedUrl, parser, defaultCategory)
        } else {
            parseRss(feedUrl, parser, defaultCategory)
        }
    }

    private fun parseRss(feedUrl: String, parser: XmlPullParser, defaultCategory: String): ParsedFeedResult {
        var feedTitle = "RSS Feed"
        var feedDescription = ""
        var siteUrl = ""
        var feedIcon = ""
        val articles = mutableListOf<Article>()

        var insideItem = false
        var currentTitle = ""
        var currentLink = ""
        var currentAuthor = ""
        var currentPubDate = ""
        var currentDescription = ""
        var currentContent = ""
        var currentImageUrl: String? = null

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            val eventType = parser.eventType
            val tagName = parser.name ?: continue

            if (eventType == XmlPullParser.START_TAG) {
                if (tagName.equals("item", ignoreCase = true)) {
                    insideItem = true
                    currentTitle = ""
                    currentLink = ""
                    currentAuthor = ""
                    currentPubDate = ""
                    currentDescription = ""
                    currentContent = ""
                    currentImageUrl = null
                } else if (!insideItem) {
                    when (tagName.lowercase(Locale.ROOT)) {
                        "title" -> feedTitle = readText(parser).trim()
                        "description" -> feedDescription = readText(parser).trim()
                        "link" -> siteUrl = readText(parser).trim()
                        "image" -> { /* nested image url will be picked up */ }
                        "url" -> if (feedIcon.isEmpty()) feedIcon = readText(parser).trim()
                    }
                } else {
                    when (tagName.lowercase(Locale.ROOT)) {
                        "title" -> currentTitle = readText(parser).trim()
                        "link" -> currentLink = readText(parser).trim()
                        "dc:creator", "author" -> currentAuthor = readText(parser).trim()
                        "pubdate", "dc:date" -> currentPubDate = readText(parser).trim()
                        "description" -> currentDescription = readText(parser).trim()
                        "content:encoded", "content" -> currentContent = readText(parser).trim()
                        "enclosure" -> {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            val url = parser.getAttributeValue(null, "url")
                            if (type.startsWith("image/") && !url.isNullOrBlank()) {
                                currentImageUrl = url
                            }
                        }
                        "media:content", "media:thumbnail" -> {
                            val url = parser.getAttributeValue(null, "url")
                            if (!url.isNullOrBlank() && currentImageUrl == null) {
                                currentImageUrl = url
                            }
                        }
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (tagName.equals("item", ignoreCase = true)) {
                    insideItem = false
                    val fullContent = if (currentContent.isNotBlank()) currentContent else currentDescription
                    val plain = cleanHtml(if (fullContent.isNotBlank()) fullContent else currentTitle)
                    val img = currentImageUrl ?: extractImageFromHtml(currentContent) ?: extractImageFromHtml(currentDescription)
                    val timestamp = parseDateToMillis(currentPubDate)
                    val readTime = calculateReadingMinutes(plain)

                    if (currentTitle.isNotBlank() || currentLink.isNotBlank()) {
                        articles.add(
                            Article(
                                feedId = 0,
                                feedTitle = feedTitle,
                                title = if (currentTitle.isNotBlank()) cleanHtml(currentTitle) else "Untitled",
                                link = currentLink,
                                author = currentAuthor,
                                pubDate = currentPubDate,
                                pubDateTimestamp = timestamp,
                                description = cleanHtml(currentDescription),
                                contentHtml = fullContent,
                                plainText = plain,
                                imageUrl = img,
                                isRead = false,
                                readProgress = 0f,
                                isBookmarked = false,
                                estimatedReadMinutes = readTime
                            )
                        )
                    }
                }
            }
        }

        if (feedIcon.isEmpty() && siteUrl.isNotEmpty()) {
            feedIcon = extractFavicon(siteUrl)
        }

        val feed = Feed(
            title = feedTitle,
            description = feedDescription,
            url = feedUrl,
            siteUrl = siteUrl,
            iconUrl = feedIcon,
            category = defaultCategory,
            lastFetched = System.currentTimeMillis()
        )

        return ParsedFeedResult(feed, articles)
    }

    private fun parseAtom(feedUrl: String, parser: XmlPullParser, defaultCategory: String): ParsedFeedResult {
        var feedTitle = "Atom Feed"
        var feedDescription = ""
        var siteUrl = ""
        var feedIcon = ""
        val articles = mutableListOf<Article>()

        var insideEntry = false
        var currentTitle = ""
        var currentLink = ""
        var currentAuthor = ""
        var currentPubDate = ""
        var currentSummary = ""
        var currentContent = ""
        var currentImageUrl: String? = null

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            val eventType = parser.eventType
            val tagName = parser.name ?: continue

            if (eventType == XmlPullParser.START_TAG) {
                if (tagName.equals("entry", ignoreCase = true)) {
                    insideEntry = true
                    currentTitle = ""
                    currentLink = ""
                    currentAuthor = ""
                    currentPubDate = ""
                    currentSummary = ""
                    currentContent = ""
                    currentImageUrl = null
                } else if (!insideEntry) {
                    when (tagName.lowercase(Locale.ROOT)) {
                        "title" -> feedTitle = readText(parser).trim()
                        "subtitle" -> feedDescription = readText(parser).trim()
                        "icon", "logo" -> feedIcon = readText(parser).trim()
                        "link" -> {
                            val rel = parser.getAttributeValue(null, "rel") ?: "alternate"
                            if (rel == "alternate" || siteUrl.isEmpty()) {
                                siteUrl = parser.getAttributeValue(null, "href") ?: ""
                            }
                        }
                    }
                } else {
                    when (tagName.lowercase(Locale.ROOT)) {
                        "title" -> currentTitle = readText(parser).trim()
                        "link" -> {
                            val rel = parser.getAttributeValue(null, "rel") ?: "alternate"
                            val href = parser.getAttributeValue(null, "href") ?: ""
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (type.startsWith("image/")) {
                                currentImageUrl = href
                            } else if (rel == "alternate" || currentLink.isEmpty()) {
                                currentLink = href
                            }
                        }
                        "name" -> currentAuthor = readText(parser).trim()
                        "published", "updated" -> if (currentPubDate.isEmpty()) currentPubDate = readText(parser).trim()
                        "summary" -> currentSummary = readText(parser).trim()
                        "content" -> currentContent = readText(parser).trim()
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (tagName.equals("entry", ignoreCase = true)) {
                    insideEntry = false
                    val fullContent = if (currentContent.isNotBlank()) currentContent else currentSummary
                    val plain = cleanHtml(if (fullContent.isNotBlank()) fullContent else currentTitle)
                    val img = currentImageUrl ?: extractImageFromHtml(currentContent) ?: extractImageFromHtml(currentSummary)
                    val timestamp = parseDateToMillis(currentPubDate)
                    val readTime = calculateReadingMinutes(plain)

                    if (currentTitle.isNotBlank() || currentLink.isNotBlank()) {
                        articles.add(
                            Article(
                                feedId = 0,
                                feedTitle = feedTitle,
                                title = cleanHtml(currentTitle),
                                link = currentLink,
                                author = currentAuthor,
                                pubDate = currentPubDate,
                                pubDateTimestamp = timestamp,
                                description = cleanHtml(currentSummary),
                                contentHtml = fullContent,
                                plainText = plain,
                                imageUrl = img,
                                isRead = false,
                                readProgress = 0f,
                                isBookmarked = false,
                                estimatedReadMinutes = readTime
                            )
                        )
                    }
                }
            }
        }

        if (feedIcon.isEmpty() && siteUrl.isNotEmpty()) {
            feedIcon = extractFavicon(siteUrl)
        }

        val feed = Feed(
            title = feedTitle,
            description = feedDescription,
            url = feedUrl,
            siteUrl = siteUrl,
            iconUrl = feedIcon,
            category = defaultCategory,
            lastFetched = System.currentTimeMillis()
        )

        return ParsedFeedResult(feed, articles)
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text ?: ""
            parser.nextTag()
        }
        return result
    }

    fun cleanHtml(html: String): String {
        if (html.isBlank()) return ""
        var text = HTML_TAG_PATTERN.matcher(html).replaceAll(" ")
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("\\s+".toRegex(), " ")
            .trim()
        return text
    }

    fun extractImageFromHtml(html: String): String? {
        if (html.isBlank()) return null
        val matcher = IMG_TAG_PATTERN.matcher(html)
        if (matcher.find()) {
            val url = matcher.group(1)
            if (!url.isNullOrBlank() && !url.startsWith("data:") && !url.endsWith(".gif")) {
                return url
            }
        }
        return null
    }

    fun parseDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val cleaned = dateStr.trim()
        for (pattern in DATE_PATTERNS) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                if (pattern.contains("'Z'")) {
                    format.timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = format.parse(cleaned)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return 0L
    }

    fun calculateReadingMinutes(plainText: String): Int {
        val wordCount = plainText.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
        val minutes = (wordCount / 180.0).coerceAtLeast(1.0).toInt()
        return minOf(minutes, 60)
    }

    fun extractFavicon(siteUrl: String): String {
        return try {
            val uri = android.net.Uri.parse(siteUrl)
            val host = uri.host ?: return ""
            "https://www.google.com/s2/favicons?domain=$host&sz=128"
        } catch (_: Exception) {
            ""
        }
    }
}
