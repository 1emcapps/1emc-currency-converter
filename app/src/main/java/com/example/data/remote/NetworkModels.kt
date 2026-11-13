package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeRateResponse(
  @Json(name = "result") val result: String? = null,
  @Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long? = null,
  @Json(name = "base_code") val baseCode: String? = null,
  @Json(name = "rates") val rates: Map<String, Double>? = null,
)

@JsonClass(generateAdapter = true)
data class CoinGeckoSimplePriceResponse(
  @Json(name = "bitcoin") val bitcoin: BitcoinUsdPrice? = null,
)

@JsonClass(generateAdapter = true)
data class BitcoinUsdPrice(
  @Json(name = "usd") val usd: Double? = null,
)

@JsonClass(generateAdapter = true)
data class CoinGeckoMarketChartResponse(
  @Json(name = "prices") val prices: List<List<Double>>? = null,
)
