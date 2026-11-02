package com.example.fin_ai.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fin_ai.data.ApiClient
import com.example.fin_ai.data.BillUpload
import com.example.fin_ai.data.LedgerRepository
import com.example.fin_ai.data.NewTransactionRequest
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.data.UpdateTransactionRequest
import com.example.fin_ai.data.toUserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The entry a bill scan just created, shown so the user can fix the category or undo it. */
data class ScanResult(val transaction: TransactionDto, val summary: String)

data class TransactionsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val transactions: List<TransactionDto> = emptyList(),
    val saving: Boolean = false,
    val saveError: String? = null,
    val scanning: Boolean = false,
    val scanError: String? = null,
    val scanResult: ScanResult? = null,
)

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(TransactionsUiState())
    val state: StateFlow<TransactionsUiState> = _state.asStateFlow()

    init {
        // The shared ledger is the source of truth; this screen mirrors it.
        viewModelScope.launch {
            LedgerRepository.transactions.collect { _state.value = _state.value.copy(transactions = it) }
        }
        viewModelScope.launch {
            LedgerRepository.error.collect { _state.value = _state.value.copy(error = it) }
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            LedgerRepository.ensureLoaded()
            _state.value = _state.value.copy(loading = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = _state.value.transactions.isEmpty())
            LedgerRepository.refresh()
            _state.value = _state.value.copy(loading = false)
        }
    }

    fun addTransaction(
        title: String,
        amountText: String,
        category: String,
        isCredit: Boolean,
        onDone: () -> Unit,
    ) {
        val amount = amountText.toDoubleOrNull()
        when {
            title.isBlank() -> {
                _state.value = _state.value.copy(saveError = "Give the entry a title.")
                return
            }
            amount == null || amount <= 0 -> {
                _state.value = _state.value.copy(saveError = "Enter an amount greater than 0.")
                return
            }
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, saveError = null)
            val body = NewTransactionRequest(
                amount = amount!!,
                type = if (isCredit) "credit" else "debit",
                category = category,
                title = title.trim(),
                date = isoNow(),
            )
            runCatching { ApiClient.api.addTransaction(body) }
                .onSuccess { response ->
                    LedgerRepository.add(response.transaction)
                    _state.value = _state.value.copy(saving = false, saveError = null)
                    onDone()
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        saving = false,
                        saveError = it.toUserMessage("Could not save that entry."),
                    )
                }
        }
    }

    fun deleteTransaction(id: String) {
        // Optimistic removal; restore the previous list if the API rejects it.
        val previous = _state.value.transactions
        LedgerRepository.remove(id)
        viewModelScope.launch {
            runCatching { ApiClient.api.deleteTransaction(id) }
                .onFailure {
                    LedgerRepository.replaceAll(previous)
                    _state.value = _state.value.copy(error = it.toUserMessage("Could not delete that entry."))
                }
        }
    }

    /** Uploads a bill photo or PDF; the server reads it with AI and saves a categorised debit. */
    fun scanBill(uri: Uri) {
        if (_state.value.scanning) return
        viewModelScope.launch {
            _state.value = _state.value.copy(scanning = true, scanError = null, scanResult = null)
            runCatching {
                val part = withContext(Dispatchers.IO) {
                    BillUpload.prepare(getApplication<Application>().contentResolver, uri)
                }
                ApiClient.api.scanBill(part)
            }
                .onSuccess { response ->
                    val tx = response.transaction
                    LedgerRepository.add(tx)
                    _state.value = _state.value.copy(scanning = false, scanResult = ScanResult(tx, response.summary.orEmpty()))
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        scanning = false,
                        scanError = if (it is BillUpload.UnsupportedBillException) it.message
                        else it.toUserMessage("Couldn't read that bill. Try a clearer photo."),
                    )
                }
        }
    }

    fun changeScannedCategory(category: String) {
        val result = _state.value.scanResult ?: return
        val id = result.transaction.id
        viewModelScope.launch {
            runCatching { ApiClient.api.updateTransaction(id, UpdateTransactionRequest(category = category)) }
                .onSuccess {
                    val updated = result.transaction.copy(category = category)
                    LedgerRepository.update(updated)
                    _state.value = _state.value.copy(scanResult = result.copy(transaction = updated))
                }
                .onFailure {
                    _state.value = _state.value.copy(scanError = it.toUserMessage("Couldn't change the category."))
                }
        }
    }

    fun undoScan() {
        val result = _state.value.scanResult ?: return
        _state.value = _state.value.copy(scanResult = null)
        deleteTransaction(result.transaction.id)
    }

    fun dismissScan() {
        _state.value = _state.value.copy(scanResult = null, scanError = null)
    }

    fun clearSaveError() {
        _state.value = _state.value.copy(saveError = null)
    }

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}
