package com.example.mystockwatch.domain.model

data class StockQuote(
    val code: String,
    val name: String,
    val market: Market,
    val price: Double,
    val changeValue: Double,
    val changeRate: Double,
    val volume: Long,
    val source: DataSource,
    val timestampMs: Long,
    val isMarketOpen: Boolean,
)

enum class Market { KOREA, USA }

enum class DataSource { NAVER, YAHOO }
