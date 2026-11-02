package com.example.fin_ai.data

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface FinApi {
    @GET("api/transactions")
    suspend fun getTransactions(): TransactionsResponse

    @POST("api/transactions")
    suspend fun addTransaction(@Body body: NewTransactionRequest): SingleTransactionResponse

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String)

    /** Edit category, title or note. */
    @PATCH("api/transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: String, @Body body: UpdateTransactionRequest)

    /**
     * Uploads a bill/receipt (JPEG/PNG/WEBP image or PDF, max 4 MB). The server reads
     * it with AI, saves it as a categorised debit and returns the new transaction.
     */
    @Multipart
    @POST("api/transactions/scan")
    suspend fun scanBill(@Part file: MultipartBody.Part): ScanBillResponse

    @GET("api/goals")
    suspend fun getGoals(): GoalsResponse

    @POST("api/goals")
    suspend fun addGoal(@Body body: NewGoalRequest): SingleGoalResponse

    @PATCH("api/goals/{id}")
    suspend fun updateGoal(@Path("id") id: String, @Body body: UpdateGoalRequest): SingleGoalResponse

    @DELETE("api/goals/{id}")
    suspend fun deleteGoal(@Path("id") id: String)

    /** AI analysis of the signed-in user's own transactions (read server-side). */
    @POST("api/ai/insights")
    suspend fun getInsights(@Body body: EmptyRequest = EmptyRequest()): InsightsResponse

    @POST("api/chat")
    suspend fun chat(@Body body: ChatRequest): ChatResponse

    /** Finance headlines (GNews, cached 15 min server-side). Public, no login needed. */
    /** Bank-SMS transactions parsed and verified on the phone (fields only, never SMS text). */
    @POST("api/transactions/sms")
    suspend fun importSms(@Body body: SmsImportRequest): SmsImportResponse

    /** The mobile number whose bank SMS this account imports (one account per number). */
    @GET("api/users/sms-phone")
    suspend fun getSmsPhone(): SmsPhoneResponse

    @PUT("api/users/sms-phone")
    suspend fun linkSmsPhone(@Body body: SmsPhoneRequest): SmsPhoneResponse

    @DELETE("api/users/sms-phone")
    suspend fun unlinkSmsPhone(): SmsPhoneResponse

    // ---- Budgets ----
    @GET("api/budgets")
    suspend fun getBudgets(): BudgetsResponse

    @PUT("api/budgets")
    suspend fun setBudget(@Body body: BudgetDto): SetBudgetResponse

    @DELETE("api/budgets")
    suspend fun deleteBudget(@Query("category") category: String)

    // ---- Split bills ----
    @GET("api/splits")
    suspend fun getSplits(): SplitsResponse

    @POST("api/splits")
    suspend fun saveSplit(@Body body: SaveSplitRequest): SplitResponse

    @PATCH("api/splits/{id}")
    suspend fun settleSplit(@Path("id") id: String, @Body body: SettleRequest): SplitResponse

    @DELETE("api/splits/{id}")
    suspend fun deleteSplit(@Path("id") id: String)

    @GET("api/news")
    suspend fun getNews(
        @Query("q") query: String,
        @Query("count") count: Int = 10,
        @Query("forceRefresh") forceRefresh: Boolean = false,
    ): NewsResponse

    @POST("api/users/sync")
    suspend fun syncUser(@Body body: SyncUserRequest)

    /**
     * Admin console data. Authorised by the caller's own Firebase ID token, which
     * the API checks against ADMIN_EMAIL — deliberately not by a shared secret,
     * which could be extracted from the APK.
     */
    @GET("api/admin/users")
    suspend fun getAdminUsers(): AdminUsersResponse
}
