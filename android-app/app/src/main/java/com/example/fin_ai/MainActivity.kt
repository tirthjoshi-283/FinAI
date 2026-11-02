package com.example.fin_ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.fin_ai.ui.nav.MainScaffold
import com.example.fin_ai.ui.screens.LoginScreen
import com.example.fin_ai.ui.theme.FinAITheme
import com.example.fin_ai.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinAITheme {
                FinAIApp()
            }
        }
    }
}

@Composable
fun FinAIApp(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val authState by authViewModel.state.collectAsState()

    // A cached Firebase session survives app restarts, so skip the login screen.
    val startDestination = if (authState.signedIn) "main" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScaffold(
                firstName = authViewModel.displayName,
                isAdmin = authViewModel.isAdmin,
                onSignOut = {
                    authViewModel.signOut()
                    // Nothing from this account may linger for the next one.
                    com.example.fin_ai.data.BudgetRepository.clear(context)
                    com.example.fin_ai.data.SplitRepository.clear()
                    context.getSharedPreferences("finai_chat", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                    scope.launch { com.example.fin_ai.widget.WidgetData.clear(context) }
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}
