package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddFeedDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onAddFeed: (url: String, category: String) -> Unit,
    isSubmitting: Boolean
) {
    if (!isOpen) return

    var url by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Technology") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presets = listOf(
        "The Verge" to "https://www.theverge.com/rss/index.xml",
        "Ars Technica" to "https://feeds.arstechnica.com/arstechnica/index",
        "NASA News" to "https://www.nasa.gov/rss/dyn/breaking_news.rss",
        "BBC News" to "http://feeds.bbci.co.uk/news/world/rss.xml",
        "Wired" to "https://www.wired.com/feed/rss"
    )

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RssFeed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Subscribe to RSS Feed", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter any RSS 2.0 or Atom feed URL to download and cache its articles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        errorMessage = null
                    },
                    label = { Text("Feed URL") },
                    placeholder = { Text("https://example.com/rss.xml") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feed_url_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category / Folder") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feed_category_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Or tap a popular feed:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presets.forEach { (name, presetUrl) ->
                        SuggestionChip(
                            onClick = {
                                url = presetUrl
                                category = if (name.contains("NASA") || name.contains("Science")) "Science"
                                else if (name.contains("BBC")) "News"
                                else "Technology"
                            },
                            label = { Text(text = name, fontSize = 11.sp) },
                            modifier = Modifier.testTag("preset_${name.replace(" ", "_")}")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isBlank()) {
                        errorMessage = "Please enter a valid RSS URL"
                    } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        errorMessage = "URL must start with http:// or https://"
                    } else {
                        onAddFeed(url, category)
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.testTag("confirm_add_feed_btn")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Subscribe")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
