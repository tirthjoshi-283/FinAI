package com.example.fin_ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fin_ai.data.AdminUserDto
import com.example.fin_ai.data.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val users: List<AdminUserDto> = emptyList(),
) {
    val totalUsers: Int get() = users.size
    val totalTransactions: Int get() = users.sumOf { it.transactions?.count ?: 0 }
    val googleUsers: Int get() = users.count { it.provider == "google" }
}

class AdminViewModel : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { ApiClient.api.getAdminUsers() }
                .onSuccess {
                    _state.value = AdminUiState(loading = false, users = it.users)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = it.message ?: "Could not load the user list.",
                    )
                }
        }
    }
}
