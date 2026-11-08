package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Article

@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["link"], unique = true),
        Index(value = ["feedId"]),
        Index(value = ["pubDateTimestamp"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feedId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val feedId: Long,
    val feedTitle: String,
    val title: String,
    val link: String,
    val author: String = "",
    val pubDate: String = "",
    val pubDateTimestamp: Long = 0L,
    val description: String = "",
    val contentHtml: String = "",
    val plainText: String = "",
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    val readProgress: Float = 0f,
    val isBookmarked: Boolean = false,
    val estimatedReadMinutes: Int = 1,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toArticle(): Article = Article(
        id = id,
        feedId = feedId,
        feedTitle = feedTitle,
        title = title,
        link = link,
        author = author,
        pubDate = pubDate,
        pubDateTimestamp = pubDateTimestamp,
        description = description,
        contentHtml = contentHtml,
        plainText = plainText,
        imageUrl = imageUrl,
        isRead = isRead,
        readProgress = readProgress,
        isBookmarked = isBookmarked,
        estimatedReadMinutes = estimatedReadMinutes,
        cachedAt = cachedAt
    )

    companion object {
        fun fromArticle(article: Article): ArticleEntity = ArticleEntity(
            id = article.id,
            feedId = article.feedId,
            feedTitle = article.feedTitle,
            title = article.title,
            link = article.link,
            author = article.author,
            pubDate = article.pubDate,
            pubDateTimestamp = article.pubDateTimestamp,
            description = article.description,
            contentHtml = article.contentHtml,
            plainText = article.plainText,
            imageUrl = article.imageUrl,
            isRead = article.isRead,
            readProgress = article.readProgress,
            isBookmarked = article.isBookmarked,
            estimatedReadMinutes = article.estimatedReadMinutes,
            cachedAt = article.cachedAt
        )
    }
}
