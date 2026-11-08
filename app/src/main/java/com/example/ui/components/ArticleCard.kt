package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Article
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArticleCard(
    article: Article,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Darkening transition when article is read
    val dimmingColor by animateColorAsState(
        targetValue = if (article.isRead) Color(0x8A000000) else Color.Transparent,
        animationSpec = tween(300),
        label = "dimmingColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .testTag("article_card_${article.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (article.isRead) 1.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (article.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background visual: Image or stylized editorial gradient
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(article.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Editorial patterned gradient placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E2846),
                                    Color(0xFF283560),
                                    Color(0xFF141A29)
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RssFeed,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.CenterEnd)
                    )
                }
            }

            // Read state darkening overlay: Darkens the card visually after reading
            if (article.isRead) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(dimmingColor)
                )
            }

            // Top Badges Row (Feed badge & Read status badge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC0D131F),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = article.feedTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // If read, display a subtle "Read" indicator pill
                if (article.isRead) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xDD1B2433),
                        contentColor = Color(0xFFA0AEC0)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Read",
                                tint = Color(0xFF48BB78),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Read",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Bottom Gradient Overlay Scrim: Headlines displayed ON the card itself at the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x88070B12),
                                Color(0xDD070B12),
                                Color(0xFA05080E)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Estimated reading time & Read progress indicator text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${article.estimatedReadMinutes} min read",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Progress percentage if in progress
                        if (article.readProgress > 0f) {
                            val percent = (article.readProgress * 100).toInt()
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = if (article.isRead) "100% complete" else "$percent% read",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (article.pubDateTimestamp > 0L) {
                            Text(
                                text = "•  ${formatRelativeDate(article.pubDateTimestamp)}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Headline ON the card itself
                    Text(
                        text = article.title,
                        color = if (article.isRead) Color(0xFFD1D5DB) else Color.White,
                        fontSize = 17.sp,
                        fontWeight = if (article.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        lineHeight = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reading progress bar at bottom of card
                    if (article.readProgress > 0f || article.isRead) {
                        LinearProgressIndicator(
                            progress = { if (article.isRead) 1.0f else article.readProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color(0x33FFFFFF)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Quick Actions Row (Bookmark, Share, Mark Read/Unread)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (article.author.isNotBlank()) "By ${article.author}" else "Tap to read full article",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Read / Unread toggle
                            IconButton(
                                onClick = onToggleRead,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("read_toggle_${article.id}")
                            ) {
                                Icon(
                                    imageVector = if (article.isRead) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                    contentDescription = if (article.isRead) "Mark unread" else "Mark read",
                                    tint = if (article.isRead) Color(0xFF48BB78) else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Bookmark toggle
                            IconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("bookmark_button_${article.id}")
                            ) {
                                Icon(
                                    imageVector = if (article.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = if (article.isBookmarked) "Remove bookmark" else "Bookmark article",
                                    tint = if (article.isBookmarked) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Share Intent
                            IconButton(
                                onClick = { shareArticle(context, article) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("share_button_${article.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share article",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun shareArticle(context: Context, article: Article) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, article.title)
        val shareBody = buildString {
            appendLine(article.title)
            if (article.link.isNotBlank()) {
                appendLine(article.link)
            }
            if (article.description.isNotBlank()) {
                appendLine()
                appendLine(article.description.take(280))
            }
            appendLine()
            append("Shared via PulseFeed RSS")
        }
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }
    val chooser = Intent.createChooser(sendIntent, "Share article via")
    context.startActivity(chooser)
}

private fun formatRelativeDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 7 -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}
