package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.OpmlParser
import com.example.data.remote.RssParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PulseFeed", appName)
    }

    @Test
    fun `test RSS parser basic feed`() {
        val sampleRss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Test Tech News</title>
                <link>https://example.com</link>
                <description>Latest technology news</description>
                <item>
                  <title>Exciting New Gadget Released</title>
                  <link>https://example.com/article1</link>
                  <description>A revolutionary gadget was announced today.</description>
                  <pubDate>Mon, 08 Nov 2026 12:00:00 GMT</pubDate>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val result = RssParser.parse("https://example.com/rss", sampleRss)
        assertEquals("Test Tech News", result.feed.title)
        assertEquals(1, result.articles.size)
        assertEquals("Exciting New Gadget Released", result.articles[0].title)
    }

    @Test
    fun `test OPML parser and generator`() {
        val sampleOpml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <body>
                <outline text="Technology">
                  <outline type="rss" text="The Verge" title="The Verge" xmlUrl="https://www.theverge.com/rss/index.xml" />
                </outline>
              </body>
            </opml>
        """.trimIndent()

        val outlines = OpmlParser.parse(sampleOpml)
        assertEquals(1, outlines.size)
        assertEquals("The Verge", outlines[0].title)
        assertEquals("https://www.theverge.com/rss/index.xml", outlines[0].xmlUrl)
    }

    @Test
    fun `test RSS and Atom date parsing`() {
        val rfcDate = "Mon, 08 Nov 2026 12:00:00 GMT"
        val parsedRfc = RssParser.parseDateToMillis(rfcDate)
        assertTrue("RFC 822 date should parse to positive millis", parsedRfc > 0L)

        val isoDate = "2026-11-08T12:00:00Z"
        val parsedIso = RssParser.parseDateToMillis(isoDate)
        assertTrue("ISO 8601 date should parse to positive millis", parsedIso > 0L)

        assertEquals("Same UTC instant should match timestamp", parsedRfc, parsedIso)
    }
}
