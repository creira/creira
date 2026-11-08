package com.example.ui.components

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Article
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingView(
    articles: List<Article>,
    currentArticle: Article,
    onArticleChanged: (Article) -> Unit,
    onClose: () -> Unit,
    onToggleBookmark: (Article) -> Unit,
    onToggleRead: (Article) -> Unit,
    onProgressUpdate: (articleId: Long, progress: Float) -> Unit,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (showBackButton) {
        BackHandler(onBack = onClose)
    }

    var fontScale by remember { mutableFloatStateOf(1.0f) }

    // Resolve articles list to at least contain currentArticle
    val displayArticles = remember(articles, currentArticle) {
        if (articles.isEmpty()) listOf(currentArticle)
        else if (articles.none { it.id == currentArticle.id }) listOf(currentArticle) + articles
        else articles
    }

    val initialIndex = remember(displayArticles, currentArticle.id) {
        displayArticles.indexOfFirst { it.id == currentArticle.id }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { displayArticles.size }
    )

    // Synchronize pager swipe with outside current article
    LaunchedEffect(pagerState.currentPage) {
        val articleAtPage = displayArticles.getOrNull(pagerState.currentPage)
        if (articleAtPage != null && articleAtPage.id != currentArticle.id) {
            onArticleChanged(articleAtPage)
        }
    }

    // Synchronize external article selection to pager
    LaunchedEffect(currentArticle.id) {
        val targetIndex = displayArticles.indexOfFirst { it.id == currentArticle.id }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    val activeDisplayArticle = displayArticles.getOrNull(pagerState.currentPage) ?: currentArticle
    val currentIndex = pagerState.currentPage
    val nextArticle = displayArticles.getOrNull(currentIndex + 1)
    val prevArticle = displayArticles.getOrNull(currentIndex - 1)

    var isArticleScrollingVertically by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeDisplayArticle.feedTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${activeDisplayArticle.estimatedReadMinutes}m read • Article ${currentIndex + 1} of ${displayArticles.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("reading_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Feed"
                            )
                        }
                    }
                },
                actions = {
                    // Font scaling
                    IconButton(
                        onClick = {
                            fontScale = if (fontScale >= 1.3f) 0.9f else fontScale + 0.15f
                        },
                        modifier = Modifier.testTag("font_scale_button")
                    ) {
                        Text(
                            text = "A±",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Read toggle
                    IconButton(
                        onClick = { onToggleRead(activeDisplayArticle) },
                        modifier = Modifier.testTag("reading_toggle_read")
                    ) {
                        Icon(
                            imageVector = if (activeDisplayArticle.isRead) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = if (activeDisplayArticle.isRead) "Mark Unread" else "Mark Read",
                            tint = if (activeDisplayArticle.isRead) Color(0xFF48BB78) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Bookmark toggle
                    IconButton(
                        onClick = { onToggleBookmark(activeDisplayArticle) },
                        modifier = Modifier.testTag("reading_toggle_bookmark")
                    ) {
                        Icon(
                            imageVector = if (activeDisplayArticle.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (activeDisplayArticle.isBookmarked) "Remove Bookmark" else "Bookmark",
                            tint = if (activeDisplayArticle.isBookmarked) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share
                    IconButton(
                        onClick = { shareArticle(context, activeDisplayArticle) },
                        modifier = Modifier.testTag("reading_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }

                    // Open in Browser
                    if (activeDisplayArticle.link.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(activeDisplayArticle.link))
                                context.startActivity(browserIntent)
                            },
                            modifier = Modifier.testTag("open_browser_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "Open in browser"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // "Up Next" bottom banner
            UpNextBottomBanner(
                nextArticle = nextArticle,
                prevArticle = prevArticle,
                onNextClick = {
                    if (nextArticle != null) {
                        scope.launch {
                            pagerState.animateScrollToPage(currentIndex + 1)
                        }
                    }
                },
                onPrevClick = {
                    if (prevArticle != null) {
                        scope.launch {
                            pagerState.animateScrollToPage(currentIndex - 1)
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        // Swipeable HorizontalPager for fluid previous/next article gesture navigation
        // userScrollEnabled is disabled while vertically scrolling to prevent diagonal thumb drift from triggering page swipes
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isArticleScrollingVertically,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("articles_horizontal_pager")
        ) { page ->
            val pageArticle = displayArticles.getOrNull(page) ?: currentArticle
            ArticleScrollableContent(
                article = pageArticle,
                fontScale = fontScale,
                onScrollingChanged = { isScrolling ->
                    isArticleScrollingVertically = isScrolling
                },
                onProgressUpdate = { progress ->
                    onProgressUpdate(pageArticle.id, progress)
                }
            )
        }
    }
}

@Composable
private fun ArticleScrollableContent(
    article: Article,
    fontScale: Float,
    onScrollingChanged: (Boolean) -> Unit,
    onProgressUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberSaveable(saver = ScrollState.Saver, key = "article_scroll_${article.id}") {
        ScrollState(0)
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        onScrollingChanged(scrollState.isScrollInProgress)
    }

    val currentProgress by remember {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
            } else {
                article.readProgress
            }
        }
    }

    var lastReportedProgress by remember(article.id) { mutableFloatStateOf(article.readProgress) }

    LaunchedEffect(currentProgress, scrollState.isScrollInProgress) {
        // Report progress at discrete milestones or when scrolling rests
        // Prevents continuous Room DB write storms and flow recomposition churn during scroll
        val shouldReport = (currentProgress >= 0.88f && lastReportedProgress < 0.88f) ||
                (!scrollState.isScrollInProgress && (currentProgress - lastReportedProgress) >= 0.05f) ||
                (currentProgress >= 1f && lastReportedProgress < 1f)
        if (shouldReport) {
            lastReportedProgress = currentProgress
            onProgressUpdate(currentProgress)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Sticky Live Reading Progress Bar
        LinearProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Feed & Offline Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = article.feedTitle.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFF48BB78)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Offline Cached",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Article Title
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = (23 * fontScale).sp,
                    lineHeight = (30 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Author, Date & Reading time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (article.author.isNotBlank()) {
                    Text(
                        text = "By ${article.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (article.pubDate.isNotBlank()) {
                    Text(
                        text = article.pubDate.take(25),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Featured Image if present
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(article.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Article Body Text (Formatted with comfortable reading metrics)
            val paragraphs = if (article.plainText.isNotBlank()) {
                article.plainText.split("\n\n").filter { it.isNotBlank() }
            } else if (article.description.isNotBlank()) {
                article.description.split("\n\n").filter { it.isNotBlank() }
            } else {
                listOf(article.title)
            }

            for (paragraph in paragraphs) {
                Text(
                    text = paragraph.trim(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (16.5f * fontScale).sp,
                        lineHeight = (26 * fontScale).sp,
                        fontFamily = FontFamily.Serif
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Completed reading footer card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (article.isRead) Icons.Filled.CheckCircle else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (article.isRead) Color(0xFF48BB78) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (article.isRead) "Article completed!" else "Keep reading to finish",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Swipe left to read next, or swipe right for previous.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun UpNextBottomBanner(
    nextArticle: Article?,
    prevArticle: Article?,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isSkinnyHeight = configuration.screenHeightDp < 500 || isLandscape

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = if (isLandscape) 16.dp else 10.dp,
                vertical = if (isSkinnyHeight) 2.dp else 4.dp
            )
            .testTag("up_next_banner"),
        shape = RoundedCornerShape(if (isSkinnyHeight) 10.dp else 14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh ?: MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        if (nextArticle != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNextClick)
                    .padding(
                        horizontal = if (isLandscape) 10.dp else 12.dp,
                        vertical = if (isSkinnyHeight) 3.dp else 5.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button if available
                if (prevArticle != null) {
                    IconButton(
                        onClick = onPrevClick,
                        modifier = Modifier
                            .size(if (isSkinnyHeight) 28.dp else 32.dp)
                            .testTag("up_next_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous article",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(if (isSkinnyHeight) 15.dp else 16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Next article preview info
                if (isSkinnyHeight) {
                    // Single ultra-compact row in landscape / short screen
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "NEXT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = nextArticle.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• ${nextArticle.estimatedReadMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                } else {
                    // Slim 2-line preview in portrait
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "UP NEXT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${nextArticle.feedTitle} • ${nextArticle.estimatedReadMinutes}m read",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = nextArticle.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Next Button
                FilledIconButton(
                    onClick = onNextClick,
                    modifier = Modifier
                        .size(if (isSkinnyHeight) 28.dp else 32.dp)
                        .testTag("up_next_advance_btn"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to next article",
                        modifier = Modifier.size(if (isSkinnyHeight) 15.dp else 16.dp)
                    )
                }
            }
        } else {
            // End of feed state
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isLandscape) 10.dp else 12.dp,
                        vertical = if (isSkinnyHeight) 3.dp else 5.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (prevArticle != null) {
                    IconButton(
                        onClick = onPrevClick,
                        modifier = Modifier
                            .size(if (isSkinnyHeight) 28.dp else 32.dp)
                            .testTag("up_next_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous article",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(if (isSkinnyHeight) 15.dp else 16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF48BB78),
                    modifier = Modifier.size(if (isSkinnyHeight) 16.dp else 18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isSkinnyHeight) {
                    Text(
                        text = "All caught up! Swipe right to revisit articles.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "You're all caught up!",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Swipe right to review previous articles.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
