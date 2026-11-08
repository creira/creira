package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Feed

@Entity(
    tableName = "feeds",
    indices = [Index(value = ["url"], unique = true)]
)
data class FeedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val url: String,
    val siteUrl: String = "",
    val iconUrl: String = "",
    val category: String = "General",
    val lastFetched: Long = 0L
) {
    fun toFeed(unreadCount: Int = 0): Feed = Feed(
        id = id,
        title = title,
        description = description,
        url = url,
        siteUrl = siteUrl,
        iconUrl = iconUrl,
        category = category,
        unreadCount = unreadCount,
        lastFetched = lastFetched
    )

    companion object {
        fun fromFeed(feed: Feed): FeedEntity = FeedEntity(
            id = feed.id,
            title = feed.title,
            description = feed.description,
            url = feed.url,
            siteUrl = feed.siteUrl,
            iconUrl = feed.iconUrl,
            category = feed.category,
            lastFetched = feed.lastFetched
        )
    }
}
