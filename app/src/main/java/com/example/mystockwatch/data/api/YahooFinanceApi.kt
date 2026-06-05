package com.example.mystockwatch.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YahooFinanceApi {
    @GET("v7/finance/quote")
    suspend fun getQuote(
        @Query("symbols") symbols: String,
    ): YahooResponse

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String,  // "1h" | "1d" | "1wk"
        @Query("range")    range: String,      // "1d"  | "1mo" | "1y"
    ): YahooChartResponse
}

data class YahooResponse(
    @SerializedName("quoteResponse") val quoteResponse: YahooQuoteResponse?,
)

data class YahooQuoteResponse(
    @SerializedName("result") val result: List<YahooQuote>?,
    @SerializedName("error") val error: Any?,
)

data class YahooQuote(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("longName") val longName: String?,
    @SerializedName("shortName") val shortName: String?,
    @SerializedName("regularMarketPrice") val price: Double,
    @SerializedName("regularMarketChange") val changeValue: Double,
    @SerializedName("regularMarketChangePercent") val changeRate: Double,
    @SerializedName("regularMarketVolume") val volume: Long,
    @SerializedName("regularMarketTime") val timestamp: Long,
    @SerializedName("marketState") val marketState: String,   // "REGULAR", "PRE", "POST", "CLOSED"
)

// ── 과거 데이터 (차트용) ────────────────────────────────────────

data class YahooChartResponse(
    @SerializedName("chart") val chart: YahooChartWrapper?,
)

data class YahooChartWrapper(
    @SerializedName("result") val result: List<YahooChartData>?,
    @SerializedName("error")  val error: Any?,
)

data class YahooChartData(
    @SerializedName("timestamp")  val timestamps: List<Long>?,
    @SerializedName("indicators") val indicators: YahooIndicators?,
)

data class YahooIndicators(
    @SerializedName("quote") val quote: List<YahooOhlcv>?,
)

data class YahooOhlcv(
    @SerializedName("close") val close: List<Double?>?,
)
