package com.example.fin_ai.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fin_ai.ui.theme.AccentCyan
import com.example.fin_ai.ui.theme.ErrorRed
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.example.fin_ai.ui.screens.AdminScreen
import com.example.fin_ai.ui.screens.AiScreen
import com.example.fin_ai.ui.screens.GoalsScreen
import com.example.fin_ai.ui.screens.DashboardScreen
import com.example.fin_ai.ui.screens.NewsScreen
import com.example.fin_ai.ui.screens.AnalyticsScreen
import com.example.fin_ai.ui.screens.BudgetsScreen
import com.example.fin_ai.ui.screens.SplitEditorScreen
import com.example.fin_ai.ui.screens.SplitsScreen
import com.example.fin_ai.ui.screens.StreaksScreen
import com.example.fin_ai.ui.screens.SubscriptionsScreen
import com.example.fin_ai.ui.screens.TransactionDetailScreen
import com.example.fin_ai.ui.screens.WrappedScreen
import com.example.fin_ai.ui.screens.TransactionsScreen
import com.example.fin_ai.ui.theme.VioletPrimary

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val baseTabs = listOf(
    BottomTab("dashboard", "Dashboard", Icons.Filled.Dashboard),
    BottomTab("transactions", "Transactions", Icons.Filled.ReceiptLong),
    BottomTab("goals", "Goals", Icons.Filled.TrackChanges),
    BottomTab("ai", "AI", Icons.Filled.AutoAwesome),
    BottomTab("news", "News", Icons.Filled.Newspaper),
)

private val adminTab = BottomTab("admin", "Admin", Icons.Filled.AdminPanelSettings)

/**
 * Six items do not fit a bottom bar legibly, so the admin account trades News for
 * the console. Everyone else sees the standard five.
 */
private fun tabsFor(isAdmin: Boolean): List<BottomTab> =
    if (isAdmin) baseTabs.dropLast(1) + adminTab else baseTabs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(firstName: String, isAdmin: Boolean, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Keep the home-screen widget and budget alerts in step with the ledger.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.example.fin_ai.data.BudgetRepository.ensureLoaded(context)
        kotlinx.coroutines.flow.combine(
            com.example.fin_ai.data.LedgerRepository.transactions,
            com.example.fin_ai.data.BudgetRepository.budgets,
        ) { txs, budgets -> txs to budgets }
            .collect { (txs, budgets) ->
                if (txs.isNotEmpty()) {
                    com.example.fin_ai.insights.BudgetAlerts.check(context, txs, budgets)
                    com.example.fin_ai.widget.WidgetData.update(context, txs, budgets)
                }
            }
    }
    val bottomTabs = tabsFor(isAdmin)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(VioletPrimary, AccentCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("FinAI", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out",
                            tint = ErrorRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            // Sub-screens (analytics, a transaction, ...) belong to the tab they were opened from.
            val currentTab = navController.currentBackStack.collectAsState().value
                .lastOrNull { e -> bottomTabs.any { it.route == e.destination.route } }
                ?.destination?.route ?: backStackEntry?.destination?.route

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 0.dp
            ) {
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab.route,
                        onClick = {
                            if (currentTab == tab.route) {
                                // Re-tapping the active tab returns to its top screen.
                                navController.popBackStack(tab.route, inclusive = false)
                                return@NavigationBarItem
                            }
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        // One line always: long labels shrink rather than wrapping ("Transactio/ns").
                        label = {
                            com.example.fin_ai.ui.util.AutoSizeText(
                                tab.label, fontSize = 12.sp, minFontSize = 9.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VioletPrimary,
                            selectedTextColor = VioletPrimary,
                            indicatorColor = VioletPrimary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            // The web app redirects an admin login straight to /admin; do the same
            // here rather than dropping them on a personal portfolio they never use.
            startDestination = if (isAdmin) "admin" else "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            val openTx: (String) -> Unit = { id -> navController.navigateSafely("transaction/$id") }
            composable("dashboard") {
                DashboardScreen(firstName = firstName, onOpen = openTx, onNavigate = { navController.navigateSafely(it) })
            }
            composable("transactions") { TransactionsScreen(onOpen = openTx) }
            composable("transaction/{id}") { entry ->
                TransactionDetailScreen(
                    id = entry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popSafely() },
                    onOpen = openTx,
                    onSplit = { id -> navController.navigateSafely("split/$id") },
                )
            }
            val back: () -> Unit = { navController.popSafely() }
            composable("analytics") { AnalyticsScreen(onBack = back, onOpen = openTx) }
            composable("budgets") { BudgetsScreen(onBack = back) }
            composable("subscriptions") { SubscriptionsScreen(onBack = back, onOpen = openTx) }
            composable("streaks") { StreaksScreen(onBack = back) }
            composable("wrapped") { WrappedScreen(onBack = back) }
            composable("splits") { SplitsScreen(onBack = back, onOpenTransaction = openTx) }
            composable("split/{id}") { entry ->
                SplitEditorScreen(transactionId = entry.arguments?.getString("id").orEmpty(), onBack = back)
            }
            composable("goals") { GoalsScreen() }
            composable("ai") { AiScreen() }
            composable("news") { NewsScreen() }
            composable("admin") { AdminScreen() }
        }
    }
}

/**
 * Navigation guards: ignore taps/back presses that arrive while a screen transition is
 * still running (double taps, back mid-animation), which otherwise can crash with
 * "Cannot transition entry that is not in the back stack".
 */
private fun androidx.navigation.NavController.isSettled(): Boolean =
    currentBackStackEntry?.lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.RESUMED

private fun androidx.navigation.NavController.navigateSafely(route: String) {
    if (isSettled()) navigate(route) { launchSingleTop = true }
}

private fun androidx.navigation.NavController.popSafely() {
    if (isSettled()) popBackStack()
}
