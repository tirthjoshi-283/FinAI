package com.example.fin_ai.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fin_ai.data.InsightDto
import com.example.fin_ai.data.InsightsData
import com.example.fin_ai.ui.anim.CountUpText
import com.example.fin_ai.ui.anim.ProgressRing
import com.example.fin_ai.ui.anim.appear
import com.example.fin_ai.ui.theme.AccentCyan
import com.example.fin_ai.ui.theme.GreenCredit
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.SoftRed
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.theme.VioletPrimary
import com.example.fin_ai.ui.viewmodel.AiViewModel
import com.example.fin_ai.ui.viewmodel.ChatMessage
import com.example.fin_ai.ui.viewmodel.HealthFactor
import kotlinx.coroutines.delay

private val AmberTip = Color(0xFFF59E0B)

@Composable
fun AiScreen(viewModel: AiViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var answerTyped by remember { mutableIntStateOf(0) }

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { spoken ->
                draft = spoken
            }
        }
    }

    // Bring the newest message into view, top first so long answers read from the start.
    // Wait a frame so the new item is part of the layout before measuring.
    LaunchedEffect(state.messages.size, state.chatLoading, answerTyped) {
        if (state.messages.size <= 1) return@LaunchedEffect
        delay(80)
        val total = listState.layoutInfo.totalItemsCount
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { AiHeader(onRefresh = viewModel::loadInsights, refreshing = state.insightsLoading) }
            item {
                when {
                    state.insightsLoading -> ShimmerCard(160.dp)
                    state.insightsError != null -> Notice(state.insightsError!!, isError = true, onRetry = viewModel::loadInsights)
                    state.insights != null -> HealthCard(state.insights!!, state.factors)
                }
            }
            val insights = state.insights?.insights.orEmpty()
            if (!state.insightsLoading && insights.isNotEmpty()) {
                item { InsightsPager(insights, onAsk = viewModel::askAbout) }
            } else if (state.insightsLoading) {
                item { ShimmerCard(120.dp) }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Ask FinAI", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    if (state.messages.size > 1) {
                        IconButton(onClick = viewModel::clearChat) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(state.messages, key = { it.id }) { m ->
                ChatBubble(m, Modifier.appear(), onTyped = {
                    viewModel.typed(m.id)
                    answerTyped++ // the answer is now full length: scroll so it reads from the top
                })
            }
            if (state.chatLoading) item(key = "typing") { TypingBubble() }
        }

        AnimatedVisibility(
            visible = !state.chatLoading && draft.isEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.suggestions.forEach { s -> Chip(s, selected = false) { viewModel.send(s) } }
            }
        }

        ChatInput(
            value = draft,
            onValueChange = { draft = it },
            enabled = !state.chatLoading,
            onSend = {
                viewModel.send(draft)
                draft = ""
            },
            onMic = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask FinAI about your money")
                }
                runCatching { voice.launch(intent) }
                    .onFailure { Toast.makeText(context, "Voice input isn't available on this device.", Toast.LENGTH_SHORT).show() }
            }
        )
    }
}

// ---- Header with the animated AI orb --------------------------------------------

@Composable
private fun AiHeader(onRefresh: () -> Unit, refreshing: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.appear()) {
        AiOrb(54.dp, thinking = refreshing)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("AI Advisor", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("Knows your transactions, goals & budgets", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        val spin by rememberInfiniteTransition(label = "refresh").animateFloat(
            0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "spin"
        )
        IconButton(onClick = onRefresh, enabled = !refreshing) {
            Icon(
                Icons.Filled.Refresh, contentDescription = "Refresh insights", tint = VioletLight,
                modifier = Modifier.rotate(if (refreshing) spin else 0f)
            )
        }
    }
}

/** A glowing, slowly rotating gradient orb that spins faster while the AI is thinking. */
@Composable
private fun AiOrb(size: androidx.compose.ui.unit.Dp, thinking: Boolean) {
    val t = rememberInfiniteTransition(label = "orb")
    val rotation by t.animateFloat(0f, 360f, infiniteRepeatable(tween(if (thinking) 1400 else 6000, easing = LinearEasing)), label = "orbRot")
    val pulse by t.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "orbPulse")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size).scale(pulse)) {
            drawCircle(Brush.radialGradient(listOf(VioletPrimary.copy(alpha = 0.55f), Color.Transparent)), radius = this.size.minDimension / 2)
        }
        Canvas(Modifier.size(size * 0.72f).rotate(rotation)) {
            drawCircle(Brush.sweepGradient(listOf(VioletPrimary, AccentCyan, Color(0xFFEC4899), VioletPrimary)))
            drawCircle(Color.White.copy(alpha = 0.18f), radius = this.size.minDimension * 0.22f, center = Offset(this.size.width * 0.35f, this.size.height * 0.32f))
        }
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.3f))
    }
}

