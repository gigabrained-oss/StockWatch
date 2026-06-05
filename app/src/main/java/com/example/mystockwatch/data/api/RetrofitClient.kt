package com.example.mystockwatch.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        // Yahoo Finance requires a browser-like User-Agent
        val request = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0")
            .build()
        chain.proceed(request)
    }
    .build()

val naverFinanceApi: NaverFinanceApi = Retrofit.Builder()
    .baseUrl("https://polling.finance.naver.com/")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(NaverFinanceApi::class.java)

val yahooFinanceApi: YahooFinanceApi = Retrofit.Builder()
    .baseUrl("https://query1.finance.yahoo.com/")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(YahooFinanceApi::class.java)
