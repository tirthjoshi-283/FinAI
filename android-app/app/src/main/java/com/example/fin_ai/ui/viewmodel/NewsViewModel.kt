package com.example.fin_ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fin_ai.data.ApiClient
import com.example.fin_ai.data.NewsArticleDto
import com.example.fin_ai.data.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Same tabs (and server-side queries) as the web Finance News page. */
enum class NewsCategory(val label: String, val query: String) {
    TOP("Top Stories", "finance"),
    MARKETS("Markets", "stock market"),
    CRYPTO("Crypto", "cryptocurrency"),
    ECONOMY("Economy", "economy"),
    FINTECH("Fintech", "fintech"),
}

data class NewsUiState(
    val category: NewsCategory? = NewsCategory.TOP,
    /** Set when showing search results instead of a category tab. */
    val searchQuery: String? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val articles: List<NewsArticleDto> = emptyList(),
    val error: String? = null,
    val notice: String? = null,
)

class NewsViewModel : ViewModel() {

    private val _state = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        load(NewsCategory.TOP.query)
    }

    fun selectCategory(category: NewsCategory) {
        if (category == _state.value.category && _state.value.searchQuery == null) return
        _state.value = _state.value.copy(category = category, searchQuery = null)
        load(category.query)
    }

    fun search(text: String) {
        val q = text.trim()
        if (q.isEmpty()) return
        _state.value = _state.value.copy(category = null, searchQuery = q)
        load(q)
    }

    fun refresh() {
        val q = _state.value.searchQuery ?: _state.value.category?.query ?: return
        load(q, force = true)
    }

    private fun load(query: String, force: Boolean = false) {
        job?.cancel() // a newer tab/search always wins over an in-flight one
        job = viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = !force,
                refreshing = force,
                error = null,
                notice = null,
                articles = if (force) _state.value.articles else emptyList(),
            )
            runCatching { ApiClient.api.getNews(query, forceRefresh = force) }
                .onSuccess { res ->
                    val articles = res.articles.filter { it.title.isNotBlank() && it.url.isNotBlank() }
                    _state.value = _state.value.copy(
                        loading = false,
                        refreshing = false,
                        articles = articles,
                        error = if (articles.isEmpty()) res.error ?: res.message ?: "No news found. Try another topic." else null,
                        notice = if (res.cooldown) "Showing the latest headlines — refresh again in a minute." else null,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        refreshing = false,
                        error = it.toUserMessage("Couldn't load the news."),
                    )
                }
        }
    }
}
