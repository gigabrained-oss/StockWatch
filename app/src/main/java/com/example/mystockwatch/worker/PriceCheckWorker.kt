package com.example.mystockwatch.worker

import android.content.Context
import androidx.work.*
import com.example.mystockwatch.data.api.yahooFinanceApi
import com.example.mystockwatch.data.db.AlertType
import com.example.mystockwatch.data.db.AppDatabase
import java.util.concurrent.TimeUnit

class PriceCheckWorker(
    ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val db       = AppDatabase.getInstance(applicationContext)
        val watchlist = db.watchlistDao().let {
            // Flow 대신 일회성 조회를 위해 직접 query
            it.observeAll().let { flow ->
                val list = mutableListOf<com.example.mystockwatch.data.db.WatchlistEntity>()
                kotlinx.coroutines.withTimeout(5_000) {
                    flow.collect { l -> list.addAll(l); return@collect }
                }
                list
            }
        }
        val alerts = db.alertDao().getEnabledAlerts()
        if (alerts.isEmpty()) return Result.success()

        // 가격 조회 (Yahoo Finance)
        val symbols = watchlist.map { s ->
            if (s.market == "KOREA") "${s.code}.KS" else s.code
        }.joinToString(",")

        val quotes = runCatching {
            yahooFinanceApi.getQuote(symbols).quoteResponse?.result.orEmpty()
        }.getOrElse { return Result.retry() }

        // 각 알림 조건 체크
        alerts.forEachIndexed { idx, alert ->
            val yahooSymbol = if (alert.market == "KOREA") "${alert.stockCode}.KS" else alert.stockCode
            val quote = quotes.firstOrNull {
                it.symbol.equals(yahooSymbol, ignoreCase = true)
            } ?: return@forEachIndexed

            val price      = quote.price
            val changeRate = quote.changeRate
            val type       = runCatching { AlertType.valueOf(alert.type) }.getOrNull() ?: return@forEachIndexed

            val triggered = when (type) {
                AlertType.PRICE_ABOVE  -> price >= alert.threshold
                AlertType.PRICE_BELOW  -> price <= alert.threshold
                AlertType.CHANGE_ABOVE -> changeRate >= alert.threshold
                AlertType.CHANGE_BELOW -> changeRate <= -alert.threshold
            }

            if (triggered) {
                val (title, body) = formatNotification(alert.stockName, type, alert.threshold, price, changeRate)
                sendAlertNotification(applicationContext, title, body, alert.id)
            }
        }

        return Result.success()
    }

    private fun formatNotification(
        name: String,
        type: AlertType,
        threshold: Double,
        price: Double,
        changeRate: Double,
    ): Pair<String, String> {
        val title = "$name — 조건 도달"
        val body = when (type) {
            AlertType.PRICE_ABOVE  -> "현재가 ₩%,.0f ≥ 설정가 ₩%,.0f".format(price, threshold)
            AlertType.PRICE_BELOW  -> "현재가 ₩%,.0f ≤ 설정가 ₩%,.0f".format(price, threshold)
            AlertType.CHANGE_ABOVE -> "등락률 +%.2f%% ≥ 설정값 +%.2f%%".format(changeRate, threshold)
            AlertType.CHANGE_BELOW -> "등락률 %.2f%% ≤ 설정값 -%.2f%%".format(changeRate, threshold)
        }
        return title to body
    }

    companion object {
        const val WORK_NAME = "PriceCheckWorker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PriceCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
