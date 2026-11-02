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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fin_ai.data.Categories
import com.example.fin_ai.data.LedgerRepository
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.ui.theme.GreenCredit
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.SoftRed
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.theme.VioletPrimary
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.util.isDebit
import com.example.fin_ai.ui.util.payee
import com.example.fin_ai.ui.util.spend
import com.example.fin_ai.ui.util.thisMonth
import com.example.fin_ai.ui.viewmodel.TransactionDetailViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailScreen(
    id: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onSplit: (String) -> Unit,
    viewModel: TransactionDetailViewModel = viewModel(),
) {
    val all by LedgerRepository.transactions.collectAsState()
    val state by viewModel.state.collectAsState()
    val tx = all.firstOrNull { it.id == id }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    if (tx == null) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
            BackRow("Transaction", onBack)
            Spacer(Modifier.height(20.dp))
            Notice(if (all.isEmpty()) "Loading…" else "This transaction no longer exists.")
        }
        return
    }

    val insights = remember(tx, all) { buildInsights(tx, all) }
    val similar = remember(tx, all) {
        all.filter { it.id != tx.id && (it.payee.equals(tx.payee, ignoreCase = true) || it.category == tx.category) }
            .sortedByDescending { if (it.payee.equals(tx.payee, ignoreCase = true)) 1 else 0 }
            .take(5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BackRow("Transaction", onBack) }

        // ---- Hero ----
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, OutlineViolet, RoundedCornerShape(24.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background((if (tx.isCredit) GreenCredit else SoftRed).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(categoryIconFor(tx.category), contentDescription = null, tint = if (tx.isCredit) GreenCredit else SoftRed)
                }
                Spacer(Modifier.height(12.dp))
                com.example.fin_ai.ui.util.AutoSizeText(
                    Fmt.signed(tx),
                    color = if (tx.isCredit) GreenCredit else SoftRed,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(tx.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(Fmt.dateTime(tx.date), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }

        // ---- Insights ----
        if (insights.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(VioletPrimary.copy(alpha = 0.10f))
                        .border(1.dp, VioletPrimary.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Insights, contentDescription = null, tint = VioletLight, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Insights", color = VioletLight, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    insights.forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 18.sp) }
                }
            }
        }

        // ---- Details ----
        item {
            val clipboard = LocalClipboardManager.current
            DetailCard {
                DetailRow("Type", if (tx.isCredit) "Money in (credit)" else "Money out (debit)")
                tx.merchant?.takeIf { it.isNotBlank() }?.let { DetailRow(if (tx.isCredit) "From" else "Paid to", it) }
                tx.bankName?.takeIf { it.isNotBlank() }?.let { DetailRow("Bank", it) }
                tx.accountLast4?.takeIf { it.isNotBlank() }?.let { DetailRow("Account", "•••• $it") }
                tx.upiRef?.takeIf { it.isNotBlank() }?.let { ref ->
                    DetailRow("UPI / Ref no.", ref, trailing = {
                        IconButton(onClick = { clipboard.setText(AnnotatedString(ref)) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy reference", tint = VioletLight, modifier = Modifier.size(16.dp))
                        }
                    })
                }
                DetailRow("Added via", sourceLabel(tx.source))
            }
        }

        // ---- Category ----
        item {
            DetailCard {
                Text("Category", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (if (tx.isCredit) Categories.credit else Categories.debit).forEach { cat ->
                        Chip(cat, selected = cat == tx.category) { viewModel.setCategory(tx, cat) }
                    }
                }
            }
        }

        // ---- Note ----
        item {
            var note by remember(tx.id) { mutableStateOf(tx.note.orEmpty()) }
            DetailCard {
                Text("Note", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                FinTextField(value = note, onValueChange = { note = it.take(300) }, placeholder = "e.g. Dinner with college friends")
                if (note.trim() != tx.note.orEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Chip("Save note", selected = true) { viewModel.saveNote(tx, note) }
                }
            }
        }

        state.message?.let { msg ->
            item {
                LaunchedEffect(msg) { kotlinx.coroutines.delay(2500); viewModel.clearMessage() }
                Text(msg, color = VioletLight, fontSize = 12.sp)
            }
        }

        // ---- Actions ----
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (tx.isDebit) ActionButton(Icons.Filled.CallSplit, "Split bill", VioletLight, Modifier.weight(1f)) { onSplit(tx.id) }
                ActionButton(Icons.Filled.Delete, "Delete", SoftRed, Modifier.weight(1f)) { confirmDelete = true }
            }
        }

        // ---- Similar ----
        if (similar.isNotEmpty()) {
            item {
                Text("Similar transactions", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            items(similar, key = { it.id }) { s -> MiniTxRow(s) { onOpen(s.id) } }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text("Delete this transaction?", fontSize = 18.sp) },
            text = { Text("${Fmt.signed(tx)} · ${tx.title}", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete(tx) }) { Text("Delete", color = SoftRed) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        )
    }
}

/** Plain-language facts about one transaction relative to the rest of the ledger. */
private fun buildInsights(tx: TransactionDto, all: List<TransactionDto>): List<String> {
    val out = mutableListOf<String>()
    val month = all.thisMonth()
    if (tx.isDebit) {
        val samePayee = month.filter { it.isDebit && it.payee.equals(tx.payee, ignoreCase = true) }
        if (samePayee.size > 1) {
            out += "You've paid ${tx.payee} ${samePayee.size} times this month — ${Fmt.rupees(samePayee.spend())} in total."
        }
        val monthDebits = month.filter { it.isDebit }.sortedByDescending { it.amount }
        val rank = monthDebits.indexOfFirst { it.id == tx.id }
        if (rank in 0..2 && monthDebits.size >= 5) {
            out += "This is your #${rank + 1} biggest expense this month."
        }
        val catTotal = month.filter { it.isDebit && it.category == tx.category }.spend()
        val monthTotal = month.spend()
        if (catTotal > 0 && monthTotal > 0) {
            out += "${tx.category} is ${(catTotal * 100 / monthTotal).toInt()}% of your spending this month (${Fmt.rupees(catTotal)})."
        }
        val avg = all.filter { it.isDebit && it.category == tx.category }.map { it.amount }.average()
        if (!avg.isNaN() && tx.amount > avg * 2 && all.count { it.category == tx.category } >= 4) {
            out += "That's over 2× your usual ${tx.category} spend of ${Fmt.rupees(avg)}."
        }
    } else {
        val income = month.filter { it.isCredit }.sumOf { it.amount }
        if (income > 0) out += "Makes up ${(tx.amount * 100 / income).toInt()}% of the money you received this month."
    }
    return out
}

private fun sourceLabel(source: String?): String = when (source) {
    "sms" -> "Bank SMS (auto-captured)"
    "bill_scan" -> "Scanned bill"
    "gmail_upi" -> "Gmail UPI alert"
    else -> "Added manually"
}

@Composable
internal fun BackRow(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun DetailRow(label: String, value: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
internal fun Chip(label: String, selected: Boolean, tint: Color = VioletLight, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) tint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, if (selected) tint.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** Compact clickable row reused by detail, analytics and subscription screens. */
@Composable
internal fun MiniTxRow(tx: TransactionDto, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(categoryIconFor(tx.category), contentDescription = null, tint = if (tx.isCredit) GreenCredit else SoftRed, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${Fmt.dayMonth(tx.date)} · ${tx.category}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Text(Fmt.signed(tx), color = if (tx.isCredit) GreenCredit else SoftRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
