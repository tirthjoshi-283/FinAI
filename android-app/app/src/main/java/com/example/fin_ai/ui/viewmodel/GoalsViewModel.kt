package com.example.fin_ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fin_ai.data.ApiClient
import com.example.fin_ai.data.GoalDto
import com.example.fin_ai.data.LedgerRepository
import com.example.fin_ai.data.NewGoalRequest
import com.example.fin_ai.data.UpdateGoalRequest
import com.example.fin_ai.data.toUserMessage
import com.example.fin_ai.insights.GoalMath
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GoalsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val goals: List<GoalDto> = emptyList(),
    val saving: Boolean = false,
    val saveError: String? = null,
    /** Average monthly savings (income − spend) over the last 3 months, for projections. */
    val pace: Double = 0.0,
    /** Bumped after a successful change, so the edited card can "pop". */
    val lastChangedId: String? = null,
    val changeCount: Int = 0,
)

/** A milestone or completion to celebrate with confetti and a message. */
data class Celebration(val goal: GoalDto, val milestone: Int)

class GoalsViewModel : ViewModel() {

    private val _state = MutableStateFlow(GoalsUiState())
    val state: StateFlow<GoalsUiState> = _state.asStateFlow()

    private val _celebrations = MutableSharedFlow<Celebration>(extraBufferCapacity = 4)
    val celebrations: SharedFlow<Celebration> = _celebrations.asSharedFlow()

    init {
        refresh()
        viewModelScope.launch {
            LedgerRepository.ensureLoaded()
            LedgerRepository.transactions.collect { txs ->
                _state.value = _state.value.copy(pace = GoalMath.monthlySavingsPace(txs))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = _state.value.goals.isEmpty(), error = null)
            runCatching { ApiClient.api.getGoals() }
                .onSuccess { _state.value = _state.value.copy(loading = false, goals = it.goals) }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.toUserMessage("Could not load goals."))
                }
        }
    }

    fun createGoal(title: String, targetText: String, emoji: String?, deadline: String?, onDone: () -> Unit) {
        val target = targetText.toDoubleOrNull()
        when {
            title.isBlank() -> return setSaveError("Give the goal a name.")
            target == null || target <= 0 -> return setSaveError("Enter a target greater than 0.")
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, saveError = null)
            runCatching { ApiClient.api.addGoal(NewGoalRequest(title.trim(), target!!, emoji, deadline)) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        saving = false,
                        goals = listOf(it.goal) + _state.value.goals,
                        lastChangedId = it.goal.id,
                        changeCount = _state.value.changeCount + 1,
                    )
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, saveError = it.toUserMessage("Could not create the goal.")) }
        }
    }

    fun editGoal(goal: GoalDto, title: String, targetText: String, emoji: String?, deadline: String?, onDone: () -> Unit) {
        val target = targetText.toDoubleOrNull()
        when {
            title.isBlank() -> return setSaveError("Give the goal a name.")
            target == null || target <= 0 -> return setSaveError("Enter a target greater than 0.")
        }
        update(
            goal,
            UpdateGoalRequest(title = title.trim(), targetAmount = target, emoji = emoji ?: "", deadline = deadline ?: ""),
            onDone,
        )
    }

    /** Positive adds, negative withdraws. */
    fun changeFunds(goal: GoalDto, amount: Double, onDone: () -> Unit = {}) {
        if (amount == 0.0) return setSaveError("Enter an amount greater than 0.")
        update(goal, UpdateGoalRequest(amountToAdd = amount), onDone)
    }

    fun deleteGoal(goalId: String) {
        val previous = _state.value.goals
        _state.value = _state.value.copy(goals = previous.filterNot { it.id == goalId })
        viewModelScope.launch {
            runCatching { ApiClient.api.deleteGoal(goalId) }
                .onFailure { _state.value = _state.value.copy(goals = previous, error = it.toUserMessage("Could not delete the goal.")) }
        }
    }

    fun clearSaveError() {
        _state.value = _state.value.copy(saveError = null)
    }

    private fun update(goal: GoalDto, body: UpdateGoalRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, saveError = null)
            runCatching { ApiClient.api.updateGoal(goal.id, body) }
                .onSuccess { res ->
                    val updated = res.goal
                    _state.value = _state.value.copy(
                        saving = false,
                        goals = _state.value.goals.map { if (it.id == updated.id) updated else it },
                        lastChangedId = updated.id,
                        changeCount = _state.value.changeCount + 1,
                    )
                    GoalMath.milestoneCrossed(updated.targetAmount, goal.currentAmount, updated.currentAmount)
                        ?.let { _celebrations.tryEmit(Celebration(updated, it)) }
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, saveError = it.toUserMessage("Could not update the goal.")) }
        }
    }

    private fun setSaveError(message: String) {
        _state.value = _state.value.copy(saveError = message)
    }
}
