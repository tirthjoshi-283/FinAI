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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fin_ai.data.AdminUserDto
import com.example.fin_ai.ui.theme.AccentCyan
import com.example.fin_ai.ui.theme.GreenCredit
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.SoftRed
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.theme.VioletPrimary
import com.example.fin_ai.ui.viewmodel.AdminViewModel

@Composable
fun AdminScreen(viewModel: AdminViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    "Admin Console",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                )
                Text(
                    "Every registered FinAI account.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        when {
            state.loading -> item { AdminNotice("Loading users...") }
            state.error != null -> item {
                AdminNotice(state.error!!, isError = true, onRetry = viewModel::refresh)
            }
            else -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile("USERS", state.totalUsers.toString(), Modifier.weight(1f))
                        StatTile("ENTRIES", state.totalTransactions.toString(), Modifier.weight(1f))
                        StatTile("GOOGLE", state.googleUsers.toString(), Modifier.weight(1f))
                    }
                }
                item {
                    Text(
                        "All Registered Users",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(state.users, key = { it.uid }) { user -> AdminUserRow(user) }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(18.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = VioletLight,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun AdminUserRow(user: AdminUserDto) {
    val displayName = user.name?.takeIf { it.isNotBlank() } ?: "Unnamed"
    val initial = displayName.first().uppercaseChar().toString()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(VioletPrimary, AccentCyan))),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    user.email ?: "no email",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (user.isAdmin) {
                Badge("ADMIN", SoftRed)
            } else {
                Badge(user.provider?.uppercase() ?: "EMAIL", VioletLight)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            MetaColumn("Entries", (user.transactions?.count ?: 0).toString())
            MetaColumn("Logins", user.loginCount.toString())
            MetaColumn("Volume", formatCompact(user.transactions?.total ?: 0.0))
        }
    }
}

@Composable
private fun MetaColumn(label: String, value: String) {
    Column {
        Text(
            label.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = GreenCredit,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun Badge(text: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.15f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatCompact(amount: Double): String = when {
    amount >= 100_000 -> "₹" + "%.1fL".format(amount / 100_000)
    amount >= 1_000 -> "₹" + "%.1fK".format(amount / 1_000)
    else -> "₹" + "%.0f".format(amount)
}

@Composable
private fun AdminNotice(
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
