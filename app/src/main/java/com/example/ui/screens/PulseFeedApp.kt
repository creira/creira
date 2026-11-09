package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import android.content.res.Configuration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Article
import com.example.data.model.ArticleFilter
import com.example.ui.PulseFeedViewModel
import com.example.ui.components.AddFeedDialog
import com.example.ui.components.ArticleCard
import com.example.ui.components.FeedDrawerContent
import com.example.ui.components.OpmlDialog
import com.example.ui.components.ReadingView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseFeedApp(
    viewModel: PulseFeedViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val feeds by viewModel.feeds.collectAsState()
    val selectedFeed by viewModel.selectedFeed.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val articles by viewModel.articles.collectAsState()
    val activeArticle by viewModel.activeArticle.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showOpmlDialog by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isLeftMenuExpanded by remember { mutableStateOf(false) }

    // Notification permission request for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Daily recommendations enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val isWideScreen = (isLandscape && maxWidth >= 680.dp) || maxWidth >= 840.dp
        val isDualPaneReading = maxWidth >= 960.dp && isLandscape

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isWideScreen && activeArticle == null,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(320.dp)
                ) {
                    FeedDrawerContent(
                        feeds = feeds,
                        selectedFeed = selectedFeed,
                        currentFilter = filter,
                        themeMode = themeMode,
                        onSelectFeed = { feed ->
                            viewModel.selectFeed(feed)
                            scope.launch { drawerState.close() }
                        },
                        onSelectFilter = { newFilter ->
                            viewModel.setFilter(newFilter)
                            scope.launch { drawerState.close() }
                        },
                        onAddFeedClick = {
                            scope.launch { drawerState.close() }
                            showAddDialog = true
                        },
                        onOpmlClick = {
                            scope.launch { drawerState.close() }
                            showOpmlDialog = true
                        },
                        onDeleteFeed = { feedId ->
                            viewModel.deleteFeed(feedId)
                        },
                        onToggleTheme = { newTheme ->
                            viewModel.setThemeMode(newTheme)
                        },
                        onSendRecommendationTest = {
                            viewModel.triggerDailyRecommendationNow()
                            scope.launch { drawerState.close() }
                        },
                        dynamicColor = dynamicColor,
                        onToggleDynamicColor = { viewModel.setDynamicColor(it) }
                    )
                }
            }
        ) {
            if (isWideScreen) {
                // Adaptive Layout for Foldables (unfolded), Tablets, and Horizontal phones with collapsible left menu
                Row(modifier = Modifier.fillMaxSize()) {
                    // Collapsible Left Pane: Navigation Drawer in horizontal mode
                    AnimatedVisibility(
                        visible = isLeftMenuExpanded,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        Row(modifier = Modifier.fillMaxHeight()) {
                            Box(
                                modifier = Modifier
                                    .width(320.dp)
                                    .fillMaxHeight()
                            ) {
                                FeedDrawerContent(
                                    feeds = feeds,
                                    selectedFeed = selectedFeed,
                                    currentFilter = filter,
                                    themeMode = themeMode,
                                    onSelectFeed = { viewModel.selectFeed(it) },
                                    onSelectFilter = { viewModel.setFilter(it) },
                                    onAddFeedClick = { showAddDialog = true },
                                    onOpmlClick = { showOpmlDialog = true },
                                    onDeleteFeed = { viewModel.deleteFeed(it) },
                                    onToggleTheme = { viewModel.setThemeMode(it) },
                                    onSendRecommendationTest = { viewModel.triggerDailyRecommendationNow() },
                                    onCollapse = { isLeftMenuExpanded = false },
                                    dynamicColor = dynamicColor,
                                    onToggleDynamicColor = { viewModel.setDynamicColor(it) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )
                        }
                    }

                    // Content Area: Either side-by-side (if very wide landscape) or full view
                    if (isDualPaneReading) {
                        // Very wide landscape: Split view
                        Box(
                            modifier = Modifier
                                .weight(if (activeArticle != null) 1f else 1.5f)
                                .fillMaxHeight()
                        ) {
                            ArticlesListScreen(
                                articles = articles,
                                selectedFeedTitle = selectedFeed?.title,
                                currentFilter = filter,
                                isSyncing = isSyncing,
                                searchQuery = searchQuery,
                                isSearchExpanded = isSearchExpanded,
                                onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onOpenMenu = { scope.launch { drawerState.open() } },
                                onRefresh = { viewModel.syncCurrentFeed() },
                                onMarkAllRead = { viewModel.markAllAsRead() },
                                onArticleClick = { viewModel.openArticle(it) },
                                onToggleBookmark = { viewModel.toggleArticleBookmark(it) },
                                onToggleRead = { viewModel.toggleArticleRead(it) },
                                onFilterChange = { viewModel.setFilter(it) },
                                onClearFeedFilter = { viewModel.selectFeed(null) },
                                onAddFeedClick = { showAddDialog = true },
                                onOpmlClick = { showOpmlDialog = true },
                                snackbarHostState = snackbarHostState,
                                showMenuButton = false,
                                onToggleLeftMenu = { isLeftMenuExpanded = !isLeftMenuExpanded },
                                isLeftMenuExpanded = isLeftMenuExpanded,
                                maxColumns = if (activeArticle != null) 2 else null
                            )
                        }

                        if (activeArticle != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1.4f)
                                    .fillMaxHeight()
                            ) {
                                androidx.compose.runtime.key(activeArticle?.id) {
                                    ReadingView(
                                        articles = articles,
                                        currentArticle = activeArticle!!,
                                        onArticleChanged = { viewModel.openArticle(it) },
                                        onClose = { viewModel.closeActiveArticle() },
                                        onToggleBookmark = { viewModel.toggleArticleBookmark(it) },
                                        onToggleRead = { viewModel.toggleArticleRead(it) },
                                        onProgressUpdate = { id, progress ->
                                            viewModel.updateReadingProgress(id, progress)
                                        },
                                        showBackButton = true
                                    )
                                }
                            }
                        }
                    } else {
                        // Standard landscape or Foldable: animated transition between list and reader
                        AnimatedContent(
                            targetState = activeArticle != null,
                            transitionSpec = {
                                if (targetState) {
                                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> -width / 3 } + fadeOut()
                                    )
                                } else {
                                    (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> width } + fadeOut()
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            label = "WideArticleNavigation"
                        ) { hasActiveArticle ->
                            if (hasActiveArticle && activeArticle != null) {
                                androidx.compose.runtime.key(activeArticle?.id) {
                                    ReadingView(
                                        articles = articles,
                                        currentArticle = activeArticle!!,
                                        onArticleChanged = { viewModel.openArticle(it) },
                                        onClose = { viewModel.closeActiveArticle() },
                                        onToggleBookmark = { viewModel.toggleArticleBookmark(it) },
                                        onToggleRead = { viewModel.toggleArticleRead(it) },
                                        onProgressUpdate = { id, progress ->
                                            viewModel.updateReadingProgress(id, progress)
                                        },
                                        showBackButton = true
                                    )
                                }
                            } else {
                                ArticlesListScreen(
                                    articles = articles,
                                    selectedFeedTitle = selectedFeed?.title,
                                    currentFilter = filter,
                                    isSyncing = isSyncing,
                                    searchQuery = searchQuery,
                                    isSearchExpanded = isSearchExpanded,
                                    onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onOpenMenu = { scope.launch { drawerState.open() } },
                                    onRefresh = { viewModel.syncCurrentFeed() },
                                    onMarkAllRead = { viewModel.markAllAsRead() },
                                    onArticleClick = { viewModel.openArticle(it) },
                                    onToggleBookmark = { viewModel.toggleArticleBookmark(it) },
                                    onToggleRead = { viewModel.toggleArticleRead(it) },
                                    onFilterChange = { viewModel.setFilter(it) },
                                    onClearFeedFilter = { viewModel.selectFeed(null) },
                                    onAddFeedClick = { showAddDialog = true },
                                    onOpmlClick = { showOpmlDialog = true },
                                    snackbarHostState = snackbarHostState,
                                    showMenuButton = false,
                                    onToggleLeftMenu = { isLeftMenuExpanded = !isLeftMenuExpanded },
                                    isLeftMenuExpanded = isLeftMenuExpanded
                                )
                            }
                        }
                    }
                }
            } else {
                // Single Pane Layout for Standard & Folded Screens with smooth animated transitions
                AnimatedContent(
                    targetState = activeArticle != null,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width / 3 } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "ArticleNavigationTransition"
                ) { hasActiveArticle ->
                    if (hasActiveArticle && activeArticle != null) {
                        androidx.compose.runtime.key(activeArticle?.id) {
                            ReadingView(
                                articles = articles,
                                currentArticle = activeArticle!!,
                                onArticleChanged = { viewModel.openArticle(it) },
                                onClose = { viewModel.closeActiveArticle() },
                                onToggleBookmark = { viewModel.toggleArticleBookmark(it) },
                                onToggleRead = { viewModel.toggleArticleRead(it) },
                                onProgressUpdate = { id, progress ->
                                    viewModel.updateReadingProgress(id, progress)
                                },
                                showBackButton = true
                            )
                        }
                    } else {
                        ArticlesListScreen(
                            articles = articles,
                            selectedFeedTitle = selectedFeed?.title,
                            currentFilter = filter,
                            isSyncing = isSyncing,
                            searchQuery = searchQuery,
                            isSearchExpanded = isSearchExpanded,
                            onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onOpenMenu = { scope.launch { drawerState.open() } },
                            onRefresh = { viewModel.syncCurrentFeed() },
                            onMarkAllRead = { viewModel.markAllAsRead() },
                            onArticleClick = { viewModel.openArticle(it) },
                            onToggleBookmark = { viewModel.toggleArticleBookmark(it) },
                            onToggleRead = { viewModel.toggleArticleRead(it) },
                            onFilterChange = { viewModel.setFilter(it) },
                            onClearFeedFilter = { viewModel.selectFeed(null) },
                            onAddFeedClick = { showAddDialog = true },
                            onOpmlClick = { showOpmlDialog = true },
                            snackbarHostState = snackbarHostState,
                            showMenuButton = true
                        )
                    }
                }
            }
        }
    }

    // Add Feed Dialog
    AddFeedDialog(
        isOpen = showAddDialog,
        onDismiss = { showAddDialog = false },
        onAddFeed = { url, category ->
            viewModel.addFeed(url, category) { success, msg ->
                if (success) {
                    showAddDialog = false
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        },
        isSubmitting = isSyncing
    )

    // OPML Dialog
    OpmlDialog(
        isOpen = showOpmlDialog,
        onDismiss = { showOpmlDialog = false },
        onImportOpml = { opmlContent ->
            viewModel.importOpml(opmlContent) { count ->
                showOpmlDialog = false
                Toast.makeText(context, "Imported $count feeds from OPML", Toast.LENGTH_SHORT).show()
            }
        },
        onExportOpml = {
            viewModel.exportOpml()
        },
        isProcessing = isSyncing
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticlesListScreen(
    articles: List<Article>,
    selectedFeedTitle: String?,
    currentFilter: ArticleFilter,
    isSyncing: Boolean,
    searchQuery: String,
    isSearchExpanded: Boolean,
    onToggleSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenMenu: () -> Unit,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
    onArticleClick: (Article) -> Unit,
    onToggleBookmark: (Article) -> Unit,
    onToggleRead: (Article) -> Unit,
    onFilterChange: (ArticleFilter) -> Unit,
    onClearFeedFilter: () -> Unit,
    onAddFeedClick: () -> Unit,
    onOpmlClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    showMenuButton: Boolean,
    onToggleLeftMenu: (() -> Unit)? = null,
    isLeftMenuExpanded: Boolean = false,
    maxColumns: Int? = null
) {
    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = selectedFeedTitle ?: if (currentFilter == ArticleFilter.BOOKMARKS) "Saved Articles" else if (currentFilter == ArticleFilter.UNREAD_ONLY) "Unread Articles" else "All Articles",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${articles.size} articles ${if (selectedFeedTitle == null) "from all sources • most recent first" else "cached offline"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        if (showMenuButton) {
                            IconButton(
                                onClick = onOpenMenu,
                                modifier = Modifier.testTag("app_bar_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Feeds Menu"
                                )
                            }
                        } else if (onToggleLeftMenu != null) {
                            IconButton(
                                onClick = onToggleLeftMenu,
                                modifier = Modifier.testTag("app_bar_toggle_sidebar")
                            ) {
                                Icon(
                                    imageVector = if (isLeftMenuExpanded) Icons.Default.MenuOpen else Icons.Default.Menu,
                                    contentDescription = if (isLeftMenuExpanded) "Collapse Feeds Menu" else "Expand Feeds Menu",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        // Search button
                        IconButton(
                            onClick = onToggleSearch,
                            modifier = Modifier.testTag("app_bar_search_btn")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search Articles"
                            )
                        }

                        // Mark all as read
                        IconButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.testTag("app_bar_mark_all_read")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark All Read"
                            )
                        }

                        // Sync button
                        IconButton(
                            onClick = onRefresh,
                            enabled = !isSyncing,
                            modifier = Modifier.testTag("app_bar_sync_btn")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync Feeds"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Expandable Search Bar
                AnimatedVisibility(visible = isSearchExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Search article titles, contents...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("article_search_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Filter chips row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedFeedTitle != null) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = onClearFeedFilter,
                                label = { Text("Feed: $selectedFeedTitle ✕") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("chip_clear_feed_filter")
                            )
                        }
                    }

                    item {
                        FilterChip(
                            selected = currentFilter == ArticleFilter.ALL && selectedFeedTitle == null,
                            onClick = {
                                onClearFeedFilter()
                                onFilterChange(ArticleFilter.ALL)
                            },
                            label = { Text("All Sources") },
                            modifier = Modifier.testTag("chip_filter_all")
                        )
                    }

                    item {
                        FilterChip(
                            selected = currentFilter == ArticleFilter.UNREAD_ONLY,
                            onClick = { onFilterChange(ArticleFilter.UNREAD_ONLY) },
                            label = { Text("Unread") },
                            modifier = Modifier.testTag("chip_filter_unread")
                        )
                    }

                    item {
                        FilterChip(
                            selected = currentFilter == ArticleFilter.BOOKMARKS,
                            onClick = { onFilterChange(ArticleFilter.BOOKMARKS) },
                            label = { Text("Saved") },
                            modifier = Modifier.testTag("chip_filter_saved")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        if (articles.isEmpty()) {
            // Friendly Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching articles found"
                        else if (currentFilter == ArticleFilter.BOOKMARKS) "No saved articles yet"
                        else if (currentFilter == ArticleFilter.UNREAD_ONLY) "All caught up! Zero unread articles."
                        else "No articles available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try searching for other keywords"
                        else "Sync your feeds or import an OPML file to read articles offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("empty_refresh_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Feeds")
                        }

                        Button(
                            onClick = onOpmlClick,
                            modifier = Modifier.testTag("empty_opml_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OPML Import")
                        }
                    }
                }
            }
        } else {
            // Main Articles Feed with Dynamic Responsive Grid
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val availableWidth = maxWidth

                // Calculate columns according to screen shape & device:
                // - Standard candy bar shape in vertical mode (Galaxy S26 portrait): 1 card
                // - Screen in horizontal mode: at least 2 cards (or 3 if wide)
                // - Galaxy S Fold 8 in portrait: 2 cards
                // - Galaxy S Fold 8 in landscape: 3 cards
                // - In foldable landscape when reading an article (split view): left pane capped to 2 columns of cards!
                val baseColumns = when {
                    // Wide horizontal screen when using full width (Fold 8 unfolded landscape ~800dp+): 3 cards!
                    isLandscape && availableWidth >= 740.dp -> 3

                    // Horizontal / landscape mode on phone or medium screen: at least 2 cards!
                    isLandscape -> 2

                    // Foldable unfolded in portrait (Galaxy Fold 8 in portrait ~600-760dp): 2 cards!
                    availableWidth >= 560.dp || (!isLandscape && configuration.screenWidthDp >= 560) -> 2

                    // Standard candy bar phone in vertical mode (e.g. Galaxy S26 portrait): 1 card!
                    else -> 1
                }

                val columnCount = if (maxColumns != null) minOf(baseColumns, maxColumns) else baseColumns

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("articles_feed_list"),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = articles,
                        key = { it.id }
                    ) { article ->
                        ArticleCard(
                            article = article,
                            onClick = { onArticleClick(article) },
                            onToggleBookmark = { onToggleBookmark(article) },
                            onToggleRead = { onToggleRead(article) }
                        )
                    }
                }
            }
        }
    }
}
