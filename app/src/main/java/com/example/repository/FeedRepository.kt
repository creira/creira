package com.example.repository

import android.content.Context
import com.example.data.local.ArticleEntity
import com.example.data.local.FeedEntity
import com.example.data.local.PulseFeedDatabase
import com.example.data.model.Article
import com.example.data.model.ArticleFilter
import com.example.data.model.Feed
import com.example.data.model.OpmlOutline
import com.example.data.remote.FeedFetcher
import com.example.data.remote.OpmlParser
import com.example.data.remote.RssParser
import com.example.widget.PulseFeedWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class FeedRepository(private val context: Context) {

    private val db = PulseFeedDatabase.getInstance(context)
    private val feedDao = db.feedDao()
    private val articleDao = db.articleDao()

    val feedsFlow: Flow<List<Feed>> = combine(
        feedDao.getAllFeedsFlow(),
        articleDao.getAllArticlesFlow()
    ) { feedEntities, allArticles ->
        val unreadCounts = allArticles.filter { !it.isRead }.groupBy { it.feedId }
        feedEntities.map { entity ->
            entity.toFeed(unreadCount = unreadCounts[entity.id]?.size ?: 0)
        }
    }.flowOn(Dispatchers.IO)

    fun getArticlesFlow(
        feedId: Long?,
        filter: ArticleFilter,
        searchQuery: String = ""
    ): Flow<List<Article>> {
        val baseFlow = when {
            searchQuery.isNotBlank() -> articleDao.searchArticlesFlow(searchQuery)
            feedId != null -> articleDao.getArticlesByFeedFlow(feedId)
            filter == ArticleFilter.BOOKMARKS -> articleDao.getBookmarkedArticlesFlow()
            filter == ArticleFilter.UNREAD_ONLY -> articleDao.getUnreadArticlesFlow()
            else -> articleDao.getAllArticlesFlow()
        }

        return combine(baseFlow) { listArray ->
            var articles = listArray[0].map { it.toArticle() }
            if (feedId != null && searchQuery.isNotBlank()) {
                articles = articles.filter { it.feedId == feedId }
            }
            if (filter == ArticleFilter.BOOKMARKS && (feedId != null || searchQuery.isNotBlank())) {
                articles = articles.filter { it.isBookmarked }
            }
            if (filter == ArticleFilter.UNREAD_ONLY && (feedId != null || searchQuery.isNotBlank())) {
                articles = articles.filter { !it.isRead }
            }
            articles.sortedWith(
                compareByDescending<Article> {
                    if (it.pubDateTimestamp > 0L) it.pubDateTimestamp else it.cachedAt
                }.thenByDescending { it.id }
            )
        }.flowOn(Dispatchers.IO)
    }

    suspend fun initializePresetFeedsIfNeeded() = withContext(Dispatchers.IO) {
        val count = feedDao.getFeedCount()
        if (count == 0) {
            importOutlines(OpmlParser.SAMPLE_PRESETS)
        }
    }

    suspend fun addFeed(url: String, category: String = "General"): Result<Feed> = withContext(Dispatchers.IO) {
        val existing = feedDao.getFeedByUrl(url)
        if (existing != null) {
            return@withContext Result.success(existing.toFeed())
        }

        val fetchResult = FeedFetcher.fetchRss(url, category)
        if (fetchResult.isFailure) {
            return@withContext Result.failure(fetchResult.exceptionOrNull() ?: Exception("Failed to fetch feed"))
        }

        val parsed = fetchResult.getOrThrow()
        val feedId = feedDao.insertFeed(FeedEntity.fromFeed(parsed.feed))
        val articlesToInsert = parsed.articles.map { article ->
            ArticleEntity.fromArticle(article.copy(feedId = feedId, feedTitle = parsed.feed.title))
        }
        articleDao.insertArticles(articlesToInsert)

        updateWidget()
        Result.success(parsed.feed.copy(id = feedId))
    }

    suspend fun importOpml(opmlContent: String): Int = withContext(Dispatchers.IO) {
        val outlines = OpmlParser.parse(opmlContent)
        importOutlines(outlines)
    }

    suspend fun importOutlines(outlines: List<OpmlOutline>): Int = withContext(Dispatchers.IO) {
        var addedCount = 0
        for (outline in outlines) {
            try {
                val existing = feedDao.getFeedByUrl(outline.xmlUrl)
                if (existing == null) {
                    val fetchResult = FeedFetcher.fetchRss(outline.xmlUrl, outline.category)
                    if (fetchResult.isSuccess) {
                        val parsed = fetchResult.getOrThrow()
                        val feedToSave = parsed.feed.copy(
                            title = if (outline.title.isNotBlank() && outline.title != "Feed") outline.title else parsed.feed.title,
                            siteUrl = if (outline.htmlUrl.isNotBlank()) outline.htmlUrl else parsed.feed.siteUrl,
                            category = outline.category
                        )
                        val feedId = feedDao.insertFeed(FeedEntity.fromFeed(feedToSave))
                        val articlesToInsert = parsed.articles.map { article ->
                            ArticleEntity.fromArticle(article.copy(feedId = feedId, feedTitle = feedToSave.title))
                        }
                        articleDao.insertArticles(articlesToInsert)
                        addedCount++
                    } else {
                        // Insert placeholder feed record even if offline so user sees the feed
                        val feedId = feedDao.insertFeed(
                            FeedEntity(
                                title = outline.title,
                                url = outline.xmlUrl,
                                siteUrl = outline.htmlUrl,
                                category = outline.category,
                                iconUrl = OpmlParser.SAMPLE_PRESETS.firstOrNull { it.xmlUrl == outline.xmlUrl }?.let {
                                    RssParser.extractFavicon(it.htmlUrl)
                                } ?: ""
                            )
                        )
                        addedCount++
                    }
                }
            } catch (_: Exception) {
            }
        }
        updateWidget()
        addedCount
    }

    suspend fun exportOpml(): String = withContext(Dispatchers.IO) {
        val feeds = feedDao.getAllFeeds().map { it.toFeed() }
        OpmlParser.generateOpml(feeds)
    }

    suspend fun syncFeed(feedId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val feed = feedDao.getFeedById(feedId) ?: return@withContext Result.failure(Exception("Feed not found"))
        val fetchResult = FeedFetcher.fetchRss(feed.url, feed.category)
        if (fetchResult.isFailure) {
            return@withContext Result.failure(fetchResult.exceptionOrNull() ?: Exception("Sync failed"))
        }

        val parsed = fetchResult.getOrThrow()
        feedDao.updateFeed(
            feed.copy(
                title = if (feed.title.isBlank() || feed.title == "Feed") parsed.feed.title else feed.title,
                siteUrl = if (feed.siteUrl.isBlank()) parsed.feed.siteUrl else feed.siteUrl,
                iconUrl = if (feed.iconUrl.isBlank()) parsed.feed.iconUrl else feed.iconUrl,
                lastFetched = System.currentTimeMillis()
            )
        )

        // Insert new articles only (OnConflictStrategy.IGNORE preserves existing read/progress status)
        val articlesToInsert = parsed.articles.map {
            ArticleEntity.fromArticle(it.copy(feedId = feed.id, feedTitle = feed.title))
        }
        articleDao.insertArticles(articlesToInsert)

        updateWidget()
        Result.success(Unit)
    }

    suspend fun syncAllFeeds(): Result<Int> = withContext(Dispatchers.IO) {
        val feeds = feedDao.getAllFeeds()
        var successCount = 0
        for (feed in feeds) {
            val result = syncFeed(feed.id)
            if (result.isSuccess) successCount++
        }
        updateWidget()
        Result.success(successCount)
    }

    suspend fun deleteFeed(feedId: Long) = withContext(Dispatchers.IO) {
        feedDao.deleteFeedById(feedId)
        updateWidget()
    }

    suspend fun updateReadingProgress(articleId: Long, progress: Float) = withContext(Dispatchers.IO) {
        val clamped = progress.coerceIn(0f, 1f)
        val isRead = clamped >= 0.88f
        articleDao.updateReadStatus(articleId, isRead, clamped)
        updateWidget()
    }

    suspend fun setArticleRead(articleId: Long, isRead: Boolean) = withContext(Dispatchers.IO) {
        val progress = if (isRead) 1.0f else 0.0f
        articleDao.updateReadStatus(articleId, isRead, progress)
        updateWidget()
    }

    suspend fun toggleBookmark(articleId: Long, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        articleDao.updateBookmarkStatus(articleId, isBookmarked)
    }

    suspend fun markAllAsRead(feedId: Long? = null) = withContext(Dispatchers.IO) {
        articleDao.markAllAsRead(feedId)
        updateWidget()
    }

    suspend fun getRecentArticles(limit: Int = 20): List<Article> = withContext(Dispatchers.IO) {
        articleDao.getRecentArticles(limit).map { it.toArticle() }
    }

    suspend fun getRandomUnreadArticle(): Article? = withContext(Dispatchers.IO) {
        articleDao.getRandomUnreadArticle()?.toArticle()
    }

    suspend fun getArticleById(articleId: Long): Article? = withContext(Dispatchers.IO) {
        articleDao.getArticleById(articleId)?.toArticle()
    }

    private fun updateWidget() {
        try {
            PulseFeedWidgetProvider.triggerUpdate(context)
        } catch (_: Exception) {
        }
    }
}
