package com.example.fin_ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fin_ai.data.LedgerRepository
import com.example.fin_ai.data.TransactionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val transactions: List<TransactionDto> = emptyList(),
) {
    val totalCredit: Double get() = transactions.filter { it.isCredit }.sumOf { it.amount }
    val totalDebit: Double get() = transactions.filterNot { it.isCredit }.sumOf { it.amount }
    val netWorth: Double get() = totalCredit - totalDebit
}

class DashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(LedgerRepository.transactions, LedgerRepository.error, LedgerRepository.loading) { txs, err, busy ->
                DashboardUiState(loading = busy && txs.isEmpty(), error = err.takeIf { txs.isEmpty() }, transactions = txs)
            }.collect { _state.value = it }
        }
        viewModelScope.launch { LedgerRepository.ensureLoaded() }
    }

    fun refresh() {
        viewModelScope.launch { LedgerRepository.refresh() }
    }
}
