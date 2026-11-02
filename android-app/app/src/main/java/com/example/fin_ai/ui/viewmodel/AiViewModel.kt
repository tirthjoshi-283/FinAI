package com.example.fin_ai.ui.viewmodel

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fin_ai.data.ApiClient
import com.example.fin_ai.data.BudgetRepository
import com.example.fin_ai.data.ChatRequest
import com.example.fin_ai.data.GoalDto
import com.example.fin_ai.data.InsightDto
import com.example.fin_ai.data.InsightsData
import com.example.fin_ai.data.LedgerRepository
import com.example.fin_ai.data.toUserMessage
import com.example.fin_ai.insights.Achievements
import com.example.fin_ai.insights.BudgetMath
import com.example.fin_ai.insights.BudgetStatus
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.util.income
import com.example.fin_ai.ui.util.isDebit
import com.example.fin_ai.ui.util.spend
import com.example.fin_ai.ui.util.thisMonth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    /** True only for a reply that just arrived — the UI types it out once. */
    val fresh: Boolean = false,
    val isError: Boolean = false,
)

/** One small fact shown under the health score ("Savings 12%", "1 budget over"). */
data class HealthFactor(val emoji: String, val label: String, val good: Boolean?)

data class AiUiState(
    val insightsLoading: Boolean = true,
    val insights: InsightsData? = null,
    val insightsError: String? = null,
    val factors: List<HealthFactor> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val chatLoading: Boolean = false,
    val suggestions: List<String> = DEFAULT_SUGGESTIONS,
)

private val DEFAULT_SUGGESTIONS = listOf(
    "Where am I overspending?",
    "Make me a monthly budget",
    "How can I save ₹5,000 this month?",
    "Summarise my last 30 days",
)

private val WELCOME = ChatMessage(
    id = 0, fromUser = false,
    text = "Hi! I'm your **FinAI advisor** ⚡ I can see your transactions, goals and budgets. " +
        "Ask me anything — or tap a suggestion below.",
)

class AiViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("finai_chat", android.content.Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _state = MutableStateFlow(AiUiState(messages = loadHistory()))
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    private var goals: List<GoalDto> = emptyList()

    init {
        loadInsights()
        viewModelScope.launch {
            LedgerRepository.ensureLoaded()
            BudgetRepository.ensureLoaded(getApplication())
            goals = runCatching { ApiClient.api.getGoals().goals }.getOrDefault(emptyList())
            combine(LedgerRepository.transactions, BudgetRepository.budgets) { txs, budgets -> txs to budgets }
                .collect { (txs, budgets) ->
                    _state.value = _state.value.copy(
                        factors = healthFactors(txs, BudgetMath.statuses(budgets, txs)),
                        suggestions = smartSuggestions(txs),
                    )
                }
        }
    }

    fun loadInsights() {
        viewModelScope.launch {
            _state.value = _state.value.copy(insightsLoading = true, insightsError = null)
            runCatching { ApiClient.api.getInsights() }
                .onSuccess { _state.value = _state.value.copy(insightsLoading = false, insights = it.data) }
                .onFailure {
                    _state.value = _state.value.copy(
                        insightsLoading = false,
                        insightsError = it.toUserMessage("The AI couldn't analyse your transactions right now."),
                    )
                }
        }
    }

    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _state.value.chatLoading) return
        val now = System.currentTimeMillis()
        publish(_state.value.messages.map { it.copy(fresh = false) } + ChatMessage(now, fromUser = true, text = message), loading = true)
        viewModelScope.launch {
            val reply = runCatching { ApiClient.api.chat(ChatRequest(message)).reply }
            val msg = reply.fold(
                onSuccess = { r ->
                    ChatMessage(now + 1, fromUser = false, text = r?.takeIf(String::isNotBlank) ?: "I couldn't come up with an answer. Try rephrasing.", fresh = true)
                },
                onFailure = { ChatMessage(now + 1, fromUser = false, text = it.toUserMessage("I couldn't reach the AI. Please try again."), isError = true) },
            )
            publish(_state.value.messages + msg, loading = false)
        }
    }

    /** "Explain this" on an insight card. */
    fun askAbout(insight: InsightDto) {
        send("Explain this insight using my own numbers and tell me exactly what to do: \"${insight.title}\" — ${insight.desc}")
    }

    fun typed(id: Long) {
        _state.value = _state.value.copy(messages = _state.value.messages.map { if (it.id == id) it.copy(fresh = false) else it })
    }

    fun clearChat() = publish(listOf(WELCOME), loading = false)

    private fun publish(messages: List<ChatMessage>, loading: Boolean) {
        _state.value = _state.value.copy(messages = messages, chatLoading = loading)
        // Keep the last 50 messages on the device (never on the server).
        prefs.edit { putString(KEY, gson.toJson(messages.takeLast(50).map { it.copy(fresh = false) })) }
    }

    private fun loadHistory(): List<ChatMessage> =
        prefs.getString(KEY, null)
            ?.let { runCatching { gson.fromJson<List<ChatMessage>>(it, object : TypeToken<List<ChatMessage>>() {}.type) }.getOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(WELCOME)

    private fun healthFactors(
        txs: List<com.example.fin_ai.data.TransactionDto>,
        budgets: List<BudgetStatus>,
    ): List<HealthFactor> {
        val month = txs.thisMonth()
        val income = month.income()
        val spent = month.spend()
        val out = mutableListOf<HealthFactor>()
        if (income > 0) {
            val rate = ((income - spent) / income * 100).toInt()
            out += HealthFactor("💰", if (rate >= 0) "Saving $rate%" else "Overspent", rate >= 20)
        } else if (spent > 0) {
            out += HealthFactor("💸", "${Fmt.rupees(spent)} spent", null)
        }
        month.filter { it.isDebit }.groupBy { it.category }.maxByOrNull { (_, l) -> l.sumOf { it.amount } }?.let { (cat, l) ->
            val share = if (spent > 0) (l.sumOf { it.amount } * 100 / spent).toInt() else 0
            out += HealthFactor("🏷️", "$cat $share%", share < 40)
        }
        if (budgets.isNotEmpty()) {
            val over = budgets.count { it.level == BudgetStatus.Level.OVER }
            out += HealthFactor("🎯", if (over == 0) "Budgets on track" else "$over budget${if (over == 1) "" else "s"} over", over == 0)
        }
        val streak = Achievements.currentNoSpendStreak(txs)
        if (streak > 0) out += HealthFactor("🔥", "$streak-day streak", true)
        return out
    }

    /** Suggestions that mention the user's own goals and top category. */
    private fun smartSuggestions(txs: List<com.example.fin_ai.data.TransactionDto>): List<String> {
        val out = mutableListOf("Where am I overspending?")
        goals.filter { !it.isComplete }.maxByOrNull { it.progress }?.let { out += "Am I on track for my ${it.title} goal?" }
        txs.thisMonth().filter { it.isDebit }.groupBy { it.category }.maxByOrNull { (_, l) -> l.sumOf { it.amount } }
            ?.let { (cat, _) -> out += "How can I cut my $cat spending?" }
        out += listOf("Make me a monthly budget", "How can I save ₹5,000 this month?", "Summarise my last 30 days")
        return out.distinct()
    }

    private companion object {
        const val KEY = "history"
    }
}
