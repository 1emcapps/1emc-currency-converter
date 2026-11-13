package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RateDao {
  @Query("SELECT * FROM cached_rates")
  fun observeAllRates(): Flow<List<CachedRateEntity>>

  @Query("SELECT * FROM cached_rates")
  suspend fun getAllRatesOnce(): List<CachedRateEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRates(rates: List<CachedRateEntity>)

  @Query("DELETE FROM cached_rates")
  suspend fun clearRates()

  @Query("SELECT * FROM btc_history_points ORDER BY timestampMs ASC")
  fun observeBtcHistory(): Flow<List<BtcHistoryPointEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBtcHistory(points: List<BtcHistoryPointEntity>)

  @Query("DELETE FROM btc_history_points")
  suspend fun clearBtcHistory()

  @Transaction
  suspend fun replaceBtcHistory(points: List<BtcHistoryPointEntity>) {
    clearBtcHistory()
    insertBtcHistory(points)
  }

  @Transaction
  suspend fun clearAllCache() {
    clearRates()
    clearBtcHistory()
  }
}
