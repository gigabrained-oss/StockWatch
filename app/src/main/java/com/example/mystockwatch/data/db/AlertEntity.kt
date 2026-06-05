package com.example.mystockwatch.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertType {
    PRICE_ABOVE,   // 현재가 ≥ threshold
    PRICE_BELOW,   // 현재가 ≤ threshold
    CHANGE_ABOVE,  // 등락률 ≥ +threshold%
    CHANGE_BELOW,  // 등락률 ≤ -threshold%
}

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val stockCode: String,
    val stockName: String,
    val market: String,         // "KOREA" | "USA"
    val type: String,           // AlertType.name
    val threshold: Double,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
