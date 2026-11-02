package com.example.fin_ai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fin_ai.data.Categories
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.ui.theme.AccentCyan
import com.example.fin_ai.ui.theme.GreenCredit
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.SoftRed
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.theme.VioletPrimary
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.viewmodel.ScanResult
import com.example.fin_ai.ui.viewmodel.TransactionsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(onOpen: (String) -> Unit = {}, viewModel: TransactionsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(TypeFilter.ALL) }
    var period by remember { mutableStateOf(Period.ALL) }
    var category by remember { mutableStateOf<String?>(null) }
    val filtered = remember(state.transactions, query, typeFilter, period, category) {
        filterLedger(state.transactions, query, typeFilter, period, category)
    }
    var sheetOpen by remember { mutableStateOf(false) }
    var scanSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scanSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) cameraUri?.let(viewModel::scanBill)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::scanBill)
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::scanBill)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { scanSheetOpen = true },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = VioletLight
                ) {
                    Icon(Icons.Filled.DocumentScanner, contentDescription = "Scan a bill")
                }
                FloatingActionButton(
                    onClick = { sheetOpen = true },
                    containerColor = VioletPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New entry")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Ledger",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
            item { SmsCaptureCard(onImported = viewModel::refresh) }
            when {
                state.loading -> item { LedgerNotice("Loading your ledger...") }
                state.error != null -> item {
                    LedgerNotice(state.error!!, isError = true, onRetry = viewModel::refresh)
                }
                state.transactions.isEmpty() -> item {
                    LedgerNotice("No entries yet. Tap + to add one, or scan a bill.")
                }
                else -> {
                    item {
                        LedgerFilters(
                            query = query, onQuery = { query = it },
                            typeFilter = typeFilter, onType = { typeFilter = it },
                            period = period, onPeriod = { period = it },
                            category = category, onCategory = { category = it },
                            categories = state.transactions.map { it.category }.distinct().sorted(),
                        )
                    }
                    item {
                        val out = filtered.filterNot { it.isCredit }.sumOf { it.amount }
                        val inc = filtered.filter { it.isCredit }.sumOf { it.amount }
                        Text(
                            "${filtered.size} entr${if (filtered.size == 1) "y" else "ies"}  ·  Out ${Fmt.rupees(out)}  ·  In ${Fmt.rupees(inc)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    if (filtered.isEmpty()) item { LedgerNotice("Nothing matches these filters.") }
                    filtered.groupBy { Fmt.dayHeader(it.date) }.forEach { (day, txs) ->
                        item(key = "day_$day") {
                            Text(
                                day,
                                color = VioletLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(txs, key = { it.id }) { tx ->
                            LedgerRow(tx = tx, onClick = { onOpen(tx.id) }, onDelete = { viewModel.deleteTransaction(tx.id) })
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                sheetOpen = false
                viewModel.clearSaveError()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            AddTransactionForm(
                saving = state.saving,
                error = state.saveError,
                onSubmit = { title, amount, category, isCredit ->
                    viewModel.addTransaction(title, amount, category, isCredit) {
                        sheetOpen = false
                    }
                }
            )
        }
    }

    if (scanSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { scanSheetOpen = false },
            sheetState = scanSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            ScanOptions(
                onCamera = {
                    scanSheetOpen = false
                    val dir = File(context.cacheDir, "bills").apply { mkdirs() }
                    val file = File(dir, "bill-${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    cameraUri = uri
                    takePhoto.launch(uri)
                },
                onGallery = {
                    scanSheetOpen = false
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onFile = {
                    scanSheetOpen = false
                    pickFile.launch(arrayOf("application/pdf", "image/*"))
                }
            )
        }
    }

    if (state.scanning) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text("Reading your bill…", fontSize = 18.sp) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = VioletPrimary
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "AI is finding the total, date and category.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        )
    }

    state.scanResult?.let { result ->
        ScanResultDialog(
            result = result,
            onCategory = viewModel::changeScannedCategory,
            onUndo = viewModel::undoScan,
            onDone = viewModel::dismissScan
        )
    }

    val scanError = state.scanError
    if (scanError != null && state.scanResult == null && !state.scanning) {
        AlertDialog(
            onDismissRequest = viewModel::dismissScan,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text("Couldn't add that bill", fontSize = 18.sp) },
            text = { Text(scanError, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissScan) { Text("OK", color = VioletLight) }
            }
        )
    }
}

@Composable
private fun ScanOptions(onCamera: () -> Unit, onGallery: () -> Unit, onFile: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Scan a Bill",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Restaurant bills, shopping invoices, tax receipts and more. AI reads the total " +
                "and date, picks the category and adds it as a debit.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))
        ScanOptionRow(Icons.Filled.CameraAlt, "Take a photo", "Use the camera", onCamera)
        Spacer(Modifier.height(10.dp))
        ScanOptionRow(Icons.Filled.Image, "Choose from gallery", "Photo or screenshot of a bill", onGallery)
        Spacer(Modifier.height(10.dp))
        ScanOptionRow(Icons.Filled.PictureAsPdf, "Choose a PDF or file", "E-bills and invoices, up to 4 MB", onFile)
    }
}

@Composable
private fun ScanOptionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, OutlineViolet, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = VioletLight, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScanResultDialog(
    result: ScanResult,
    onCategory: (String) -> Unit,
    onUndo: () -> Unit,
    onDone: () -> Unit,
) {
    val tx = result.transaction
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text("Bill added", fontSize = 18.sp) },
        text = {
            Column {
                Text(
                    tx.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    formatSignedAmount(tx) + "  ·  " + tx.date.take(10),
                    color = SoftRed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                if (result.summary.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        result.summary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Category (tap to change)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Categories.debit.forEach { cat ->
                        TypeChip(cat, selected = cat == tx.category, tint = VioletLight) {
                            if (cat != tx.category) onCategory(cat)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text("Done", color = VioletLight) } },
        dismissButton = { TextButton(onClick = onUndo) { Text("Undo", color = SoftRed) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionForm(
    saving: Boolean,
    error: String?,
    onSubmit: (String, String, String, Boolean) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isCredit by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(Categories.debit.first()) }
    val categories = if (isCredit) Categories.credit else Categories.debit

    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "New Entry",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(16.dp))

        error?.let {
            Text(it, color = SoftRed, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
        }

        FinTextField(value = title, onValueChange = { title = it }, placeholder = "Title")
        Spacer(Modifier.height(12.dp))
        FinTextField(
            value = amount,
            onValueChange = { amount = it },
            placeholder = "Amount",
            numeric = true
        )
        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TypeChip("Debit", selected = !isCredit, tint = SoftRed) {
                isCredit = false
                category = Categories.debit.first()
            }
            TypeChip("Credit", selected = isCredit, tint = GreenCredit) {
                isCredit = true
                category = Categories.credit.first()
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("Category", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                TypeChip(cat, selected = cat == category, tint = VioletLight) { category = cat }
            }
        }
        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(VioletPrimary, AccentCyan)))
                .clickable(enabled = !saving) { onSubmit(title, amount, category, isCredit) },
            contentAlignment = Alignment.Center
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("Save Entry", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) tint.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .border(
                1.dp,
                if (selected) tint.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun LedgerRow(tx: TransactionDto, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (tx.isCredit) GreenCredit else SoftRed)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tx.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sourceTag = when (tx.source) {
                "bill_scan" -> "  ·  Scanned bill"
                "gmail_upi" -> "  ·  Gmail UPI"
                "sms" -> "  ·  Bank SMS"
                else -> ""
            }
            Text(
                tx.category + sourceTag,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            formatSignedAmount(tx),
            color = if (tx.isCredit) GreenCredit else SoftRed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete " + tx.title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatSignedAmount(tx: TransactionDto): String {
    val sign = if (tx.isCredit) "+" else "-"
    return sign + Fmt.rupees(tx.amount)
}

@Composable
private fun LedgerNotice(
    message: String,
    isError: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            message,
            color = if (isError) SoftRed else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Tap to retry",
                color = VioletLight,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

enum class TypeFilter(val label: String) { ALL("All"), OUT("Money out"), IN("Money in") }

enum class Period(val label: String) { ALL("All time"), THIS_MONTH("This month"), LAST_MONTH("Last month"), THREE_MONTHS("3 months") }

private fun filterLedger(
    all: List<TransactionDto>,
    query: String,
    type: TypeFilter,
    period: Period,
    category: String?,
): List<TransactionDto> {
    val q = query.trim().lowercase()
    val (from, until) = when (period) {
        Period.ALL -> 0L to Long.MAX_VALUE
        Period.THIS_MONTH -> Fmt.startOfMonth(0) to Long.MAX_VALUE
        Period.LAST_MONTH -> Fmt.startOfMonth(1) to Fmt.startOfMonth(0)
        Period.THREE_MONTHS -> Fmt.startOfMonth(2) to Long.MAX_VALUE
    }
    return all.filter { tx ->
        (type == TypeFilter.ALL || (type == TypeFilter.IN) == tx.isCredit) &&
            (category == null || tx.category == category) &&
            Fmt.millis(tx.date).let { it in from until until } &&
            (q.isEmpty() || listOfNotNull(tx.title, tx.merchant, tx.note, tx.category, tx.bankName)
                .any { it.lowercase().contains(q) } || "%.0f".format(tx.amount).contains(q))
    }
}

@Composable
private fun LedgerFilters(
    query: String, onQuery: (String) -> Unit,
    typeFilter: TypeFilter, onType: (TypeFilter) -> Unit,
    period: Period, onPeriod: (Period) -> Unit,
    category: String?, onCategory: (String?) -> Unit,
    categories: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FinTextField(value = query, onValueChange = onQuery, placeholder = "Search merchant, note, amount…")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeFilter.entries.forEach { t -> Chip(t.label, selected = typeFilter == t) { onType(t) } }
            Spacer(Modifier.width(4.dp))
            Period.entries.forEach { p -> Chip(p.label, selected = period == p, tint = AccentCyan) { onPeriod(p) } }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("All categories", selected = category == null, tint = GreenCredit) { onCategory(null) }
            categories.forEach { c -> Chip(c, selected = category == c, tint = GreenCredit) { onCategory(if (category == c) null else c) } }
        }
    }
}
