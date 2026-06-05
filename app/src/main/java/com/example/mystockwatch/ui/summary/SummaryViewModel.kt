package com.example.mystockwatch.ui.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mystockwatch.data.api.naverFinanceApi
import com.example.mystockwatch.data.api.yahooFinanceApi
import com.example.mystockwatch.data.db.AppDatabase
import com.example.mystockwatch.data.db.WatchlistEntity
import com.example.mystockwatch.data.repository.StockRepository
import com.example.mystockwatch.domain.model.Market
import com.example.mystockwatch.domain.model.StockQuote
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SummaryUiState(
    val quotes: List<StockQuote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class SummaryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = StockRepository(
        dao = AppDatabase.getInstance(app).watchlistDao(),
        naverApi = naverFinanceApi,
        yahooApi = yahooFinanceApi,
    )

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeWatchlist().collect { watchlist ->
                refreshQuotes(watchlist)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.observeWatchlist().first().let { refreshQuotes(it) }
        }
    }

    fun addStock(code: String, name: String, market: Market) {
        viewModelScope.launch {
            repository.addStock(code.trim().uppercase(), name.trim(), market)
        }
    }

    fun removeStock(code: String) {
        viewModelScope.launch {
            repository.removeStock(code)
        }
    }

    private suspend fun refreshQuotes(watchlist: List<WatchlistEntity>) {
        if (watchlist.isEmpty()) {
            _uiState.value = SummaryUiState()
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        val quotes = watchlist.map { entity ->
            runCatching { repository.fetchQuote(entity) }.getOrElse { e ->
                // 가격 조회 실패 시 이전 데이터 유지하고 에러 표시
                _uiState.update { it.copy(error = "${entity.code}: ${e.message}") }
                null
            }
        }.filterNotNull()
        _uiState.update { it.copy(quotes = quotes, isLoading = false) }
    }
}
