package com.example.mystockwatch.domain.model

data class StockSearchResult(
    val code: String,
    val name: String,
    val market: Market,
    val exchangeLabel: String,
)
