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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.fin_ai.data.SplitDto
import com.example.fin_ai.data.SplitPersonDto
import com.example.fin_ai.data.SplitRepository
import com.example.fin_ai.data.toUserMessage
import com.example.fin_ai.ui.theme.AccentCyan
import com.example.fin_ai.ui.theme.GreenCredit
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.SoftRed
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.theme.VioletPrimary
import com.example.fin_ai.ui.util.Fmt
import kotlinx.coroutines.launch

private class Friend(name: String, amount: String) {
    var name by mutableStateOf(name)
    var amount by mutableStateOf(amount)
}

/** Split one expense: add friends and how much each owes you. */
@Composable
fun SplitEditorScreen(transactionId: String, onBack: () -> Unit) {
    val all by LedgerRepository.transactions.collectAsState()
    val tx = all.firstOrNull { it.id == transactionId }
    val scope = rememberCoroutineScope()
    val friends = remember { mutableStateListOf<Friend>() }
    var includeMe by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        SplitRepository.ensureLoaded()
        if (!loaded) {
            SplitRepository.forTransaction(transactionId)?.people?.forEach {
                friends += Friend(it.name, "%.2f".format(it.amount).removeSuffix(".00"))
            }
            if (friends.isEmpty()) friends += Friend("", "")
            loaded = true
        }
    }

    if (tx == null) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
            BackRow("Split bill", onBack)
            Notice("Loading…")
        }
        return
    }

    val owed = friends.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val mine = tx.amount - owed

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackRow("Split bill", onBack) }
        item {
            Card {
                Text(tx.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("${Fmt.rupees(tx.amount)} · ${Fmt.dayMonth(tx.date)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Chip("Split equally", selected = true) {
                        val named = friends.filter { it.name.isNotBlank() }.ifEmpty { friends.toList() }
                        val share = tx.amount / (named.size + if (includeMe) 1 else 0)
                        named.forEach { it.amount = "%.2f".format(share).removeSuffix(".00") }
                    }
                    Spacer(Modifier.width(8.dp))
                    Chip(if (includeMe) "Including me" else "Not including me", selected = includeMe, tint = AccentCyan) { includeMe = !includeMe }
                }
            }
        }
        itemsIndexed(friends) { i, f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                FinTextField(value = f.name, onValueChange = { f.name = it.take(40) }, placeholder = "Friend ${i + 1}", modifier = Modifier.weight(1.4f))
                Spacer(Modifier.width(8.dp))
                FinTextField(value = f.amount, onValueChange = { f.amount = it.take(10) }, placeholder = "₹", numeric = true, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (friends.size > 1) friends.removeAt(i) else { f.name = ""; f.amount = "" } }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Chip("+ Add friend", selected = false) { if (friends.size < 20) friends += Friend("", "") } }
        item {
            Card {
                SummaryLine("Friends owe you", Fmt.rupees(owed), VioletLight)
                SummaryLine("Your share", Fmt.rupees(mine), if (mine < -0.01) SoftRed else MaterialTheme.colorScheme.onSurface)
            }
        }
        error?.let { item { Text(it, color = SoftRed, fontSize = 13.sp) } }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(VioletPrimary, AccentCyan)))
                    .clickable(enabled = !saving) {
                        val people = friends.filter { it.name.isNotBlank() }
                            .map { SplitPersonDto(it.name.trim(), it.amount.toDoubleOrNull() ?: -1.0) }
                        error = when {
                            people.isEmpty() -> "Add at least one friend's name."
                            people.any { it.amount < 0 } -> "Enter an amount for every friend."
                            mine < -0.01 -> "Friends can't owe more than the bill."
                            else -> null
                        }
                        if (error != null) return@clickable
                        saving = true
                        scope.launch {
                            SplitRepository.save(tx.id, people)
                                .onSuccess { onBack() }
                                .onFailure { error = it.toUserMessage("Couldn't save the split.") }
                            saving = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Save split", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** Everything friends owe you, with one-tap "paid" ticks. */
@Composable
fun SplitsScreen(onBack: () -> Unit, onOpenTransaction: (String) -> Unit) {
    val splits by SplitRepository.splits.collectAsState()
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        LedgerRepository.ensureLoaded()
        SplitRepository.refresh().onFailure { error = it.toUserMessage("Couldn't load splits.") }
        loading = false
    }
    val outstanding = splits.sumOf { it.outstanding }
    val byPerson = splits.flatMap { s -> s.people.filterNot { it.settled } }.groupBy { it.name }
        .mapValues { (_, l) -> l.sumOf { it.amount } }.toList().sortedByDescending { it.second }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackRow("Split bills", onBack) }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(GreenCredit.copy(alpha = 0.28f), VioletPrimary.copy(alpha = 0.2f))))
                    .border(1.dp, OutlineViolet, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text("Friends owe you", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                com.example.fin_ai.ui.util.AutoSizeText(Fmt.rupees(outstanding), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 28.sp, modifier = Modifier.fillMaxWidth())
                if (byPerson.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(byPerson.take(4).joinToString("  ·  ") { "${it.first} ${Fmt.rupees(it.second)}" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
            }
        }
        when {
            loading -> item { Notice("Loading…") }
            error != null -> item { Notice(error!!, isError = true) }
            splits.isEmpty() -> item {
                Notice("No splits yet. Open any expense in your ledger and tap \"Split bill\" to share it with friends.")
            }
            else -> items(splits, key = { it.id }) { s ->
                SplitCard(
                    s,
                    onOpen = { onOpenTransaction(s.transactionId) },
                    onToggle = { name, settled -> scope.launch { SplitRepository.settle(s, name, settled) } },
                    onDelete = { scope.launch { SplitRepository.delete(s) } },
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SplitCard(s: SplitDto, onOpen: () -> Unit, onToggle: (String, Boolean) -> Unit, onDelete: () -> Unit) {
    val done = s.outstanding <= 0.0
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, if (done) GreenCredit.copy(alpha = 0.4f) else OutlineViolet, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onOpen)) {
            Column(Modifier.weight(1f)) {
                Text(s.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Bill ${Fmt.rupees(s.total)} · your share ${Fmt.rupees(s.myShare)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Text(if (done) "Settled ✓" else "${Fmt.rupees(s.outstanding)} due", color = if (done) GreenCredit else VioletLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        s.people.forEach { p ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onToggle(p.name, !p.settled) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (p.settled) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (p.settled) "Paid" else "Not paid",
                    tint = if (p.settled) GreenCredit else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(26.dp).clip(CircleShape).background(VioletPrimary.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    Text(p.name.take(1).uppercase(), color = VioletLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(p.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(Fmt.rupees(p.amount), color = if (p.settled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Remove split", color = SoftRed.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDelete).padding(vertical = 4.dp))
    }
}

@Composable
private fun SummaryLine(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
