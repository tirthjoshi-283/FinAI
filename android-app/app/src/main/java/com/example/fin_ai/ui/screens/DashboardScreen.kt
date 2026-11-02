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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.ui.util.AutoSizeText
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.example.fin_ai.ui.theme.AccentCyan
import com.example.fin_ai.ui.theme.GreenCredit
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.SoftRed
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.theme.VioletPrimary

@Composable
fun DashboardScreen(
    firstName: String,
    onOpen: (String) -> Unit = {},
    onNavigate: (String) -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { DashboardHeader(firstName) }

        when {
            state.loading -> item { StatusBlock("Loading your ledger…") }
            state.error != null -> item {
                StatusBlock(
                    message = state.error!!,
                    isError = true,
                    onRetry = viewModel::refresh
                )
            }
            else -> {
                item { NetWorthHeroCard(state.netWorth, onAnalytics = { onNavigate("analytics") }, onLedger = { onNavigate("transactions") }) }
                item { ExploreSection(state.transactions, onNavigate) }
                item { LiquidAssetsCard(state.totalCredit) }
                item { LiabilitiesCard(state.totalDebit) }
                item {
                    Text(
                        "Recent Activity",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                if (state.transactions.isEmpty()) {
                    item { StatusBlock("No transactions yet. Add one on the web app to see it here.") }
                } else {
                    // Just the latest few; the full history lives in the ledger.
                    items(state.transactions.take(5), key = { it.id }) { tx -> TransactionRow(tx) { onOpen(tx.id) } }
                    if (state.transactions.size > 5) {
                        item {
                            Text(
                                "View all ${state.transactions.size} transactions →",
                                color = VioletLight,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onNavigate("transactions") }
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun StatusBlock(
    message: String,
    isError: Boolean = false,
    onRetry: (() -> Unit)? = null
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

@Composable
private fun DashboardHeader(firstName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Portfolio Overview",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            )
            Text(
                "Good morning $firstName, your wealth ecosystem is looking vibrant.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(VioletPrimary, VioletLight))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                firstName.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NetWorthHeroCard(totalBalance: Double, onAnalytics: () -> Unit, onLedger: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(28.dp))
            .padding(24.dp)
    ) {
        Text(
            "TOTAL NET WORTH",
            color = VioletLight,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(8.dp))
        AutoSizeText(
            Fmt.rupees(totalBalance),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 38.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(VioletLight.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = VioletLight, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("All time", color = VioletLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(8.dp))
            Text("Cashflow balance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(onClick = onAnalytics)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("Analytics", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onLedger)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("View Ledger", color = VioletPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun LiquidAssetsCard(totalCredit: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "LIQUID ASSETS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = VioletPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        AutoSizeText(
            Fmt.rupees(totalCredit),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(VioletPrimary, AccentCyan)))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Representing total lifetime credits",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun LiabilitiesCard(totalDebit: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TOTAL LIABILITIES",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CreditCard, contentDescription = null, tint = SoftRed, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        AutoSizeText(
            Fmt.rupees(totalDebit),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TrendingDown, contentDescription = null, tint = SoftRed, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Total Spends", color = SoftRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            Text("recorded historically", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                categoryIcon(tx.category),
                contentDescription = null,
                tint = if (tx.isCredit) GreenCredit else SoftRed,
                modifier = Modifier.size(18.dp)
            )
        }
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
            Text(
                listOfNotNull(formatDate(tx.date), tx.merchant).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            Fmt.signed(tx),
            color = if (tx.isCredit) GreenCredit else SoftRed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

private fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "food", "dining", "groceries" -> Icons.Filled.Fastfood
    "rent", "housing", "home" -> Icons.Filled.Home
    "shopping" -> Icons.Filled.ShoppingBag
    "salary", "income" -> Icons.Filled.Work
    else -> Icons.Filled.CreditCard
}



/** API dates are ISO-8601 (e.g. 2026-08-05T00:00:00.000Z); show just "05 Aug". */
private fun formatDate(iso: String): String = runCatching {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
    val parsed = parser.parse(iso.take(19)) ?: return iso.take(10)
    SimpleDateFormat("dd MMM", Locale.getDefault()).format(parsed)
}.getOrElse { iso.take(10) }
