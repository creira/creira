package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Article
import com.example.data.model.ArticleFilter
import com.example.data.model.Feed
import com.example.data.model.OpmlOutline
import com.example.data.model.ThemeMode
import com.example.notification.RecommendationNotifier
import com.example.repository.FeedRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PulseFeedViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository = FeedRepository(context)
    private val prefs = context.getSharedPreferences("pulsefeed_prefs", Context.MODE_PRIVATE)

    val feeds: StateFlow<List<Feed>> = repository.feedsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFeed = MutableStateFlow<Feed?>(null)
    val selectedFeed: StateFlow<Feed?> = _selectedFeed.asStateFlow()

    private val _filter = MutableStateFlow(ArticleFilter.ALL)
    val filter: StateFlow<ArticleFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val articles: StateFlow<List<Article>> = combine(
        _selectedFeed,
        _filter,
        _searchQuery
    ) { feed, filter, query ->
        Triple(feed, filter, query)
    }.flatMapLatest { (feed, filter, query) ->
        repository.getArticlesFlow(feed?.id, filter, query)
    }.map { list ->
        list.sortedWith(
            compareByDescending<Article> {
                if (it.pubDateTimestamp > 0L) it.pubDateTimestamp else it.cachedAt
            }.thenByDescending { it.id }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun showAllArticles() {
        _selectedFeed.value = null
        _filter.value = ArticleFilter.ALL
        _searchQuery.value = ""
    }

    private val _activeArticle = MutableStateFlow<Article?>(null)
    val activeArticle: StateFlow<Article?> = _activeArticle.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _themeMode = MutableStateFlow(loadThemePreference())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(loadDynamicColorPreference())
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    init {
        RecommendationNotifier.createNotificationChannel(context)
        RecommendationNotifier.scheduleDailyRecommendation(context)

        viewModelScope.launch {
            repository.initializePresetFeedsIfNeeded()
        }
    }

    private fun loadThemePreference(): ThemeMode {
        val saved = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(saved ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    private fun loadDynamicColorPreference(): Boolean {
        return prefs.getBoolean("dynamic_color", true)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs.edit().putBoolean("dynamic_color", enabled).apply()
    }

    fun selectFeed(feed: Feed?) {
        _selectedFeed.value = feed
    }

    fun setFilter(filter: ArticleFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openArticle(article: Article) {
        _activeArticle.value = article
    }

    fun openArticleById(id: Long) {
        viewModelScope.launch {
            val article = repository.getArticleById(id)
            if (article != null) {
                _activeArticle.value = article
            }
        }
    }

    fun closeActiveArticle() {
        _activeArticle.value = null
    }

    fun updateReadingProgress(articleId: Long, progress: Float) {
        viewModelScope.launch {
            repository.updateReadingProgress(articleId, progress)
            if (_activeArticle.value?.id == articleId) {
                _activeArticle.value = _activeArticle.value?.copy(
                    readProgress = progress,
                    isRead = progress >= 0.88f
                )
            }
        }
    }

    fun toggleArticleRead(article: Article) {
        viewModelScope.launch {
            val newReadState = !article.isRead
            repository.setArticleRead(article.id, newReadState)
            if (_activeArticle.value?.id == article.id) {
                _activeArticle.value = _activeArticle.value?.copy(
                    isRead = newReadState,
                    readProgress = if (newReadState) 1f else 0f
                )
            }
        }
    }

    fun toggleArticleBookmark(article: Article) {
        viewModelScope.launch {
            val newBookmarkState = !article.isBookmarked
            repository.toggleBookmark(article.id, newBookmarkState)
            if (_activeArticle.value?.id == article.id) {
                _activeArticle.value = _activeArticle.value?.copy(
                    isBookmarked = newBookmarkState
                )
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead(_selectedFeed.value?.id)
            _statusMessage.value = "All articles marked as read"
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "Updating all feeds..."
            val result = repository.syncAllFeeds()
            _isSyncing.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Updated ${result.getOrNull()} feeds"
            } else {
                _statusMessage.value = "Feed update completed"
            }
        }
    }

    fun syncCurrentFeed() {
        val current = _selectedFeed.value
        if (current == null) {
            syncAll()
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "Updating ${current.title}..."
            val result = repository.syncFeed(current.id)
            _isSyncing.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Updated ${current.title}"
            } else {
                _statusMessage.value = "Failed to update feed"
            }
        }
    }

    fun addFeed(url: String, category: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.addFeed(url.trim(), category.trim())
            _isSyncing.value = false
            if (result.isSuccess) {
                val feed = result.getOrThrow()
                _selectedFeed.value = feed
                _statusMessage.value = "Subscribed to ${feed.title}"
                onComplete(true, "Subscribed to ${feed.title}")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to add feed"
                onComplete(false, error)
            }
        }
    }

    fun importOpml(opmlContent: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val added = repository.importOpml(opmlContent)
            _isSyncing.value = false
            _statusMessage.value = "Imported $added feeds from OPML"
            onComplete(added)
        }
    }

    fun importPresetOutlines(outlines: List<OpmlOutline>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val added = repository.importOutlines(outlines)
            _isSyncing.value = false
            _statusMessage.value = "Imported $added curated feeds"
            onComplete(added)
        }
    }

    fun deleteFeed(feedId: Long) {
        viewModelScope.launch {
            repository.deleteFeed(feedId)
            if (_selectedFeed.value?.id == feedId) {
                _selectedFeed.value = null
            }
            _statusMessage.value = "Feed removed"
        }
    }

    suspend fun exportOpml(): String {
        return repository.exportOpml()
    }

    fun triggerDailyRecommendationNow() {
        RecommendationNotifier.triggerRecommendationNow(context)
        _statusMessage.value = "Recommendation notification sent!"
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
