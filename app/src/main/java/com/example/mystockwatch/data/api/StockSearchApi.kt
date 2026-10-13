package com.example.mystockwatch.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface StockSearchApi {
    @GET("ac")
    suspend fun search(
        @Query("q") query: String,
        @Query("target") target: String = "stock,ETF,ETN",
        @Query("st") st: Int = 111,
    ): StockSearchResponse
}

data class StockSearchResponse(
    @SerializedName("items") val items: List<StockSearchItem> = emptyList(),
)

data class StockSearchItem(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("typeName") val typeName: String,     // "코스피", "나스닥 증권거래소", ...
    @SerializedName("nationCode") val nationCode: String, // "KOR", "USA", ...
)
