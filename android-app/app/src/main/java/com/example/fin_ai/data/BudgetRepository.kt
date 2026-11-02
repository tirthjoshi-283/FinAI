package com.example.fin_ai.data

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monthly category budgets, synced with /api/budgets. A copy is cached on the device so
 * background work (SMS capture, the home-screen widget) can check budgets without a UI.
 */
object BudgetRepository {

    private val _budgets = MutableStateFlow<List<BudgetDto>>(emptyList())
    val budgets: StateFlow<List<BudgetDto>> = _budgets.asStateFlow()

    @Volatile
    private var loaded = false

    suspend fun ensureLoaded(context: Context) {
        if (!loaded) refresh(context)
    }

    suspend fun refresh(context: Context): Result<Unit> = runCatching {
        val list = ApiClient.api.getBudgets().budgets
        publish(context, list)
        loaded = true
    }

    suspend fun set(context: Context, category: String, limit: Double): Result<Unit> = runCatching {
        val saved = ApiClient.api.setBudget(BudgetDto(category, limit)).budget
        publish(context, (_budgets.value.filterNot { it.category == category } + saved).sortedBy { it.category })
    }

    suspend fun delete(context: Context, category: String): Result<Unit> = runCatching {
        ApiClient.api.deleteBudget(category)
        publish(context, _budgets.value.filterNot { it.category == category })
    }

    /** Last known budgets, usable from background work without a network call. */
    fun cached(context: Context): List<BudgetDto> =
        prefs(context).getString(KEY, null)?.let {
            runCatching { Gson().fromJson<List<BudgetDto>>(it, object : TypeToken<List<BudgetDto>>() {}.type) }.getOrNull()
        }.orEmpty()

    fun clear(context: Context) {
        _budgets.value = emptyList()
        loaded = false
        prefs(context).edit { remove(KEY) }
    }

    private fun publish(context: Context, list: List<BudgetDto>) {
        _budgets.value = list
        prefs(context).edit { putString(KEY, Gson().toJson(list)) }
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("finai_budgets", Context.MODE_PRIVATE)
    private const val KEY = "budgets"
}
