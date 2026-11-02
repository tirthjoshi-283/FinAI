package com.example.fin_ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fin_ai.data.LedgerRepository
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.ui.theme.AccentCyan
import com.example.fin_ai.ui.theme.GreenCredit
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.SoftRed
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.util.income
import com.example.fin_ai.ui.util.isDebit
import com.example.fin_ai.ui.util.millis
import com.example.fin_ai.ui.util.monthAgo
import com.example.fin_ai.ui.util.payee
import com.example.fin_ai.ui.util.spend
import java.util.Calendar

@Composable
fun AnalyticsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val all by LedgerRepository.transactions.collectAsState()
    LaunchedEffect(Unit) { LedgerRepository.ensureLoaded() }
    var monthsAgo by remember { mutableIntStateOf(0) }
    var selectedCat by remember { mutableStateOf<String?>(null) }

    val month = remember(all, monthsAgo) { all.monthAgo(monthsAgo) }
    val prev = remember(all, monthsAgo) { all.monthAgo(monthsAgo + 1) }
    val spent = month.spend()
    val received = month.income()
    val prevSpent = prev.spend()

    val slices = remember(month) {
        month.filter { it.isDebit }.groupBy { it.category }
            .map { (c, l) -> Slice(c, l.sumOf { it.amount }, categoryColor(c)) }
            .sortedByDescending { it.value }
    }
    val sixMonths = remember(all) {
        (5 downTo 0).map { m ->
            val l = all.monthAgo(m)
            BarGroup(Fmt.monthName(m, "MMM"), listOf(l.spend(), l.income()))
        }
    }
    val daily = remember(month, monthsAgo) { dailySpend(month, monthsAgo) }
    val topMerchants = remember(month) {
        month.filter { it.isDebit }.groupBy { it.payee }.map { (p, l) -> Triple(p, l.sumOf { it.amount }, l.size) }
            .sortedByDescending { it.second }.take(5)
    }
    val biggest = remember(month, selectedCat) {
        month.filter { it.isDebit && (selectedCat == null || it.category == selectedCat) }.sortedByDescending { it.amount }.take(5)
    }
    val weekday = remember(all) { busiestWeekday(all) }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BackRow("Analytics", onBack) }

        // Month switcher
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { monthsAgo++; selectedCat = null }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = VioletLight)
                }
                Text(Fmt.monthName(monthsAgo, "MMMM yyyy"), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                IconButton(onClick = { if (monthsAgo > 0) { monthsAgo--; selectedCat = null } }, enabled = monthsAgo > 0) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month",
                        tint = if (monthsAgo > 0) VioletLight else MaterialTheme.colorScheme.surfaceContainer
                    )
                }
            }
        }

        // Summary
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Spent", Fmt.rupees(spent), SoftRed, Modifier.weight(1f), delta(spent, prevSpent, lowerIsBetter = true))
                StatTile("Received", Fmt.rupees(received), GreenCredit, Modifier.weight(1f))
            }
        }
        item {
            val saved = received - spent
            val rate = if (received > 0) (saved / received * 100).toInt() else null
            StatTile(
                "Saved",
                Fmt.rupees(saved),
                if (saved >= 0) AccentCyan else SoftRed,
                Modifier.fillMaxWidth(),
                when {
                    saved < 0 -> "You spent ${Fmt.rupees(-saved)} more than you received this month."
                    rate != null -> "Savings rate $rate%" + if (rate >= 20) " — great!" else ""
                    else -> null
                }
            )
        }

        // Donut
        item {
            Card {
                Text("Where your money went", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("Tap a slice to focus on a category", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                if (slices.isEmpty()) {
                    Text("No spending recorded this month.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                } else {
                    val focus = slices.firstOrNull { it.label == selectedCat }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(
                            slices = slices,
                            selected = selectedCat,
                            onSelect = { selectedCat = it },
                            centerTop = Fmt.rupees(focus?.value ?: spent),
                            centerBottom = focus?.let { "${it.label}\n${(it.value * 100 / spent).toInt()}%" } ?: "total spent"
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    slices.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(s.color))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                s.label,
                                color = if (selectedCat == null || selectedCat == s.label) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${(s.value * 100 / spent).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(Fmt.rupees(s.value), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Daily
        item {
            Card {
                Text("Daily spending", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val active = daily.count { it > 0 }
                val avg = if (daily.isNotEmpty()) daily.sum() / daily.size else 0.0
                Text(
                    "Average ${Fmt.rupees(avg)} / day · spent on $active of ${daily.size} days",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                DailyBars(daily, VioletLight, avg)
            }
        }

        // Six months
        item {
            Card {
                Text("Last 6 months", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Legend(listOf("Money out" to SoftRed, "Money in" to GreenCredit))
                Spacer(Modifier.height(12.dp))
                GroupedBarChart(sixMonths, listOf(SoftRed, GreenCredit))
            }
        }

        // Patterns
        weekday?.let { (day, share) ->
            item {
                Card {
                    Text("Your spending pattern", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "You spend the most on ${day}s — $share% of all your spending happens that day.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp
                    )
                }
            }
        }

        // Top merchants
        if (topMerchants.isNotEmpty()) {
            item { SectionTitle("Top places you paid") }
            items(topMerchants) { (name, total, count) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(1.dp, OutlineViolet, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$count payment${if (count == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Text(Fmt.rupees(total), color = SoftRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }

        // Biggest
        if (biggest.isNotEmpty()) {
            item { SectionTitle(if (selectedCat != null) "Biggest $selectedCat expenses" else "Biggest expenses") }
            items(biggest, key = { it.id }) { tx -> MiniTxRow(tx) { onOpen(tx.id) } }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun delta(now: Double, before: Double, lowerIsBetter: Boolean): String? {
    if (before <= 0) return null
    val pct = ((now - before) / before * 100).toInt()
    val arrow = if (pct > 0) "▲" else if (pct < 0) "▼" else "="
    val good = if (lowerIsBetter) pct < 0 else pct > 0
    return "$arrow ${kotlin.math.abs(pct)}% vs last month" + if (pct != 0) (if (good) " 👍" else "") else ""
}

/** Spend per calendar day of the selected month (up to today for the current month). */
private fun dailySpend(month: List<TransactionDto>, monthsAgo: Int): List<Double> {
    val cal = Calendar.getInstance().apply { timeInMillis = Fmt.startOfMonth(monthsAgo) }
    val days = if (monthsAgo == 0) Calendar.getInstance().get(Calendar.DAY_OF_MONTH) else cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val out = DoubleArray(days)
    month.filter { it.isDebit }.forEach { tx ->
        val d = Calendar.getInstance().apply { timeInMillis = tx.millis }.get(Calendar.DAY_OF_MONTH) - 1
        if (d in out.indices) out[d] += tx.amount
    }
    return out.toList()
}

private fun busiestWeekday(all: List<TransactionDto>): Pair<String, Int>? {
    val debits = all.filter { it.isDebit }
    if (debits.size < 8) return null
    val byDay = debits.groupBy { Fmt.format(it.millis, "EEEE") }.mapValues { (_, l) -> l.sumOf { it.amount } }
    val total = byDay.values.sum().takeIf { it > 0 } ?: return null
    val (day, sum) = byDay.maxByOrNull { it.value } ?: return null
    return day to (sum * 100 / total).toInt()
}

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier, sub: String? = null) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        com.example.fin_ai.ui.util.AutoSizeText(value, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth())
        sub?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
internal fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) { content() }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
}
