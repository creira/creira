package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.remote.RssParser

/**
 * Renders a crisp source icon (favicon or custom icon) for RSS feeds and article cards.
 */
@Composable
fun SourceIcon(
    iconUrl: String? = null,
    fallbackUrl: String? = null,
    title: String = "",
    size: Dp = 18.dp,
    shapeRadius: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveUrl = remember(iconUrl, fallbackUrl) {
        when {
            !iconUrl.isNullOrBlank() -> iconUrl
            !fallbackUrl.isNullOrBlank() -> RssParser.extractFavicon(fallbackUrl)
            else -> ""
        }
    }

    if (effectiveUrl.isNotBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(effectiveUrl)
                .crossfade(true)
                .build(),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(shapeRadius))
                .background(Color.White.copy(alpha = 0.95f))
        )
    } else {
        // Fallback badge with source initial or RSS emblem
        val initial = title.trim().firstOrNull()?.uppercaseChar()?.toString()
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(shapeRadius))
                .background(Color(0xFFFF9100)),
            contentAlignment = Alignment.Center
        ) {
            if (initial != null) {
                Text(
                    text = initial,
                    fontSize = (size.value * 0.55f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RssFeed,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.65f)
                )
            }
        }
    }
}
