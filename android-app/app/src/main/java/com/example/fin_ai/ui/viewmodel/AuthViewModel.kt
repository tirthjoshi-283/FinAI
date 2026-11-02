package com.example.fin_ai.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fin_ai.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val googleLoading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
)

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState(signedIn = repo.currentUser != null))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    // Firebase returns an empty string (not null) for an unset display name, so a
    // plain elvis chain would yield "" and render "Good morning ,".
    val displayName: String
        get() = repo.currentUser?.displayName?.substringBefore(' ')?.takeIf { it.isNotBlank() }
            ?: repo.currentUser?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: "there"

    /**
     * Mirrors the web app's admin check. This only decides whether to *offer* the
     * admin tab — the API independently verifies the caller's ID token against
     * ADMIN_EMAIL, so faking this client-side grants nothing.
     */
    val isAdmin: Boolean
        get() = repo.currentUser?.email?.equals(ADMIN_EMAIL, ignoreCase = true) == true

    private companion object {
        const val ADMIN_EMAIL = "admin@finai.com"
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Enter your email and password.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val result = runCatching { repo.signInWithEmail(email.trim(), password) }
            _state.value = result.fold(
                onSuccess = { AuthUiState(signedIn = true) },
                onFailure = { AuthUiState(error = friendlyError(it)) }
            )
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(googleLoading = true, error = null)
            val result = runCatching { repo.signInWithGoogle(context) }
            _state.value = result.fold(
                onSuccess = { AuthUiState(signedIn = true) },
                onFailure = { AuthUiState(error = friendlyError(it)) }
            )
        }
    }

    fun signOut() {
        repo.signOut()
        com.example.fin_ai.data.LedgerRepository.clear()
        _state.value = AuthUiState()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun friendlyError(t: Throwable): String = when {
        t.message?.contains("password is invalid", true) == true ||
            t.message?.contains("INVALID_LOGIN_CREDENTIALS", true) == true ->
            "Invalid email or password."
        t.message?.contains("no user record", true) == true ->
            "No account found with this email."
        t.message?.contains("network", true) == true ->
            "Network error. Is the dev server running?"
        t.message?.contains("cancel", true) == true ->
            "Sign-in cancelled."
        else -> t.message ?: "Something went wrong. Please try again."
    }
}
