package com.example.fin_ai.data

import com.google.gson.annotations.SerializedName

data class TransactionDto(
    @SerializedName("id") val id: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String,
    @SerializedName("category") val category: String,
    @SerializedName("title") val title: String,
    @SerializedName("date") val date: String,
    @SerializedName("source") val source: String? = null,
    @SerializedName("merchant") val merchant: String? = null,
    @SerializedName("bankName") val bankName: String? = null,
    @SerializedName("upiRef") val upiRef: String? = null,
    @SerializedName("accountLast4") val accountLast4: String? = null,
    @SerializedName("note") val note: String? = null,
) {
    val isCredit: Boolean get() = type == "credit"
}

data class TransactionsResponse(
    @SerializedName("transactions") val transactions: List<TransactionDto>
)

data class NewTransactionRequest(
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String,
    @SerializedName("category") val category: String,
    @SerializedName("title") val title: String,
    @SerializedName("date") val date: String,
)

data class SingleTransactionResponse(
    @SerializedName("transaction") val transaction: TransactionDto
)

/** Any subset of fields; nulls are omitted from the JSON. Send note = "" to clear it. */
data class UpdateTransactionRequest(
    @SerializedName("category") val category: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("note") val note: String? = null,
)

data class ScanBillResponse(
    @SerializedName("transaction") val transaction: TransactionDto,
    @SerializedName("summary") val summary: String? = null,
)

data class SyncUserRequest(
    @SerializedName("uid") val uid: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("provider") val provider: String,
)

/** One row of the admin console's user table. */
data class AdminUserDto(
    @SerializedName("uid") val uid: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("provider") val provider: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("loginHistory") val loginHistory: List<AdminLoginEvent>? = null,
    @SerializedName("transactions") val transactions: AdminTxSummary? = null,
) {
    val isAdmin: Boolean get() = role == "admin"
    val loginCount: Int get() = loginHistory?.size ?: 0
}

data class AdminLoginEvent(
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("device") val device: String? = null,
)

data class AdminTxSummary(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("total") val total: Double = 0.0,
)

data class AdminUsersResponse(
    @SerializedName("users") val users: List<AdminUserDto>
)

// ---- Goals -------------------------------------------------------------------

data class GoalDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("targetAmount") val targetAmount: Double,
    @SerializedName("currentAmount") val currentAmount: Double,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("emoji") val emoji: String? = null,
    /** ISO date the user wants to reach the goal by, if set. */
    @SerializedName("deadline") val deadline: String? = null,
) {
    val progress: Float
        get() = if (targetAmount <= 0) 0f else (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f)
    val isComplete: Boolean get() = currentAmount >= targetAmount
}

data class GoalsResponse(@SerializedName("goals") val goals: List<GoalDto>)

data class SingleGoalResponse(@SerializedName("goal") val goal: GoalDto)

data class NewGoalRequest(
    @SerializedName("title") val title: String,
    @SerializedName("targetAmount") val targetAmount: Double,
    @SerializedName("emoji") val emoji: String? = null,
    @SerializedName("deadline") val deadline: String? = null,
)

/** Any subset; nulls are omitted. amountToAdd < 0 withdraws. deadline = "" clears it. */
data class UpdateGoalRequest(
    @SerializedName("amountToAdd") val amountToAdd: Double? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("targetAmount") val targetAmount: Double? = null,
    @SerializedName("emoji") val emoji: String? = null,
    @SerializedName("deadline") val deadline: String? = null,
)

// ---- AI ----------------------------------------------------------------------

/** Serialises to `{}` for POST endpoints that take no input. */
class EmptyRequest

data class InsightDto(
    @SerializedName("type") val type: String? = null, // "warning" | "opportunity" | "tip"
    @SerializedName("title") val title: String? = null,
    @SerializedName("desc") val desc: String? = null,
)

data class InsightsData(
    @SerializedName("healthScore") val healthScore: Int? = null,
    @SerializedName("insightText") val insightText: String? = null,
    @SerializedName("insights") val insights: List<InsightDto>? = null,
)

data class InsightsResponse(@SerializedName("data") val data: InsightsData)

data class ChatRequest(@SerializedName("message") val message: String)

data class ChatResponse(@SerializedName("reply") val reply: String? = null)

// ---- News --------------------------------------------------------------------

data class NewsArticleDto(
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("image") val image: String = "",
    @SerializedName("publishedAt") val publishedAt: String = "",
    @SerializedName("source") val source: String = "",
)

data class NewsResponse(
    @SerializedName("articles") val articles: List<NewsArticleDto> = emptyList(),
    @SerializedName("error") val error: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("cooldown") val cooldown: Boolean = false,
)

// ---- Bank SMS auto-capture -----------------------------------------------------

data class SmsTransactionDto(
    @SerializedName("hash") val hash: String,
    @SerializedName("type") val type: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("date") val date: String,
    @SerializedName("bankName") val bankName: String,
    @SerializedName("merchant") val merchant: String? = null,
    @SerializedName("upiRef") val upiRef: String? = null,
    @SerializedName("accountLast4") val accountLast4: String? = null,
)

data class SmsImportRequest(@SerializedName("transactions") val transactions: List<SmsTransactionDto>)

data class SmsImportResponse(
    @SerializedName("imported") val imported: Int = 0,
    @SerializedName("duplicates") val duplicates: Int = 0,
    @SerializedName("invalid") val invalid: Int = 0,
    /** Already imported by a different account (a second login on the same phone). */
    @SerializedName("otherAccount") val otherAccount: Int = 0,
    @SerializedName("transactions") val transactions: List<TransactionDto> = emptyList(),
)

data class SmsPhoneRequest(@SerializedName("phone") val phone: String)

data class SmsPhoneResponse(@SerializedName("phone") val phone: String? = null)

// ---- Budgets -----------------------------------------------------------------

data class BudgetDto(
    @SerializedName("category") val category: String,
    @SerializedName("limit") val limit: Double,
)

data class BudgetsResponse(@SerializedName("budgets") val budgets: List<BudgetDto> = emptyList())

data class SetBudgetResponse(@SerializedName("budget") val budget: BudgetDto)

// ---- Split bills ---------------------------------------------------------------

data class SplitPersonDto(
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("settled") val settled: Boolean = false,
)

data class SplitDto(
    @SerializedName("id") val id: String,
    @SerializedName("transactionId") val transactionId: String,
    @SerializedName("title") val title: String,
    @SerializedName("total") val total: Double,
    @SerializedName("people") val people: List<SplitPersonDto> = emptyList(),
    @SerializedName("createdAt") val createdAt: String? = null,
) {
    val outstanding: Double get() = people.filterNot { it.settled }.sumOf { it.amount }
    val myShare: Double get() = (total - people.sumOf { it.amount }).coerceAtLeast(0.0)
}

data class SplitsResponse(@SerializedName("splits") val splits: List<SplitDto> = emptyList())

data class SplitResponse(@SerializedName("split") val split: SplitDto)

data class SaveSplitRequest(
    @SerializedName("transactionId") val transactionId: String,
    @SerializedName("people") val people: List<SplitPersonDto>,
)

data class SettleRequest(@SerializedName("name") val name: String, @SerializedName("settled") val settled: Boolean)
