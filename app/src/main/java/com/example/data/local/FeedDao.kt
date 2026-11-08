package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM feeds ORDER BY title ASC")
    fun getAllFeedsFlow(): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feeds ORDER BY title ASC")
    suspend fun getAllFeeds(): List<FeedEntity>

    @Query("SELECT * FROM feeds WHERE id = :id LIMIT 1")
    suspend fun getFeedById(id: Long): FeedEntity?

    @Query("SELECT * FROM feeds WHERE url = :url LIMIT 1")
    suspend fun getFeedByUrl(url: String): FeedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: FeedEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeds(feeds: List<FeedEntity>): List<Long>

    @Update
    suspend fun updateFeed(feed: FeedEntity)

    @Delete
    suspend fun deleteFeed(feed: FeedEntity)

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteFeedById(id: Long)

    @Query("SELECT COUNT(*) FROM feeds")
    suspend fun getFeedCount(): Int
}
