package com.example.mystockwatch.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val code: String,
    val name: String,
    val market: String,   // "KOREA" or "USA"
    val addedAt: Long = System.currentTimeMillis(),
)