// ---- Health score ---------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthCard(data: InsightsData, factors: List<HealthFactor>) {
    val score = (data.healthScore ?: 0).coerceIn(0, 100)
    val color = when {
        score >= 70 -> GreenCredit
        score >= 40 -> AmberTip
        else -> SoftRed
    }
    val label = when {
        score >= 80 -> "Excellent"
        score >= 65 -> "Healthy"
        score >= 40 -> "Needs attention"
        else -> "At risk"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .appear(1)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.16f), MaterialTheme.colorScheme.surfaceContainerLowest)))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(score / 100f, 92.dp, 9.dp, listOf(color, color.copy(alpha = 0.6f)), MaterialTheme.colorScheme.surfaceContainer) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CountUpText(score.toDouble(), { "${it.toInt()}" }, TextStyle(color = color, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp))
                    Text("/ 100", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Financial health", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(data.insightText.orEmpty(), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        if (factors.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                factors.forEach { f ->
                    val c = when (f.good) { true -> GreenCredit; false -> AmberTip; null -> VioletLight }
                    Text(
                        "${f.emoji} ${f.label}",
                        color = c, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(c.copy(alpha = 0.13f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

// ---- Insight cards ----------------------------------------------------------------

@Composable
private fun InsightsPager(insights: List<InsightDto>, onAsk: (InsightDto) -> Unit) {
    val pager = rememberPagerState(pageCount = { insights.size })
    Column(Modifier.appear(2)) {
        HorizontalPager(state = pager, contentPadding = PaddingValues(end = 36.dp), pageSpacing = 12.dp) { page ->
            InsightCard(insights[page]) { onAsk(insights[page]) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(insights.size) { i ->
                val w by animateFloatAsState(if (i == pager.currentPage) 20f else 6f, label = "dot")
                Box(
                    Modifier
                        .height(6.dp)
                        .width(w.dp)
                        .clip(CircleShape)
                        .background(if (i == pager.currentPage) VioletLight else MaterialTheme.colorScheme.surfaceContainer)
                )
            }
        }
    }
}

@Composable
private fun InsightCard(insight: InsightDto, onAsk: () -> Unit) {
    val (icon, tint, kind) = when (insight.type) {
        "warning" -> Triple(Icons.Filled.WarningAmber, SoftRed, "Watch out")
        "opportunity" -> Triple(Icons.Filled.TrendingUp, GreenCredit, "Opportunity")
        else -> Triple(Icons.Filled.Lightbulb, AmberTip, "Tip")
    }
    Column(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.18f), MaterialTheme.colorScheme.surfaceContainerLowest)))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .clickable(onClick = onAsk)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(tint.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(kind.uppercase(), color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(insight.title.orEmpty(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(
            insight.desc.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp,
            maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        Text("Ask AI about this →", color = VioletLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Placeholder with a moving highlight while the AI is working. */
@Composable
private fun ShimmerCard(height: androidx.compose.ui.unit.Dp) {
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        -400f, 1400f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "shimmerX"
    )
    val base = MaterialTheme.colorScheme.surfaceContainerLowest
    val shine = VioletPrimary.copy(alpha = 0.18f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(base, shine, base), start = Offset(x, 0f), end = Offset(x + 400f, 300f)))
            .border(1.dp, OutlineViolet, RoundedCornerShape(22.dp))
    )
}

// ---- Chat -------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(message: ChatMessage, modifier: Modifier, onTyped: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.fromUser) {
            AiOrb(30.dp, thinking = false)
            Spacer(Modifier.width(8.dp))
        }
        val shape = RoundedCornerShape(
            topStart = 18.dp, topEnd = 18.dp,
            bottomStart = if (message.fromUser) 18.dp else 4.dp,
            bottomEnd = if (message.fromUser) 4.dp else 18.dp,
        )
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(
                    if (message.fromUser) Brush.linearGradient(listOf(VioletPrimary, Color(0xFF6D28D9)))
                    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.colorScheme.surfaceContainerLowest))
                )
                .then(if (message.fromUser) Modifier else Modifier.border(1.dp, if (message.isError) SoftRed.copy(alpha = 0.5f) else OutlineViolet, shape))
                .combinedClickable(onClick = {}, onLongClick = {
                    clipboard.setText(AnnotatedString(message.text))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                })
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val textColor = when {
                message.fromUser -> Color.White
                message.isError -> SoftRed
                else -> MaterialTheme.colorScheme.onSurface
            }
            if (!message.fromUser && message.fresh) {
                TypewriterText(message.text, textColor, onDone = onTyped)
            } else {
                Text(
                    if (message.fromUser) AnnotatedString(message.text) else renderMarkdown(message.text),
                    color = textColor, fontSize = 14.sp, lineHeight = 20.sp
                )
            }
        }
    }
}

