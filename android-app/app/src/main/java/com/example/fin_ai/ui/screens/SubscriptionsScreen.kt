package com.example.fin_ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fin_ai.data.LedgerRepository
import com.example.fin_ai.insights.Subscription
import com.example.fin_ai.insights.SubscriptionDetector
import com.example.fin_ai.ui.theme.AccentCyan
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.SoftRed
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.theme.VioletPrimary
import com.example.fin_ai.ui.util.Fmt

private val Amber = Color(0xFFF59E0B)

@Composable
fun SubscriptionsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val all by LedgerRepository.transactions.collectAsState()
    LaunchedEffect(Unit) { LedgerRepository.ensureLoaded() }
    val subs = remember(all) { SubscriptionDetector.detect(all) }
    val monthly = subs.sumOf { it.monthlyCost }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BackRow("Subscriptions & Bills", onBack) }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(VioletPrimary.copy(alpha = 0.35f), AccentCyan.copy(alpha = 0.15f))))
                    .border(1.dp, OutlineViolet, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text("Recurring payments", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                com.example.fin_ai.ui.util.AutoSizeText("${Fmt.rupees(monthly)} / month", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 26.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(
                    if (subs.isEmpty()) "Nothing recurring found yet."
                    else "${subs.size} found automatically · ${Fmt.rupees(monthly * 12)} a year",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
            }
        }
        if (subs.isEmpty()) {
            item {
                Notice(
                    "FinAI looks for payments to the same place, for about the same amount, on a regular schedule " +
                        "(weekly, monthly, quarterly or yearly). As your history grows — especially with bank SMS " +
                        "auto-capture on — Netflix, rent, recharges and EMIs will show up here."
                )
            }
        } else {
            val soon = subs.filter { it.daysUntilDue() in 0..7 }
            if (soon.isNotEmpty()) {
                item { SectionTitle("Due this week") }
                items(soon, key = { "soon_" + it.lastTransactionId }) { SubscriptionRow(it) { onOpen(it.lastTransactionId) } }
            }
            item { SectionTitle("All recurring payments") }
            items(subs, key = { it.lastTransactionId }) { SubscriptionRow(it) { onOpen(it.lastTransactionId) } }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SubscriptionRow(s: Subscription, onClick: () -> Unit) {
    val days = s.daysUntilDue()
    val (dueText, dueColor) = when {
        days < 0 -> "Was due ${-days} day${if (days == -1) "" else "s"} ago" to SoftRed
        days == 0 -> "Due today" to Amber
        days == 1 -> "Due tomorrow" to Amber
        days <= 7 -> "Due in $days days" to Amber
        else -> "Next on ${Fmt.format(s.nextDue, "dd MMM")}" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(categoryColor(s.category).copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(categoryIconFor(s.category), contentDescription = null, tint = categoryColor(s.category), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${s.frequency.label} · paid ${s.occurrences}×", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(dueText, color = dueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Fmt.rupees(s.amount), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (s.frequency != Subscription.Frequency.MONTHLY) {
                Text("≈ ${Fmt.rupees(s.monthlyCost)}/mo", color = VioletLight, fontSize = 11.sp)
            }
        }
    }
}
