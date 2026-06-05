package com.example.mystockwatch.ui.alerts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mystockwatch.data.db.AlertEntity
import com.example.mystockwatch.data.db.AlertType
import com.example.mystockwatch.data.db.AppDatabase
import com.example.mystockwatch.data.db.WatchlistEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AlertsUiState(
    val watchlist: List<WatchlistEntity> = emptyList(),
    // 종목 코드별 알림 목록
    val alertsByStock: Map<String, List<AlertEntity>> = emptyMap(),
)

class AlertsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                db.watchlistDao().observeAll(),
                db.alertDao().observeAll(),
            ) { watchlist, alerts ->
                AlertsUiState(
                    watchlist     = watchlist,
                    alertsByStock = alerts.groupBy { it.stockCode },
                )
            }.collect { _uiState.value = it }
        }
    }

    fun addAlert(
        stock: WatchlistEntity,
        type: AlertType,
        threshold: Double,
    ) {
        viewModelScope.launch {
            db.alertDao().insert(
                AlertEntity(
                    stockCode = stock.code,
                    stockName = stock.name,
                    market    = stock.market,
                    type      = type.name,
                    threshold = threshold,
                )
            )
        }
    }

    fun toggleAlert(id: Int, enabled: Boolean) {
        viewModelScope.launch { db.alertDao().setEnabled(id, enabled) }
    }

    fun deleteAlert(id: Int) {
        viewModelScope.launch { db.alertDao().delete(id) }
    }
}
