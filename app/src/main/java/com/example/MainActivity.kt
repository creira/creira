package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.PulseFeedViewModel
import com.example.ui.screens.PulseFeedApp
import com.example.ui.theme.PulseFeedTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PulseFeedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val dynamicColor by viewModel.dynamicColor.collectAsState()

            PulseFeedTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PulseFeedApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val rawExtra = intent.extras?.get("OPEN_ARTICLE_ID")
        var openArticleId = when (rawExtra) {
            is Long -> rawExtra
            is Int -> rawExtra.toLong()
            is String -> rawExtra.toLongOrNull() ?: -1L
            else -> -1L
        }
        if (openArticleId <= 0L && intent.data != null) {
            openArticleId = intent.data?.lastPathSegment?.toLongOrNull() ?: -1L
        }
        if (openArticleId > 0) {
            viewModel.openArticleById(openArticleId)
            intent.removeExtra("OPEN_ARTICLE_ID")
            intent.data = null
        }
    }
}
