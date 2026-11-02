package com.example.fin_ai.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.fin_ai.data.LedgerRepository
import com.example.fin_ai.insights.Wrapped
import com.example.fin_ai.insights.WrappedCard
import com.example.fin_ai.ui.theme.VioletLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val THEMES = listOf(
    listOf(Color(0xFF7C3AED), Color(0xFF22D3EE)),
    listOf(Color(0xFFF43F5E), Color(0xFF7C3AED)),
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    listOf(Color(0xFF10B981), Color(0xFF0EA5E9)),
    listOf(Color(0xFFEC4899), Color(0xFFF97316)),
    listOf(Color(0xFF14B8A6), Color(0xFF6366F1)),
    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
)

@Composable
fun WrappedScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val all by LedgerRepository.transactions.collectAsState()
    LaunchedEffect(Unit) { LedgerRepository.ensureLoaded() }
    var monthsAgo by remember { mutableIntStateOf(0) }
    val cards = remember(all, monthsAgo) { Wrapped.cards(all, monthsAgo) }
    val pager = rememberPagerState(pageCount = { cards.size })
    val layer = rememberGraphicsLayer()

    LaunchedEffect(monthsAgo) { pager.scrollToPage(0) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
        BackRow("Wrapped", onBack)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("This month", selected = monthsAgo == 0) { monthsAgo = 0 }
            Chip(com.example.fin_ai.ui.util.Fmt.monthName(1), selected = monthsAgo == 1) { monthsAgo = 1 }
            Chip(com.example.fin_ai.ui.util.Fmt.monthName(2), selected = monthsAgo == 2) { monthsAgo = 2 }
        }
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }
        ) {
            HorizontalPager(state = pager, pageSpacing = 12.dp) { page -> StoryCard(cards[page]) }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(cards.size) { i ->
                    Box(
                        Modifier
                            .height(6.dp)
                            .width(if (i == pager.currentPage) 22.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i == pager.currentPage) VioletLight else MaterialTheme.colorScheme.surfaceContainer)
                    )
                }
            }
            Chip("Share", selected = true) {
                scope.launch {
                    val bitmap = layer.toImageBitmap().asAndroidBitmap()
                    shareImage(context, bitmap)
                }
            }
        }
    }
}

@Composable
private fun StoryCard(card: WrappedCard) {
    val colors = THEMES[card.theme % THEMES.size]
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(colors))
            .padding(28.dp)
    ) {
        Text("FinAI ⚡", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopStart))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(card.emoji, fontSize = 64.sp)
            Spacer(Modifier.height(18.dp))
            Text(card.kicker.uppercase(), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            Text(card.headline, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 44.sp)
            Spacer(Modifier.height(14.dp))
            Text(card.detail, color = Color.White.copy(alpha = 0.92f), fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
        }
    }
}

/** Saves the card to the app cache and opens the system share sheet. */
private suspend fun shareImage(context: Context, bitmap: Bitmap) {
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        File(dir, "finai-wrapped.png").also { f -> f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "My month in money, by FinAI ⚡")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share your Wrapped").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
