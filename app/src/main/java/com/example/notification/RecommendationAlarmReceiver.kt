package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.repository.FeedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecommendationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        CoroutineScope(Dispatchers.IO).launch {
            val repo = FeedRepository(context)
            val article = repo.getRandomUnreadArticle() ?: repo.getRecentArticles(1).firstOrNull()
            if (article != null) {
                RecommendationNotifier.showRecommendation(context, article)
            }
        }
    }
}
