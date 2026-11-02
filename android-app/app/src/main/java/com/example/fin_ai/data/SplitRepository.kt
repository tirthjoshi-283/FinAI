package com.example.fin_ai.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Bills the user split with friends, synced with /api/splits. */
object SplitRepository {

    private val _splits = MutableStateFlow<List<SplitDto>>(emptyList())
    val splits: StateFlow<List<SplitDto>> = _splits.asStateFlow()

    @Volatile
    private var loaded = false

    suspend fun ensureLoaded() {
        if (!loaded) refresh()
    }

    suspend fun refresh(): Result<Unit> = runCatching {
        _splits.value = ApiClient.api.getSplits().splits
        loaded = true
    }

    suspend fun save(transactionId: String, people: List<SplitPersonDto>): Result<SplitDto> = runCatching {
        val split = ApiClient.api.saveSplit(SaveSplitRequest(transactionId, people)).split
        _splits.value = listOf(split) + _splits.value.filterNot { it.id == split.id }
        split
    }

    suspend fun settle(split: SplitDto, name: String, settled: Boolean): Result<Unit> = runCatching {
        val updated = ApiClient.api.settleSplit(split.id, SettleRequest(name, settled)).split
        _splits.value = _splits.value.map { if (it.id == updated.id) updated else it }
    }

    suspend fun delete(split: SplitDto): Result<Unit> = runCatching {
        ApiClient.api.deleteSplit(split.id)
        _splits.value = _splits.value.filterNot { it.id == split.id }
    }

    fun forTransaction(id: String): SplitDto? = _splits.value.firstOrNull { it.transactionId == id }

    fun clear() {
        _splits.value = emptyList()
        loaded = false
    }
}
