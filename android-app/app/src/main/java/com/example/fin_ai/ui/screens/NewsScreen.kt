package com.example.fin_ai.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fin_ai.data.NewsArticleDto
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.viewmodel.NewsCategory
import com.example.fin_ai.ui.viewmodel.NewsViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun NewsScreen(viewModel: NewsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Finance News",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.weight(1f)
                )
                if (state.refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = VioletLight)
                } else {
                    IconButton(onClick = viewModel::refresh, enabled = !state.loading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh news", tint = VioletLight)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FinTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = "Search e.g. Nifty, gold, RBI",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(enabled = searchText.isNotBlank()) { viewModel.search(searchText) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search news", tint = VioletLight)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NewsCategory.entries.forEach { cat ->
                    CategoryChip(cat.label, selected = state.category == cat) {
                        searchText = ""
                        viewModel.selectCategory(cat)
                    }
                }
            }
        }
        state.searchQuery?.let { q ->
            item {
                Text(
                    "Results for \"$q\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        state.notice?.let { item { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } }

        when {
            state.loading -> item { Notice("Fetching the latest headlines...") }
            state.error != null -> item { Notice(state.error!!, isError = true, onRetry = viewModel::refresh) }
            else -> items(state.articles, key = { it.url }) { article ->
                ArticleCard(article) { runCatching { uriHandler.openUri(article.url) } }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) VioletLight.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceContainerLowest
            )
            .border(
                1.dp,
                if (selected) VioletLight.copy(alpha = 0.5f) else OutlineViolet,
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = if (selected) VioletLight else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ArticleCard(article: NewsArticleDto, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen)
    ) {
        if (article.image.isNotBlank()) {
            AsyncImage(
                model = article.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )
        }
        Column(Modifier.padding(16.dp)) {
            Text(
                listOfNotNull(article.source.takeIf { it.isNotBlank() }, relativeTime(article.publishedAt))
                    .joinToString("  ·  "),
                color = VioletLight,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                article.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (article.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    article.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** "2026-10-07T10:15:00Z" -> "2 hours ago"; null when the date can't be read. */
private fun relativeTime(iso: String): String? {
    if (iso.length < 19) return null
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val millis = parser.parse(iso.substring(0, 19))!!.time
        DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }.getOrNull()
}
