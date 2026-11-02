package com.example.fin_ai.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** One icon + colour per category, shared by the ledger, detail screen and charts. */
fun categoryIconFor(category: String): ImageVector = when (category.lowercase()) {
    "food", "food & dining", "dining", "groceries" -> Icons.Filled.Fastfood
    "shopping" -> Icons.Filled.ShoppingBag
    "jewellery" -> Icons.Filled.Diamond
    "travel", "transport", "fuel" -> Icons.Filled.Flight
    "utilities", "bills & utilities" -> Icons.Filled.Bolt
    "health", "healthcare" -> Icons.Filled.LocalHospital
    "education" -> Icons.Filled.School
    "entertainment" -> Icons.Filled.Movie
    "rent", "housing" -> Icons.Filled.Home
    "taxes" -> Icons.Filled.Receipt
    "salary", "income" -> Icons.Filled.Work
    "freelance" -> Icons.Filled.LaptopMac
    "investments" -> Icons.Filled.ShowChart
    "refunds" -> Icons.Filled.Undo
    "gifts" -> Icons.Filled.CardGiftcard
    "other income" -> Icons.Filled.AccountBalance
    else -> Icons.Filled.CreditCard
}

private val palette = listOf(
    Color(0xFF8B5CF6), Color(0xFF22D3EE), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFFF43F5E),
    Color(0xFF3B82F6), Color(0xFFEC4899), Color(0xFF84CC16), Color(0xFFF97316), Color(0xFF14B8A6), Color(0xFFA1A1AA),
)

private val fixed = mapOf(
    "Food" to palette[2], "Shopping" to palette[0], "Travel" to palette[5], "Utilities" to palette[1],
    "Health" to palette[4], "Entertainment" to palette[6], "Rent" to palette[8], "Education" to palette[3],
    "Taxes" to palette[9], "Jewellery" to palette[7], "Miscellaneous" to palette[10],
)

fun categoryColor(category: String): Color =
    fixed[category] ?: palette[(category.hashCode() and Int.MAX_VALUE) % palette.size]
