package com.example.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.local.PulseFeedDatabase
import com.example.data.model.Article
import kotlinx.coroutines.runBlocking

class PulseFeedWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return PulseFeedRemoteViewsFactory(applicationContext)
    }
}

class PulseFeedRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var articles: List<Article> = emptyList()

    override fun onCreate() {
        // Init
    }

    override fun onDataSetChanged() {
        runBlocking {
            try {
                val db = PulseFeedDatabase.getInstance(context)
                val entities = db.articleDao().getRecentArticles(40)
                articles = entities.map { it.toArticle() }
            } catch (_: Exception) {
                articles = emptyList()
            }
        }
    }

    override fun onDestroy() {
        articles = emptyList()
    }

    override fun getCount(): Int = articles.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= articles.size) return null
        val article = articles[position]

        val views = RemoteViews(context.packageName, R.layout.widget_pulsefeed_item)
        views.setTextViewText(R.id.widget_item_feed, article.feedTitle.uppercase())
        views.setTextViewText(R.id.widget_item_time, "${article.estimatedReadMinutes}m")
        views.setTextViewText(R.id.widget_item_headline, article.title)

        // Card read vs unread visual distinction
        val bgRes = if (article.isRead) R.drawable.widget_card_bg_read else R.drawable.widget_card_bg
        views.setInt(R.id.widget_item_root, "setBackgroundResource", bgRes)
        val textColor = if (article.isRead) 0xFF8A94A6.toInt() else 0xFFFFFFFF.toInt()
        views.setTextColor(R.id.widget_item_headline, textColor)

        // FillInIntent for card item click
        val fillInIntent = Intent().apply {
            putExtra("OPEN_ARTICLE_ID", article.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = if (position in articles.indices) articles[position].id else position.toLong()

    override fun hasStableIds(): Boolean = true
}
