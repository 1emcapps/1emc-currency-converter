package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi {
  @GET("v6/latest/USD")
  suspend fun getLatestUsdRates(): ExchangeRateResponse
}

interface CoinGeckoApi {
  @GET("api/v3/simple/price")
  suspend fun getBitcoinUsdPrice(
    @Query("ids") ids: String = "bitcoin",
    @Query("vs_currencies") vsCurrencies: String = "usd",
  ): CoinGeckoSimplePriceResponse

  @GET("api/v3/coins/bitcoin/market_chart")
  suspend fun getBitcoinSevenDayHistory(
    @Query("vs_currency") vsCurrency: String = "usd",
    @Query("days") days: Int = 7,
  ): CoinGeckoMarketChartResponse
}
