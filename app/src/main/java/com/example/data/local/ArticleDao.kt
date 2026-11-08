package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY CASE WHEN pubDateTimestamp > 0 THEN pubDateTimestamp ELSE cachedAt END DESC, id DESC")
    fun getAllArticlesFlow(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE feedId = :feedId ORDER BY CASE WHEN pubDateTimestamp > 0 THEN pubDateTimestamp ELSE cachedAt END DESC, id DESC")
    fun getArticlesByFeedFlow(feedId: Long): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY CASE WHEN pubDateTimestamp > 0 THEN pubDateTimestamp ELSE cachedAt END DESC, id DESC")
    fun getBookmarkedArticlesFlow(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isRead = 0 ORDER BY CASE WHEN pubDateTimestamp > 0 THEN pubDateTimestamp ELSE cachedAt END DESC, id DESC")
    fun getUnreadArticlesFlow(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: Long): ArticleEntity?

    @Query("SELECT * FROM articles WHERE link = :link LIMIT 1")
    suspend fun getArticleByLink(link: String): ArticleEntity?

    @Query("SELECT * FROM articles ORDER BY CASE WHEN pubDateTimestamp > 0 THEN pubDateTimestamp ELSE cachedAt END DESC, id DESC LIMIT :limit")
    suspend fun getRecentArticles(limit: Int): List<ArticleEntity>

    @Query("SELECT COUNT(*) FROM articles WHERE feedId = :feedId AND isRead = 0")
    suspend fun getUnreadCountForFeed(feedId: Long): Int

    @Query("SELECT COUNT(*) FROM articles WHERE isRead = 0")
    suspend fun getTotalUnreadCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticle(article: ArticleEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<ArticleEntity>): List<Long>

    @Update
    suspend fun updateArticle(article: ArticleEntity)

    @Query("UPDATE articles SET isRead = :isRead, readProgress = :progress WHERE id = :id")
    suspend fun updateReadStatus(id: Long, isRead: Boolean, progress: Float)

    @Query("UPDATE articles SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarkStatus(id: Long, isBookmarked: Boolean)

    @Query("UPDATE articles SET isRead = 1, readProgress = 1.0 WHERE (:feedId IS NULL OR feedId = :feedId)")
    suspend fun markAllAsRead(feedId: Long? = null)

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY CASE WHEN pubDateTimestamp > 0 THEN pubDateTimestamp ELSE cachedAt END DESC, id DESC")
    fun searchArticlesFlow(query: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isRead = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomUnreadArticle(): ArticleEntity?

    @Query("DELETE FROM articles WHERE isBookmarked = 0 AND isRead = 1 AND cachedAt < :cutoffTimestamp")
    suspend fun deleteOldReadArticles(cutoffTimestamp: Long)
}
