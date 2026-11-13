package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_rates")
data class CachedRateEntity(
  @PrimaryKey val code: String,
  val ratePerUsd: Double,
  val updatedAtEpochMs: Long,
)

@Entity(tableName = "btc_history_points")
data class BtcHistoryPointEntity(
  @PrimaryKey val timestampMs: Long,
  val priceUsd: Double,
)
