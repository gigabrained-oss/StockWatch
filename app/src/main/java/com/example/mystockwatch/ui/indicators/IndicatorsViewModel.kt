package com.example.mystockwatch.ui.indicators

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mystockwatch.data.api.yahooFinanceApi
import com.example.mystockwatch.data.db.AppDatabase
import com.example.mystockwatch.data.db.WatchlistEntity
import com.example.mystockwatch.domain.indicator.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class IndicatorsUiState(
    val watchlist: List<WatchlistEntity> = emptyList(),
    val selectedStock: WatchlistEntity? = null,
    val rsi: Double? = null,
    val macd: MacdResult? = null,
    val bollinger: BollingerResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class IndicatorsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).watchlistDao()

    private val _uiState = MutableStateFlow(IndicatorsUiState())
    val uiState: StateFlow<IndicatorsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dao.observeAll().collect { list ->
                _uiState.update { it.copy(watchlist = list) }
                // 자동으로 첫 종목 선택
                if (list.isNotEmpty() && _uiState.value.selectedStock == null) {
                    selectStock(list.first())
                }
            }
        }
    }

    fun selectStock(stock: WatchlistEntity) {
        _uiState.update { it.copy(selectedStock = stock, error = null) }
        loadIndicators(stock)
    }

    fun refresh() {
        _uiState.value.selectedStock?.let { loadIndicators(it) }
    }

    private fun loadIndicators(stock: WatchlistEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                // Yahoo Finance에서 3개월치 일봉 데이터 가져오기
                val symbol = if (stock.market == "KOREA") "${stock.code}.KS" else stock.code
                val chartData = yahooFinanceApi.getChart(
                    symbol   = symbol,
                    interval = "1d",
                    range    = "3mo",
                )
                val closes = chartData.chart?.result?.firstOrNull()
                    ?.indicators?.quote?.firstOrNull()
                    ?.close?.filterNotNull()
                    ?: error("가격 데이터 없음")

                if (closes.size < 30) error("데이터 부족 (${closes.size}일치)")

                _uiState.update {
                    it.copy(
                        rsi       = calculateRsi(closes),
                        macd      = calculateMacd(closes),
                        bollinger = calculateBollinger(closes),
                        isLoading = false,
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
