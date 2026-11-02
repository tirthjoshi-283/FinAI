package com.example.fin_ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fin_ai.data.ApiClient
import com.example.fin_ai.data.LedgerRepository
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.data.UpdateTransactionRequest
import com.example.fin_ai.data.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(val saving: Boolean = false, val message: String? = null, val deleted: Boolean = false)

/** Edits for the transaction detail screen; the transaction itself is read from [LedgerRepository]. */
class TransactionDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { LedgerRepository.ensureLoaded() }
    }

    fun setCategory(tx: TransactionDto, category: String) {
        if (category == tx.category) return
        save(tx, UpdateTransactionRequest(category = category), tx.copy(category = category), "Category updated")
    }

    fun saveNote(tx: TransactionDto, note: String) {
        val clean = note.trim()
        if (clean == tx.note.orEmpty()) return
        save(tx, UpdateTransactionRequest(note = clean), tx.copy(note = clean.ifEmpty { null }), if (clean.isEmpty()) "Note removed" else "Note saved")
    }

    fun delete(tx: TransactionDto) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            runCatching { ApiClient.api.deleteTransaction(tx.id) }
                .onSuccess {
                    LedgerRepository.remove(tx.id)
                    _state.value = DetailUiState(deleted = true)
                }
                .onFailure { _state.value = DetailUiState(message = it.toUserMessage("Couldn't delete it.")) }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun save(old: TransactionDto, body: UpdateTransactionRequest, updated: TransactionDto, done: String) {
        LedgerRepository.update(updated) // optimistic
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            runCatching { ApiClient.api.updateTransaction(old.id, body) }
                .onSuccess { _state.value = DetailUiState(message = done) }
                .onFailure {
                    LedgerRepository.update(old)
                    _state.value = DetailUiState(message = it.toUserMessage("Couldn't save the change."))
                }
        }
    }
}
