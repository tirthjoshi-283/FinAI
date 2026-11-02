package com.example.fin_ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fin_ai.data.BudgetRepository
import com.example.fin_ai.data.SplitRepository
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.insights.Achievements
import com.example.fin_ai.insights.BudgetMath
import com.example.fin_ai.insights.SubscriptionDetector
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.util.spend
import com.example.fin_ai.ui.util.thisMonth

private data class Tile(val route: String, val emoji: String, val title: String, val hint: String, val tint: Color)

/** Dashboard hub: one tile per feature, each with a live one-line hint. */
@Composable
fun ExploreSection(transactions: List<TransactionDto>, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val budgets by BudgetRepository.budgets.collectAsState()
    val splits by SplitRepository.splits.collectAsState()
    LaunchedEffect(Unit) {
        BudgetRepository.ensureLoaded(context)
        SplitRepository.ensureLoaded()
    }

    val tiles = remember(transactions, budgets, splits) {
        val streak = Achievements.currentNoSpendStreak(transactions)
        val statuses = BudgetMath.statuses(budgets, transactions)
        val budgetHint = if (statuses.isEmpty()) "Set monthly limits" else {
            val used = (statuses.sumOf { it.spent } * 100 / statuses.sumOf { it.limit }).toInt()
            "$used% of budget used"
        }
        val subs = SubscriptionDetector.detect(transactions)
        val owed = splits.sumOf { it.outstanding }
        val badges = Achievements.badges(transactions, budgets, 0, splits.size).count { it.unlocked }
        listOf(
            Tile("analytics", "📊", "Analytics", "${Fmt.rupees(transactions.thisMonth().spend())} spent this month", Color(0xFF8B5CF6)),
            Tile("budgets", "🎯", "Budgets", budgetHint, Color(0xFF10B981)),
            Tile("subscriptions", "🔁", "Subscriptions", if (subs.isEmpty()) "Auto-detects recurring bills" else "${subs.size} found · ${Fmt.rupees(subs.sumOf { it.monthlyCost })}/mo", Color(0xFF22D3EE)),
            Tile("streaks", "🔥", "Streaks & Badges", if (streak > 0) "$streak-day no-spend streak" else "$badges badges unlocked", Color(0xFFF97316)),
            Tile("wrapped", "✨", "Monthly Wrapped", "Your ${Fmt.monthName()} story", Color(0xFFEC4899)),
            Tile("splits", "🤝", "Split bills", if (owed > 0) "Friends owe you ${Fmt.rupees(owed)}" else "Share costs with friends", Color(0xFFF59E0B)),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Explore", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t -> TileCard(t, Modifier.weight(1f)) { onNavigate(t.route) } }
            }
        }
    }
}

@Composable
private fun TileCard(t: Tile, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(t.tint.copy(alpha = 0.22f), MaterialTheme.colorScheme.surfaceContainerLowest)))
            .border(1.dp, OutlineViolet, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(t.emoji, fontSize = 26.sp)
        Spacer(Modifier.height(10.dp))
        Text(t.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(2.dp))
        Text(t.hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
    }
}
