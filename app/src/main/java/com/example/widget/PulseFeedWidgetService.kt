package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.R
import com.example.data.local.PulseFeedDatabase
import com.example.data.model.Article
import com.example.repository.FeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class PulseFeedWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return PulseFeedRemoteViewsFactory(applicationContext)
    }
}

class PulseFeedRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var articles: List<Article> = emptyList()
    private val imageLoader: ImageLoader by lazy { ImageLoader(context) }

    override fun onCreate() {
        loadArticles()
    }

    override fun onDataSetChanged() {
        loadArticles()
    }

    private fun loadArticles() {
        runBlocking(Dispatchers.IO) {
            try {
                val db = PulseFeedDatabase.getInstance(context)
                // Allow all unread articles without artificial limitation
                var entities = db.articleDao().getAllUnreadArticles()
                if (entities.isEmpty()) {
                    // Fallback to recent articles if all articles are read
                    entities = db.articleDao().getRecentArticles(100)
                }
                if (entities.isEmpty()) {
                    val repo = FeedRepository(context)
                    repo.initializePresetFeedsIfNeeded()
                    entities = db.articleDao().getAllUnreadArticles()
                    if (entities.isEmpty()) {
                        entities = db.articleDao().getRecentArticles(100)
                    }
                }
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
        views.setTextViewText(R.id.widget_item_time, "${article.estimatedReadMinutes}m read")
        views.setTextViewText(R.id.widget_item_headline, article.title)

        // Load article graphic if available
        if (!article.imageUrl.isNullOrBlank()) {
            val bitmap = fetchArticleBitmap(article.imageUrl)
            if (bitmap != null) {
                views.setImageViewBitmap(R.id.widget_item_image, bitmap)
            } else {
                views.setImageViewResource(R.id.widget_item_image, R.drawable.widget_image_placeholder)
            }
        } else {
            views.setImageViewResource(R.id.widget_item_image, R.drawable.widget_image_placeholder)
        }

        // Visual distinction between unread and read articles
        val headlineColor = if (article.isRead) 0xFF9E9E9E.toInt() else 0xFFFFFFFF.toInt()
        val timeColor = if (article.isRead) 0xFF718096.toInt() else 0xFFD1D5DB.toInt()
        views.setTextColor(R.id.widget_item_headline, headlineColor)
        views.setTextColor(R.id.widget_item_time, timeColor)

        // FillInIntent for card item click
        val fillInIntent = Intent().apply {
            action = PulseFeedWidgetProvider.ACTION_OPEN_ARTICLE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(PulseFeedWidgetProvider.EXTRA_OPEN_ARTICLE_ID, article.id)
            data = android.net.Uri.parse("pulsefeed://article/${article.id}")
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_content, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_headline, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_image, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_scrim, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_feed, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_time, fillInIntent)

        return views
    }

    private fun fetchArticleBitmap(url: String): Bitmap? {
        return try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(360, 240)
                .allowHardware(false) // Must be software bitmap for Binder IPC
                .build()
            val result = runBlocking(Dispatchers.IO) { imageLoader.execute(request) }
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_pulsefeed_item)
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = if (position in articles.indices) articles[position].id else position.toLong()

    override fun hasStableIds(): Boolean = true
}
