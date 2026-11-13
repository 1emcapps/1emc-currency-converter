package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.DecimalPrecisionMode
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "emc_converter_prefs"
)

data class UserPreferences(
  val favorites: Set<String> = setOf("BTC", "SATS", "USD", "EUR", "GBP", "JPY"),
  val recents: List<String> = listOf("BTC", "USD", "EUR", "SATS"),
  val multiTargets: List<String> = listOf("BTC", "SATS", "EUR", "GBP", "JPY", "IDR"),
  val multiBaseCurrency: String = "USD",
  val convertFromCurrency: String = "BTC",
  val convertToCurrency: String = "USD",
  val defaultBaseCurrency: String = "USD",
  val themeMode: ThemeMode = ThemeMode.DARK,
  val precisionMode: DecimalPrecisionMode = DecimalPrecisionMode.AUTO,
  val refreshIntervalMinutes: Int = 5,
  val lastUpdatedEpochMs: Long = 0L,
)

class UserPreferencesStore(private val context: Context) {

  private object Keys {
    val FAVORITES = stringPreferencesKey("favorites_csv")
    val RECENTS = stringPreferencesKey("recents_csv")
    val MULTI_TARGETS = stringPreferencesKey("multi_targets_csv")
    val MULTI_BASE = stringPreferencesKey("multi_base")
    val CONVERT_FROM = stringPreferencesKey("convert_from")
    val CONVERT_TO = stringPreferencesKey("convert_to")
    val DEFAULT_BASE = stringPreferencesKey("default_base")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val PRECISION_MODE = stringPreferencesKey("precision_mode")
    val REFRESH_INTERVAL_MINUTES = intPreferencesKey("refresh_interval_minutes")
    val LAST_UPDATED_EPOCH_MS = longPreferencesKey("last_updated_epoch_ms")
  }

  val preferencesFlow: Flow<UserPreferences> = context.userPreferencesDataStore.data.map { prefs ->
    val defaults = UserPreferences()
    val defaultBase = prefs[Keys.DEFAULT_BASE] ?: defaults.defaultBaseCurrency

    UserPreferences(
      favorites = prefs[Keys.FAVORITES]?.let(::decodeCsvSet) ?: defaults.favorites,
      recents = prefs[Keys.RECENTS]?.let(::decodeCsvList) ?: defaults.recents,
      multiTargets = prefs[Keys.MULTI_TARGETS]?.let(::decodeCsvList) ?: defaults.multiTargets,
      multiBaseCurrency = prefs[Keys.MULTI_BASE] ?: defaultBase,
      convertFromCurrency = prefs[Keys.CONVERT_FROM] ?: defaults.convertFromCurrency,
      convertToCurrency = prefs[Keys.CONVERT_TO] ?: defaultBase,
      defaultBaseCurrency = defaultBase,
      themeMode = prefs[Keys.THEME_MODE]?.let { name ->
        ThemeMode.entries.firstOrNull { it.name == name }
      } ?: defaults.themeMode,
      precisionMode = prefs[Keys.PRECISION_MODE]?.let { name ->
        DecimalPrecisionMode.entries.firstOrNull { it.name == name }
      } ?: defaults.precisionMode,
      refreshIntervalMinutes = prefs[Keys.REFRESH_INTERVAL_MINUTES] ?: defaults.refreshIntervalMinutes,
      lastUpdatedEpochMs = prefs[Keys.LAST_UPDATED_EPOCH_MS] ?: defaults.lastUpdatedEpochMs,
    )
  }

  suspend fun toggleFavorite(code: String) {
    context.userPreferencesDataStore.edit { prefs ->
      val current = prefs[Keys.FAVORITES]?.let(::decodeCsvSet)?.toMutableSet()
        ?: UserPreferences().favorites.toMutableSet()
      if (!current.add(code)) {
        current.remove(code)
      }
      prefs[Keys.FAVORITES] = encodeCsv(current.toList())
    }
  }

  suspend fun recordRecent(code: String) {
    context.userPreferencesDataStore.edit { prefs ->
      val current = prefs[Keys.RECENTS]?.let(::decodeCsvList)?.toMutableList()
        ?: UserPreferences().recents.toMutableList()
      current.remove(code)
      current.add(0, code)
      prefs[Keys.RECENTS] = encodeCsv(current.take(6))
    }
  }

  suspend fun setConvertPair(fromCode: String, toCode: String) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.CONVERT_FROM] = fromCode
      prefs[Keys.CONVERT_TO] = toCode
    }
  }

  suspend fun setMultiBaseCurrency(code: String) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.MULTI_BASE] = code
    }
  }

  suspend fun setMultiTargets(targets: List<String>) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.MULTI_TARGETS] = encodeCsv(targets.distinct())
    }
  }

  suspend fun setDefaultBaseCurrency(code: String) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.DEFAULT_BASE] = code
    }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.THEME_MODE] = mode.name
    }
  }

  suspend fun setPrecisionMode(mode: DecimalPrecisionMode) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.PRECISION_MODE] = mode.name
    }
  }

  suspend fun setRefreshIntervalMinutes(minutes: Int) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.REFRESH_INTERVAL_MINUTES] = minutes
    }
  }

  suspend fun setLastUpdatedEpochMs(epochMs: Long) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.LAST_UPDATED_EPOCH_MS] = epochMs
    }
  }

  private fun encodeCsv(items: List<String>): String = items.joinToString(",")

  private fun decodeCsvList(raw: String): List<String> =
    if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

  private fun decodeCsvSet(raw: String): Set<String> = decodeCsvList(raw).toSet()
}