/** Reveals a reply a few words at a time, like it's being written live. */
@Composable
private fun TypewriterText(text: String, color: Color, onDone: () -> Unit) {
    val words = remember(text) { text.split(Regex("(?<=\\s)")) }
    var shown by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        // Long answers reveal in bigger steps so they never take more than ~3 s.
        val step = (words.size / 120).coerceAtLeast(1)
        while (shown < words.size) {
            shown = (shown + step).coerceAtMost(words.size)
            delay(22)
        }
        onDone()
    }
    Text(renderMarkdown(words.take(shown).joinToString("")), color = color, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun TypingBubble() {
    val t = rememberInfiniteTransition(label = "dots")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.appear()) {
        AiOrb(30.dp, thinking = true)
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, OutlineViolet, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { i ->
                val y by t.animateFloat(
                    0f, -6f,
                    infiniteRepeatable(tween(420, delayMillis = i * 140), RepeatMode.Reverse),
                    label = "dot$i"
                )
                Box(Modifier.offset(y = y.dp).size(8.dp).clip(CircleShape).background(VioletLight))
            }
        }
    }
}

@Composable
private fun ChatInput(value: String, onValueChange: (String) -> Unit, enabled: Boolean, onSend: () -> Unit, onMic: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMic, enabled = enabled) {
            Icon(Icons.Filled.Mic, contentDescription = "Speak your question", tint = if (enabled) VioletLight else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FinTextField(value = value, onValueChange = onValueChange, placeholder = "Ask about your money…", modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        val canSend = enabled && value.isNotBlank()
        val scale by animateFloatAsState(if (canSend) 1f else 0.85f, label = "sendScale")
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (canSend) Brush.linearGradient(listOf(VioletPrimary, AccentCyan))
                    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.surfaceContainer))
                )
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send, contentDescription = "Send",
                tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(if (canSend) 1f else 0.6f)
            )
        }
    }
}

/**
 * Minimal Markdown for chat replies: **bold**, # headings, "- " / "* " bullets and
 * pipe tables (shown as " · "-separated rows). Anything else is left as plain text.
 */
private fun renderMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val lines = stripLatex(text.replace("\r", "")).lines()
    lines.forEachIndexed { index, raw ->
        var line = raw.trimEnd()
        // Table separator rows like |---|:---:| carry no content.
        if (Regex("^\\s*\\|?\\s*:?-{3,}.*$").matches(line) && line.contains("-")) return@forEachIndexed

        var heading = false
        Regex("^\\s*#{1,6}\\s+").find(line)?.let {
            line = line.substring(it.range.last + 1)
            heading = true
        }
        Regex("^(\\s*)[-*]\\s+").find(line)?.let {
            line = it.groupValues[1] + "• " + line.substring(it.range.last + 1)
        }
        if (line.trim().startsWith("|")) {
            line = line.trim().trim('|').split("|").joinToString("  ·  ") { it.trim() }
        }

        if (heading) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendBold(line) } else appendBold(line)
        if (index < lines.lastIndex) append("\n")
    }
}

/**
 * Models occasionally answer with LaTeX ("\[ \frac{a}{b} \approx c \]"), which a chat bubble
 * can't typeset. Turn it into readable text: a ÷ b ≈ c.
 */
internal fun stripLatex(text: String): String {
    if ('\\' !in text) return text
    var t = text
    repeat(3) { // \frac and \text can nest
        t = t.replace(Regex("""\\d?frac\{([^{}]*)\}\{([^{}]*)\}"""), "$1 ÷ $2")
        t = t.replace(Regex("""\\(?:text|mathrm|mathbf|textbf|operatorname)\{([^{}]*)\}"""), "$1")
    }
    val symbols = listOf(
        """\approx""" to "≈", """\times""" to "×", """\div""" to "÷", """\cdot""" to "·",
        """\leq""" to "≤", """\geq""" to "≥", """\le""" to "≤", """\ge""" to "≥", """\neq""" to "≠",
        """\rightarrow""" to "→", """\to""" to "→", """\%""" to "%", """\₹""" to "₹",
    )
    symbols.forEach { (k, v) -> t = t.replace(k, v) }
    return t
        .replace(Regex("""\\[\[\]()]"""), "")      // \[ \] \( \)
        .replace("$$", "")
        .replace(Regex("""\\[,;:! ]"""), " ")       // LaTeX spacing commands
        .replace(Regex("""\\[a-zA-Z]+"""), "")      // any other command
        .replace(Regex("""[{}]"""), "")
        .replace(Regex("""[ \t]{2,}"""), " ")
        .replace(Regex("""\n{3,}"""), "\n\n")
}

private fun AnnotatedString.Builder.appendBold(line: String) {
    val parts = line.split("**")
    parts.forEachIndexed { i, part ->
        if (i % 2 == 1 && i < parts.lastIndex) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = VioletLight)) { append(part) }
        } else {
            append(part)
        }
    }
}
