package com.example.fin_ai.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The signed-in user's transactions, shared by every screen (ledger, dashboard, analytics,
 * budgets, streaks, Wrapped, widget) so they stay in sync and the list is fetched once.
 */
object LedgerRepository {

    private val _transactions = MutableStateFlow<List<TransactionDto>>(emptyList())
    val transactions: StateFlow<List<TransactionDto>> = _transactions.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    @Volatile
    private var loaded = false
    private val mutex = Mutex()

    /** Loads once per session; later callers get the cached list. */
    suspend fun ensureLoaded() {
        if (!loaded) refresh()
    }

    suspend fun refresh() = mutex.withLock {
        _loading.value = true
        _error.value = null
        runCatching { ApiClient.api.getTransactions() }
            .onSuccess {
                _transactions.value = it.transactions.sortedByDescending(TransactionDto::date)
                loaded = true
            }
            .onFailure { _error.value = it.toUserMessage("Could not load transactions.") }
        _loading.value = false
    }

    fun add(tx: TransactionDto) = add(listOf(tx))

    fun add(txs: List<TransactionDto>) {
        if (txs.isEmpty()) return
        val ids = txs.map { it.id }.toSet()
        _transactions.value = (txs + _transactions.value.filterNot { it.id in ids }).sortedByDescending(TransactionDto::date)
    }

    fun update(tx: TransactionDto) {
        _transactions.value = _transactions.value.map { if (it.id == tx.id) tx else it }
    }

    fun remove(id: String) {
        _transactions.value = _transactions.value.filterNot { it.id == id }
    }

    fun replaceAll(list: List<TransactionDto>) {
        _transactions.value = list
    }

    /** On sign-out, so the next account never sees the previous one's data. */
    fun clear() {
        _transactions.value = emptyList()
        _error.value = null
        loaded = false
    }
}
