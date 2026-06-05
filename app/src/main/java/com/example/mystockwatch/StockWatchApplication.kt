package com.example.mystockwatch

import android.app.Application
import com.example.mystockwatch.worker.PriceCheckWorker
import com.example.mystockwatch.worker.createNotificationChannel

class StockWatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
        PriceCheckWorker.schedule(this)
    }
}
