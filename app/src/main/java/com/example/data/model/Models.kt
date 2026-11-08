package com.example.data.model

data class Feed(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val url: String,
    val siteUrl: String = "",
    val iconUrl: String = "",
    val category: String = "General",
    val unreadCount: Int = 0,
    val lastFetched: Long = 0L
)

data class Article(
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
    val readProgress: Float = 0f, // 0.0 to 1.0
    val isBookmarked: Boolean = false,
    val estimatedReadMinutes: Int = 1,
    val cachedAt: Long = System.currentTimeMillis()
)

data class OpmlOutline(
    val title: String,
    val xmlUrl: String,
    val htmlUrl: String = "",
    val category: String = "General"
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class ArticleFilter {
    ALL, UNREAD_ONLY, BOOKMARKS
}
