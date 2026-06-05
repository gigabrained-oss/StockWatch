package com.example.mystockwatch.ui.chart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mystockwatch.data.prefs.ChartPrefs
import com.example.mystockwatch.domain.model.ChartLibrary
import com.example.mystockwatch.domain.model.Market
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** 캔들 1개 (시가, 고가, 저가, 종가) */
data class CandleEntry(
    val index: Int,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Long,
    val label: String,   // 날짜 레이블 (예: "06/03")
)

data class ChartUiState(
    val candles: List<CandleEntry> = emptyList(),
    val selectedCode: String = "",
    val selectedPeriod: Period = Period.ONE_MONTH,
    val chartLibrary: ChartLibrary = ChartLibrary.COMPOSE,
    val isLoading: Boolean = false,
)

enum class Period(val label: String) {
    ONE_DAY("1일"),
    ONE_WEEK("1주"),
    ONE_MONTH("1개월"),
    THREE_MONTHS("3개월"),
    ONE_YEAR("1년"),
}

class ChartViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = ChartPrefs(app)

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    init {
        // 저장된 차트 라이브러리 설정 불러오기
        viewModelScope.launch {
            prefs.chartLibrary.collect { lib ->
                _uiState.update { it.copy(chartLibrary = lib) }
            }
        }
        // TODO: 실제 API 연동 시 watchlist에서 첫 종목 자동 선택
        loadSampleData()
    }

    fun selectChartLibrary(lib: ChartLibrary) {
        viewModelScope.launch {
            prefs.setChartLibrary(lib)   // 자동으로 collect → uiState 갱신
        }
    }

    fun selectPeriod(period: Period) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadSampleData(period)
    }

    fun selectStock(code: String) {
        _uiState.update { it.copy(selectedCode = code) }
        loadSampleData()
    }

    private fun loadSampleData(period: Period = _uiState.value.selectedPeriod) {
        _uiState.update { it.copy(isLoading = true) }
        val count = when (period) {
            Period.ONE_DAY    -> 24
            Period.ONE_WEEK   -> 7
            Period.ONE_MONTH  -> 30
            Period.THREE_MONTHS -> 90
            Period.ONE_YEAR   -> 252
        }
        val candles = generateSampleCandles(count, period)
        _uiState.update { it.copy(candles = candles, isLoading = false) }
    }

    private fun generateSampleCandles(count: Int, period: Period): List<CandleEntry> {
        var price = 71_000f
        return (0 until count).map { i ->
            val change = (Math.random() * 2000 - 1000).toFloat()
            val open  = price
            val close = (price + change).coerceAtLeast(50_000f)
            val high  = maxOf(open, close) + (Math.random() * 500).toFloat()
            val low   = minOf(open, close) - (Math.random() * 500).toFloat()
            val vol   = (Math.random() * 15_000_000 + 3_000_000).toLong()
            price = close
            val label = when (period) {
                Period.ONE_DAY -> "%02d:00".format(i % 24)
                else           -> "%02d/%02d".format((i / 30) + 5, (i % 30) + 1)
            }
            CandleEntry(i, open, high, low, close, vol, label)
        }
    }
}
