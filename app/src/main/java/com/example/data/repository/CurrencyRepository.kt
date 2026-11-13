package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.AppDatabase
import com.example.data.local.BtcHistoryPointEntity
import com.example.data.local.CachedRateEntity
import com.example.data.local.RateDao
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesStore
import com.example.data.remote.CoinGeckoApi
import com.example.data.remote.ExchangeRateApi
import com.example.domain.CurrencyCatalog
import com.example.domain.CurrencyConverter
import com.example.domain.DecimalPrecisionMode
import com.example.ui.theme.ThemeMode
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

sealed interface RefreshResult {
  data class Success(val updatedAtMs: Long) : RefreshResult
  data class OfflineFallback(val reason: String, val hasCachedRates: Boolean) : RefreshResult
}

class CurrencyRepository(
  private val context: Context,
  private val rateDao: RateDao,
  val preferencesStore: UserPreferencesStore,
  private val exchangeRateApi: ExchangeRateApi,
  private val coinGeckoApi: CoinGeckoApi,
) {

  val ratesFlow: Flow<Map<String, Double>> = rateDao.observeAllRates().map { entities ->
    entities.associate { it.code to it.ratePerUsd }
  }

  val btcHistoryFlow: Flow<List<Double>> = rateDao.observeBtcHistory().map { points ->
    points.map { it.priceUsd }
  }

  val preferencesFlow: Flow<UserPreferences> = preferencesStore.preferencesFlow

  /**
   * Fetches latest fiat rates from open.er-api.com and BTC price + 7d chart from CoinGecko.
   * Uses USD as pivot currency. Preserves partial/cached data if any endpoint fails and never crashes.
   */
  suspend fun refreshRates(): RefreshResult = withContext(Dispatchers.IO) {
    val existingRates = rateDao.getAllRatesOnce().associate { it.code to it.ratePerUsd }
    val hasCache = existingRates.isNotEmpty()

    if (!isNetworkAvailable()) {
      return@withContext RefreshResult.OfflineFallback(
        reason = "No internet connection",
        hasCachedRates = hasCache,
      )
    }

    try {
      coroutineScope {
        val fiatDeferred = async {
          runCatching { exchangeRateApi.getLatestUsdRates() }
        }
        val btcPriceDeferred = async {
          runCatching { coinGeckoApi.getBitcoinUsdPrice() }
        }
        val btcHistoryDeferred = async {
          runCatching { coinGeckoApi.getBitcoinSevenDayHistory() }
        }

        val fiatOutcome = fiatDeferred.await()
        val btcPriceOutcome = btcPriceDeferred.await()
        val btcHistoryOutcome = btcHistoryDeferred.await()

        val nowMs = System.currentTimeMillis()
        val newFiatRates = fiatOutcome.getOrNull()?.rates
          ?.takeIf { it.isNotEmpty() }
        val newBtcUsd = btcPriceOutcome.getOrNull()?.bitcoin?.usd
          ?.takeIf { it.isFinite() && it > 0.0 }

        // Recover prior BTC/USD from cached BTC rate if CoinGecko price call failed
        val fallbackBtcUsd = existingRates[CurrencyCatalog.CODE_BTC]
          ?.takeIf { it > 0.0 }
          ?.let { 1.0 / it }
        val effectiveBtcUsd = newBtcUsd ?: fallbackBtcUsd

        val baseFiatMap = newFiatRates ?: existingRates.filterKeys {
          it != CurrencyCatalog.CODE_BTC && it != CurrencyCatalog.CODE_SATS
        }

        val fiatSucceeded = newFiatRates != null
        val btcSucceeded = newBtcUsd != null

        if (baseFiatMap.isNotEmpty() || effectiveBtcUsd != null) {
          val mergedRates = CurrencyConverter.buildRatesPerUsdMap(
            fiatRatesPerUsd = baseFiatMap,
            btcPriceInUsd = effectiveBtcUsd,
          )
          val entities = mergedRates.map { (code, rate) ->
            CachedRateEntity(code = code, ratePerUsd = rate, updatedAtEpochMs = nowMs)
          }
          rateDao.upsertRates(entities)
        }

        // Update 7-day BTC history if fetched successfully
        val historyPairs = btcHistoryOutcome.getOrNull()?.prices
        if (!historyPairs.isNullOrEmpty()) {
          val validPoints = historyPairs.mapNotNull { entry ->
            if (entry.size >= 2) {
              val ts = entry[0].toLong()
              val price = entry[1]
              if (price.isFinite() && price > 0.0) BtcHistoryPointEntity(ts, price) else null
            } else null
          }
          if (validPoints.size >= 2) {
            // Downsample evenly to at most 84 points for snappy Canvas drawing
            val step = (validPoints.size / 84).coerceAtLeast(1)
            val sampled = validPoints.filterIndexed { idx, _ ->
              idx % step == 0 || idx == validPoints.lastIndex
            }
            rateDao.replaceBtcHistory(sampled)
          }
        }

        if (fiatSucceeded || btcSucceeded) {
          if (fiatSucceeded && btcSucceeded) {
            preferencesStore.setLastUpdatedEpochMs(nowMs)
            RefreshResult.Success(nowMs)
          } else {
            // Partial refresh still updated available rates; record timestamp only if we got fresh data
            preferencesStore.setLastUpdatedEpochMs(nowMs)
            val missingPart = if (!fiatSucceeded) "Fiat rates unavailable" else "BTC rate unavailable"
            RefreshResult.OfflineFallback(
              reason = missingPart,
              hasCachedRates = true,
            )
          }
        } else {
          RefreshResult.OfflineFallback(
            reason = "Rate servers unreachable",
            hasCachedRates = hasCache || existingRates.isNotEmpty(),
          )
        }
      }
    } catch (ce: CancellationException) {
      throw ce
    } catch (e: Exception) {
      RefreshResult.OfflineFallback(
        reason = e.localizedMessage ?: "Network error",
        hasCachedRates = hasCache,
      )
    }
  }

  suspend fun clearCache() {
    withContext(Dispatchers.IO) {
      rateDao.clearAllCache()
      preferencesStore.setLastUpdatedEpochMs(0L)
    }
  }

  suspend fun toggleFavorite(code: String) = preferencesStore.toggleFavorite(code)
  suspend fun recordRecent(code: String) = preferencesStore.recordRecent(code)
  suspend fun setConvertPair(fromCode: String, toCode: String) =
    preferencesStore.setConvertPair(fromCode, toCode)
  suspend fun setMultiBaseCurrency(code: String) = preferencesStore.setMultiBaseCurrency(code)
  suspend fun setMultiTargets(targets: List<String>) = preferencesStore.setMultiTargets(targets)
  suspend fun setDefaultBaseCurrency(code: String) = preferencesStore.setDefaultBaseCurrency(code)
  suspend fun setThemeMode(mode: ThemeMode) = preferencesStore.setThemeMode(mode)
  suspend fun setPrecisionMode(mode: DecimalPrecisionMode) = preferencesStore.setPrecisionMode(mode)
  suspend fun setRefreshIntervalMinutes(minutes: Int) =
    preferencesStore.setRefreshIntervalMinutes(minutes)

  private fun isNetworkAvailable(): Boolean {
    return runCatching {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
      val network = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(network) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(true)
  }

  companion object {
    @Volatile
    private var INSTANCE: CurrencyRepository? = null

    fun create(context: Context): CurrencyRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: buildDefault(context.applicationContext).also { INSTANCE = it }
      }
    }

    private fun buildDefault(context: Context): CurrencyRepository {
      val okHttp = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

      val moshi = Moshi.Builder().build()
      val converter = MoshiConverterFactory.create(moshi)

      val exchangeRateApi = Retrofit.Builder()
        .baseUrl("https://open.er-api.com/")
        .client(okHttp)
        .addConverterFactory(converter)
        .build()
        .create(ExchangeRateApi::class.java)

      val coinGeckoApi = Retrofit.Builder()
        .baseUrl("https://api.coingecko.com/")
        .client(okHttp)
        .addConverterFactory(converter)
        .build()
        .create(CoinGeckoApi::class.java)

      val db = AppDatabase.getInstance(context)
      val prefs = UserPreferencesStore(context)

      return CurrencyRepository(
        context = context,
        rateDao = db.rateDao(),
        preferencesStore = prefs,
        exchangeRateApi = exchangeRateApi,
        coinGeckoApi = coinGeckoApi,
      )
    }
  }
}
