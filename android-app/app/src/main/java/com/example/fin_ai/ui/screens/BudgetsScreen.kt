package com.example.fin_ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fin_ai.data.BudgetRepository
import com.example.fin_ai.data.Categories
import com.example.fin_ai.data.LedgerRepository
import com.example.fin_ai.data.toUserMessage
import com.example.fin_ai.insights.BudgetMath
import com.example.fin_ai.insights.BudgetStatus
import com.example.fin_ai.ui.theme.AccentCyan
import com.example.fin_ai.ui.theme.GreenCredit
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.SoftRed
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.theme.VioletPrimary
import com.example.fin_ai.ui.util.Fmt
import kotlinx.coroutines.launch

private val Amber = Color(0xFFF59E0B)

fun BudgetStatus.color(): Color = when (level) {
    BudgetStatus.Level.OK -> GreenCredit
    BudgetStatus.Level.WARNING -> Amber
    BudgetStatus.Level.OVER -> SoftRed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val budgets by BudgetRepository.budgets.collectAsState()
    val txs by LedgerRepository.transactions.collectAsState()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<String?>(null) } // category being added/edited, "" = new
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        LedgerRepository.ensureLoaded()
        BudgetRepository.refresh(context).onFailure { error = it.toUserMessage("Couldn't load budgets.") }
        loading = false
    }

    val statuses = remember(budgets, txs) { BudgetMath.statuses(budgets, txs) }
    val suggestions = remember(txs) { BudgetMath.suggestions(txs) }
    val totalLimit = statuses.sumOf { it.limit }
    val totalSpent = statuses.sumOf { it.spent }
    val daysLeft = BudgetMath.daysLeftInMonth()

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { BackRow("Budgets", onBack) }
                IconButton(onClick = { editing = "" }) { Icon(Icons.Filled.Add, contentDescription = "Add budget", tint = VioletLight) }
            }
        }

        if (statuses.isNotEmpty()) {
            item {
                val fraction = (totalSpent / totalLimit).toFloat().coerceIn(0f, 1f)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(VioletPrimary.copy(alpha = 0.35f), AccentCyan.copy(alpha = 0.15f))))
                        .border(1.dp, OutlineViolet, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Text("${Fmt.monthName()} budget", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    com.example.fin_ai.ui.util.AutoSizeText(
                        "${Fmt.rupees(totalSpent)} of ${Fmt.rupees(totalLimit)}",
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                        color = if (totalSpent > totalLimit) SoftRed else VioletLight,
                        trackColor = MaterialTheme.colorScheme.surfaceContainer,
                        strokeCap = StrokeCap.Round,
                        drawStopIndicator = {}
                    )
                    Spacer(Modifier.height(10.dp))
                    val left = totalLimit - totalSpent
                    Text(
                        if (left >= 0) "You can spend about ${Fmt.rupees(left / daysLeft)} a day for the next $daysLeft days."
                        else "You're ${Fmt.rupees(-left)} over your total budget this month.",
                        color = if (left >= 0) MaterialTheme.colorScheme.onSurface else SoftRed,
                        fontSize = 13.sp
                    )
                }
            }
        }

        when {
            loading -> item { Notice("Loading budgets…") }
            error != null && budgets.isEmpty() -> item { Notice(error!!, isError = true) }
            statuses.isEmpty() -> item {
                Card {
                    Text("Set your first budget", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pick a category and a monthly limit. You'll get an alert at 80% and when you go over.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Chip("+ Add budget", selected = true) { editing = "" }
                }
            }
            else -> items(statuses, key = { it.category }) { s -> BudgetRow(s) { editing = s.category } }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    editing?.let { current ->
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            BudgetEditor(
                initialCategory = current.ifEmpty { null },
                initialLimit = budgets.firstOrNull { it.category == current }?.limit,
                existing = budgets.map { it.category }.toSet(),
                suggestions = suggestions,
                onSave = { cat, limit, done ->
                    scope.launch {
                        val r = BudgetRepository.set(context, cat, limit)
                        done(r.exceptionOrNull()?.toUserMessage("Couldn't save the budget."))
                        if (r.isSuccess) editing = null
                    }
                },
                onDelete = if (current.isNotEmpty()) ({
                    scope.launch {
                        BudgetRepository.delete(context, current)
                        editing = null
                    }
                }) else null
            )
        }
    }
}

@Composable
private fun BudgetRow(s: BudgetStatus, onClick: () -> Unit) {
    val color = s.color()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, if (s.level == BudgetStatus.Level.OK) OutlineViolet else color.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(categoryColor(s.category).copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(categoryIconFor(s.category), contentDescription = null, tint = categoryColor(s.category), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.category, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${Fmt.rupees(s.spent)} of ${Fmt.rupees(s.limit)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                )
            }
            Text("${(s.fraction * 100).toInt()}%", color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { s.fraction.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainer,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {}
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when (s.level) {
                BudgetStatus.Level.OVER -> "Over by ${Fmt.rupees(-s.remaining)}"
                BudgetStatus.Level.WARNING -> "Careful — only ${Fmt.rupees(s.remaining)} left"
                BudgetStatus.Level.OK -> "${Fmt.rupees(s.remaining)} left"
            },
            color = if (s.level == BudgetStatus.Level.OK) MaterialTheme.colorScheme.onSurfaceVariant else color,
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetEditor(
    initialCategory: String?,
    initialLimit: Double?,
    existing: Set<String>,
    suggestions: Map<String, Double>,
    onSave: (String, Double, (String?) -> Unit) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var category by remember { mutableStateOf(initialCategory ?: Categories.debit.firstOrNull { it !in existing } ?: Categories.debit.first()) }
    var amount by remember { mutableStateOf(initialLimit?.toLong()?.toString().orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (initialCategory == null) "New budget" else "Edit ${initialCategory} budget", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (initialCategory == null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Categories.debit.filter { it !in existing }.forEach { c -> Chip(c, selected = c == category) { category = c } }
            }
        }
        FinTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit).take(9) }, placeholder = "Monthly limit (₹)", numeric = true)
        suggestions[category]?.let { s ->
            Text(
                "Suggested: ${Fmt.rupees(s)} — your average $category spend over the last 3 months. Tap to use.",
                color = VioletLight, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { amount = s.toLong().toString() }.padding(vertical = 4.dp)
            )
        }
        error?.let { Text(it, color = SoftRed, fontSize = 13.sp) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(VioletPrimary, AccentCyan)))
                    .clickable(enabled = !saving) {
                        val v = amount.toDoubleOrNull()
                        if (v == null || v <= 0) { error = "Enter a monthly limit greater than 0."; return@clickable }
                        saving = true
                        onSave(category, v) { err -> saving = false; error = err }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Save budget", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            onDelete?.let {
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = it) { Icon(Icons.Filled.Delete, contentDescription = "Delete budget", tint = SoftRed) }
            }
        }
    }
}
