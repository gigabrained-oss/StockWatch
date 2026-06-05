package com.example.mystockwatch.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE code = :code")
    suspend fun delete(code: String)
}
