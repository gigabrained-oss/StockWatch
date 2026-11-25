package com.example.mystockwatch.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface NaverFinanceApi {
    @GET("api/realtime")
    suspend fun getQuote(
        @Query("query") query: String,   // "SERVICE_ITEM:{code}"
    ): NaverResponse
}

data class NaverResponse(
    @SerializedName("resultCode") val resultCode: String,
    @SerializedName("result") val result: NaverResult?,
)

data class NaverResult(
    @SerializedName("areas") val areas: List<NaverArea>,
    @SerializedName("time") val timeMs: Long?,     // server time, epoch ms
)

data class NaverArea(
    @SerializedName("datas") val datas: List<NaverData>,
)

data class NaverData(
    @SerializedName("cd") val code: String,
    @SerializedName("nm") val name: String,
    @SerializedName("nv") val nowPrice: Double?,   // current price (absent/0 before the open)
    @SerializedName("sv") val prevClose: Double,   // previous day's close (base price)
    @SerializedName("rf") val riseFlag: String?,   // "1"=상한 "2"=상승 "3"=보합 "4"=하한 "5"=하락
    @SerializedName("aq") val volume: Long,
) {
    // Derive change from nv - sv (as stockMonitor does) instead of trusting the
    // unsigned cv/cr fields plus rf, whose codes are easy to misread.
    val price: Double get() = nowPrice?.takeIf { it > 0 } ?: prevClose
    val changeValue: Double get() = price - prevClose
    val changeRate: Double get() =
        if (prevClose > 0) Math.round(changeValue / prevClose * 10000) / 100.0 else 0.0
}
