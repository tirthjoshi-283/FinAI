package com.example.fin_ai.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fin_ai.data.ApiClient
import com.example.fin_ai.data.BudgetRepository
import com.example.fin_ai.data.LedgerRepository
import com.example.fin_ai.data.SplitRepository
import com.example.fin_ai.insights.Achievements
import com.example.fin_ai.insights.Badge
import com.example.fin_ai.insights.Challenge
import com.example.fin_ai.ui.theme.AccentCyan
import com.example.fin_ai.ui.theme.GreenCredit
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.theme.VioletPrimary

private val Flame = Color(0xFFF97316)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreaksScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val txs by LedgerRepository.transactions.collectAsState()
    val budgets by BudgetRepository.budgets.collectAsState()
    val splits by SplitRepository.splits.collectAsState()
    var goalsDone by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        LedgerRepository.ensureLoaded()
        BudgetRepository.ensureLoaded(context)
        SplitRepository.ensureLoaded()
        goalsDone = runCatching { ApiClient.api.getGoals().goals.count { it.isComplete } }.getOrDefault(0)
    }

    val streak = remember(txs) { Achievements.currentNoSpendStreak(txs) }
    val best = remember(txs) { Achievements.bestNoSpendStreak(txs) }
    val challenges = remember(txs) { Achievements.challenges(txs) }
    val badges = remember(txs, budgets, goalsDone, splits) { Achievements.badges(txs, budgets, goalsDone, splits.size) }
    val level = remember(txs, badges) { Achievements.level(txs, badges) }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BackRow("Streaks & Badges", onBack) }

        // Streak hero
        item {
            val pulse by rememberInfiniteTransition(label = "flame").animateFloat(
                initialValue = 1f, targetValue = 1.12f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "flameScale"
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Flame.copy(alpha = 0.35f), VioletPrimary.copy(alpha = 0.25f))))
                    .border(1.dp, Flame.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔥", fontSize = 46.sp, modifier = Modifier.scale(if (streak > 0) pulse else 1f).alpha(if (streak > 0) 1f else 0.4f))
                Text("$streak day${if (streak == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
                Text("no-spend streak", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        streak == 0 -> "You spent today. Skip spending tomorrow to start a streak!"
                        streak < best -> "${best - streak} more day${if (best - streak == 1) "" else "s"} to beat your best of $best."
                        else -> "This is your best streak ever! 🎉"
                    },
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, textAlign = TextAlign.Center
                )
            }
        }

        // Level
        item {
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Level ${level.number} · ${level.name}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${level.xp} XP · ${level.xpForNext - level.xpIntoLevel} XP to next level", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Text("⭐", fontSize = 26.sp)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { level.xpIntoLevel / level.xpForNext.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = AccentCyan, trackColor = MaterialTheme.colorScheme.surfaceContainer,
                    strokeCap = StrokeCap.Round, drawStopIndicator = {}
                )
                Spacer(Modifier.height(6.dp))
                Text("Earn XP by unlocking badges, keeping streaks and tracking every rupee.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }

        item { SectionTitle("This week's challenges") }
        items(challenges) { ChallengeCard(it) }

        item { SectionTitle("Badges · ${badges.count { it.unlocked }}/${badges.size} unlocked") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 3) {
                badges.sortedByDescending { it.unlocked }.forEach { BadgeTile(it, Modifier.weight(1f)) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ChallengeCard(c: Challenge) {
    val color = if (c.good) GreenCredit else Flame
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(if (c.good) "On track ✓" else "Behind", color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Text(c.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { c.progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            color = color, trackColor = MaterialTheme.colorScheme.surfaceContainer,
            strokeCap = StrokeCap.Round, drawStopIndicator = {}
        )
    }
}

@Composable
private fun BadgeTile(b: Badge, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (b.unlocked) VioletPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, if (b.unlocked) VioletLight.copy(alpha = 0.5f) else OutlineViolet, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp)
            .alpha(if (b.unlocked) 1f else 0.45f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (b.unlocked) b.emoji else "🔒", fontSize = 26.sp)
        Spacer(Modifier.height(6.dp))
        Text(b.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(b.howTo, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}
