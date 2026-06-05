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
)

data class NaverArea(
    @SerializedName("datas") val datas: List<NaverData>,
)

data class NaverData(
    @SerializedName("cd") val code: String,
    @SerializedName("nm") val name: String,
    @SerializedName("sv") val price: Double,
    @SerializedName("cv") val changeValue: Double,
    @SerializedName("cr") val changeRate: Double,
    @SerializedName("rf") val riseFlag: String,    // "1"=상승 "2"=하락 "3"=보합
    @SerializedName("aq") val volume: Long,
    @SerializedName("timestamp") val timestamp: Long,
)
