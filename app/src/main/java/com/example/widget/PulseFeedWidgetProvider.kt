package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.repository.FeedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PulseFeedWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            CoroutineScope(Dispatchers.IO).launch {
                val repo = FeedRepository(context)
                repo.syncAllFeeds()
                triggerUpdate(context)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.pulsefeed.ACTION_REFRESH_WIDGET"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ dynamic responsive layout sizing:
                // Allows launcher to fluidly transition cards from 1 column up to 4 columns as widget grows/shrinks
                val viewMapping = mapOf(
                    SizeF(140f, 100f) to buildCardGridViews(context, appWidgetId, R.layout.widget_pulsefeed_1col),
                    SizeF(260f, 100f) to buildCardGridViews(context, appWidgetId, R.layout.widget_pulsefeed_2col),
                    SizeF(480f, 100f) to buildCardGridViews(context, appWidgetId, R.layout.widget_pulsefeed_3col),
                    SizeF(680f, 100f) to buildCardGridViews(context, appWidgetId, R.layout.widget_pulsefeed_4col)
                )
                appWidgetManager.updateAppWidget(appWidgetId, RemoteViews(viewMapping))
            } else {
                // Fallback for pre-Android 12 based on minWidth options
                val layoutRes = when {
                    minWidth >= 680 -> R.layout.widget_pulsefeed_4col
                    minWidth >= 480 -> R.layout.widget_pulsefeed_3col
                    minWidth >= 260 -> R.layout.widget_pulsefeed_2col
                    else -> R.layout.widget_pulsefeed_1col
                }
                val views = buildCardGridViews(context, appWidgetId, layoutRes)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun buildCardGridViews(context: Context, appWidgetId: Int, layoutId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, layoutId)

            // Header click opens app
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, appPendingIntent)

            // Refresh button click
            val refreshIntent = Intent(context, PulseFeedWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent)

            // Set up RemoteViewsService intent for grid of cards
            val serviceIntent = Intent(context, PulseFeedWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_article_grid, serviceIntent)
            views.setEmptyView(R.id.widget_article_grid, R.id.widget_empty_view)

            // Card item click template
            val clickIntent = Intent(context, MainActivity::class.java)
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                2,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_article_grid, clickPendingIntent)

            return views
        }

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, PulseFeedWidgetProvider::class.java)
            )
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_article_grid)
        }
    }
}
