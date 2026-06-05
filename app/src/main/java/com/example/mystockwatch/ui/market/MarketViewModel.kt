package com.example.mystockwatch.ui.market

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mystockwatch.data.api.YahooQuote
import com.example.mystockwatch.data.api.yahooFinanceApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketItem(
    val symbol: String,
    val name: String,
    val price: Double,
    val changeValue: Double,
    val changeRate: Double,
    val isUp: Boolean,
)

enum class FxChartPeriod(val label: String, val interval: String, val range: String) {
    H24("24H", "1h",  "1d"),
    M1 ("1M",  "1d",  "1mo"),
    Y1 ("1Y",  "1wk", "1y"),
}

data class MarketUiState(
    val exchangeRate: MarketItem? = null,
    val fxChartPoints: List<Float> = emptyList(),
    val fxChartPeriod: FxChartPeriod = FxChartPeriod.M1,
    val isFxChartLoading: Boolean = false,
    val indices: List<MarketItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

private val INDEX_SYMBOLS = listOf("^KS11", "^GSPC", "^IXIC")
private val INDEX_NAMES   = mapOf(
    "^KS11" to "KOSPI",
    "^GSPC" to "S&P 500",
    "^IXIC" to "NASDAQ",
)
private const val FX_SYMBOL = "KRW=X"

class MarketViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val symbols = (listOf(FX_SYMBOL) + INDEX_SYMBOLS).joinToString(",")
                val quotes  = yahooFinanceApi.getQuote(symbols)
                    .quoteResponse?.result.orEmpty()

                val fxQuote  = quotes.firstOrNull { it.symbol == FX_SYMBOL }
                val idxQuotes = INDEX_SYMBOLS.mapNotNull { sym ->
                    quotes.firstOrNull { it.symbol == sym }
                }
                _uiState.update {
                    it.copy(
                        exchangeRate = fxQuote?.toMarketItem("USD/KRW"),
                        indices      = idxQuotes.map { q ->
                            q.toMarketItem(INDEX_NAMES[q.symbol] ?: q.symbol)
                        },
                        isLoading = false,
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
        loadFxChart(_uiState.value.fxChartPeriod)
    }

    fun selectFxChartPeriod(period: FxChartPeriod) {
        _uiState.update { it.copy(fxChartPeriod = period) }
        loadFxChart(period)
    }

    private fun loadFxChart(period: FxChartPeriod) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFxChartLoading = true) }
            runCatching {
                val data = yahooFinanceApi.getChart(
                    symbol   = FX_SYMBOL,
                    interval = period.interval,
                    range    = period.range,
                )
                val closes = data.chart?.result?.firstOrNull()
                    ?.indicators?.quote?.firstOrNull()
                    ?.close?.filterNotNull()?.map { it.toFloat() }
                    .orEmpty()
                _uiState.update { it.copy(fxChartPoints = closes, isFxChartLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isFxChartLoading = false) }
            }
        }
    }

    private fun YahooQuote.toMarketItem(displayName: String) = MarketItem(
        symbol      = symbol,
        name        = displayName,
        price       = price,
        changeValue = changeValue,
        changeRate  = changeRate,
        isUp        = changeRate >= 0,
    )
}
