package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferences
import com.example.data.repository.CurrencyRepository
import com.example.data.repository.RefreshResult
import com.example.domain.ConversionOutcome
import com.example.domain.CurrencyCatalog
import com.example.domain.CurrencyConverter
import com.example.domain.CurrencyInfo
import com.example.domain.DecimalPrecisionMode
import com.example.ui.theme.ThemeMode
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MultiCurrencyItemUi(
  val info: CurrencyInfo,
  val formattedAmount: String,
  val unitRateLabel: String,
  val hasRate: Boolean,
)

data class MainUiState(
  val preferences: UserPreferences = UserPreferences(),
  val ratesPerUsd: Map<String, Double> = emptyMap(),
  val btcHistoryUsd: List<Double> = emptyList(),
  val availableCurrencies: List<CurrencyInfo> = CurrencyCatalog.defaultCatalog,
  val rawAmountInput: String = "1",
  val formattedAmountInput: String = "1",
  val convertedDisplay: String = "—",
  val hasMissingRate: Boolean = false,
  val unitRateSummary: String = "",
  val sparklinePoints: List<Double> = emptyList(),
  val multiRawAmountInput: String = "100",
  val multiFormattedAmountInput: String = "100",
  val multiItems: List<MultiCurrencyItemUi> = emptyList(),
  val isInitialLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val isOfflineBannerVisible: Boolean = false,
  val errorMessage: String? = null,
  val lastUpdatedLabel: String = "",
  val swapCounter: Int = 0,
) {
  val fromInfo: CurrencyInfo get() = CurrencyCatalog.getInfo(preferences.convertFromCurrency)
  val toInfo: CurrencyInfo get() = CurrencyCatalog.getInfo(preferences.convertToCurrency)
  val multiBaseInfo: CurrencyInfo get() = CurrencyCatalog.getInfo(preferences.multiBaseCurrency)
  val hasRates: Boolean get() = ratesPerUsd.isNotEmpty()
}

private data class TransientState(
  val rawAmountInput: String = "1",
  val multiRawAmountInput: String = "100",
  val isInitialLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val isOfflineBannerVisible: Boolean = false,
  val errorMessage: String? = null,
  val swapCounter: Int = 0,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = CurrencyRepository.create(application)
  private val transientState = MutableStateFlow(TransientState())
  private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

  private var autoRefreshJob: Job? = null

  val uiState: StateFlow<MainUiState> = combine(
    repository.preferencesFlow,
    repository.ratesFlow,
    repository.btcHistoryFlow,
    transientState,
  ) { prefs, rates, btcHistory, transient ->
    val available = CurrencyCatalog.allCurrencies(rates.keys)

    val convertOutcome = CurrencyConverter.convertByCode(
      amountInput = transient.rawAmountInput,
      fromCode = prefs.convertFromCurrency,
      toCode = prefs.convertToCurrency,
      ratesPerUsd = rates,
      precisionMode = prefs.precisionMode,
    )

    val (convertedText, missingRate) = when (convertOutcome) {
      is ConversionOutcome.Success -> convertOutcome.formatted to false
      ConversionOutcome.MissingRate -> (if (rates.isEmpty()) "—" else "N/A") to rates.isNotEmpty()
    }

    val unitRateOutcome = CurrencyConverter.convertByCode(
      amountInput = "1",
      fromCode = prefs.convertFromCurrency,
      toCode = prefs.convertToCurrency,
      ratesPerUsd = rates,
      precisionMode = prefs.precisionMode,
    )
    val unitRateSummary = when (unitRateOutcome) {
      is ConversionOutcome.Success ->
        "1 ${prefs.convertFromCurrency} = ${unitRateOutcome.formatted} ${prefs.convertToCurrency}"
      ConversionOutcome.MissingRate ->
        if (rates.isEmpty()) "Waiting for exchange rates"
        else "Rate unavailable for ${prefs.convertFromCurrency}/${prefs.convertToCurrency}"
    }

    val sparkline = CurrencyConverter.computePairSparkline(
      fromCode = prefs.convertFromCurrency,
      toCode = prefs.convertToCurrency,
      btcHistoryUsd = btcHistory,
      ratesPerUsd = rates,
    )

    val multiItems = prefs.multiTargets.map { targetCode ->
      val info = CurrencyCatalog.getInfo(targetCode)
      val itemOutcome = CurrencyConverter.convertByCode(
        amountInput = transient.multiRawAmountInput,
        fromCode = prefs.multiBaseCurrency,
        toCode = targetCode,
        ratesPerUsd = rates,
        precisionMode = prefs.precisionMode,
      )
      val unitOutcome = CurrencyConverter.convertByCode(
        amountInput = "1",
        fromCode = prefs.multiBaseCurrency,
        toCode = targetCode,
        ratesPerUsd = rates,
        precisionMode = prefs.precisionMode,
      )
      val unitLabel = when (unitOutcome) {
        is ConversionOutcome.Success ->
          "1 ${prefs.multiBaseCurrency} = ${unitOutcome.formatted} $targetCode"
        ConversionOutcome.MissingRate -> "Rate unavailable"
      }
      when (itemOutcome) {
        is ConversionOutcome.Success -> MultiCurrencyItemUi(
          info = info,
          formattedAmount = itemOutcome.formatted,
          unitRateLabel = unitLabel,
          hasRate = true,
        )
        ConversionOutcome.MissingRate -> MultiCurrencyItemUi(
          info = info,
          formattedAmount = if (rates.isEmpty()) "—" else "N/A",
          unitRateLabel = unitLabel,
          hasRate = false,
        )
      }
    }

    MainUiState(
      preferences = prefs,
      ratesPerUsd = rates,
      btcHistoryUsd = btcHistory,
      availableCurrencies = available,
      rawAmountInput = transient.rawAmountInput,
      formattedAmountInput = CurrencyConverter.formatEditableInput(transient.rawAmountInput),
      convertedDisplay = convertedText,
      hasMissingRate = missingRate,
      unitRateSummary = unitRateSummary,
      sparklinePoints = sparkline,
      multiRawAmountInput = transient.multiRawAmountInput,
      multiFormattedAmountInput = CurrencyConverter.formatEditableInput(transient.multiRawAmountInput),
      multiItems = multiItems,
      isInitialLoading = transient.isInitialLoading && rates.isEmpty(),
      isRefreshing = transient.isRefreshing,
      isOfflineBannerVisible = transient.isOfflineBannerVisible && rates.isNotEmpty(),
      errorMessage = if (rates.isEmpty()) transient.errorMessage else null,
      lastUpdatedLabel = formatLastUpdated(prefs.lastUpdatedEpochMs),
      swapCounter = transient.swapCounter,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = MainUiState(),
  )

  init {
    performRefresh(isInitial = true)
    observeAutoRefreshInterval()
  }

  private fun observeAutoRefreshInterval() {
    viewModelScope.launch {
      repository.preferencesFlow
        .map { it.refreshIntervalMinutes }
        .distinctUntilChanged()
        .collect { intervalMinutes ->
          autoRefreshJob?.cancel()
          if (intervalMinutes > 0) {
            autoRefreshJob = viewModelScope.launch {
              while (isActive) {
                delay(intervalMinutes * 60_000L)
                performRefresh(isInitial = false)
              }
            }
          }
        }
    }
  }

  fun manualRefresh() {
    performRefresh(isInitial = false)
  }

  private fun performRefresh(isInitial: Boolean) {
    if (transientState.value.isRefreshing) return
    viewModelScope.launch {
      transientState.update {
        it.copy(
          isRefreshing = !isInitial,
          errorMessage = null,
        )
      }
      when (val result = repository.refreshRates()) {
        is RefreshResult.Success -> {
          transientState.update {
            it.copy(
              isInitialLoading = false,
              isRefreshing = false,
              isOfflineBannerVisible = false,
              errorMessage = null,
            )
          }
        }
        is RefreshResult.OfflineFallback -> {
          transientState.update {
            it.copy(
              isInitialLoading = false,
              isRefreshing = false,
              isOfflineBannerVisible = result.hasCachedRates,
              errorMessage = if (!result.hasCachedRates) result.reason else null,
            )
          }
        }
      }
    }
  }

  // --- Convert Screen Keypad Input ---

  fun onConvertKeyDigit(digit: String) {
    transientState.update { state ->
      state.copy(rawAmountInput = appendDigit(state.rawAmountInput, digit))
    }
  }

  fun onConvertKeyDoubleZero() {
    transientState.update { state ->
      var next = appendDigit(state.rawAmountInput, "0")
      next = appendDigit(next, "0")
      state.copy(rawAmountInput = next)
    }
  }

  fun onConvertKeyDecimal() {
    transientState.update { state ->
      state.copy(rawAmountInput = appendDecimal(state.rawAmountInput))
    }
  }

  fun onConvertBackspace() {
    transientState.update { state ->
      state.copy(rawAmountInput = backspaceInput(state.rawAmountInput))
    }
  }

  fun onConvertClear() {
    transientState.update { it.copy(rawAmountInput = "") }
  }

  fun onConvertPreset(preset: String) {
    transientState.update { it.copy(rawAmountInput = preset) }
  }

  // --- Multi Screen Keypad Input ---

  fun onMultiKeyDigit(digit: String) {
    transientState.update { state ->
      state.copy(multiRawAmountInput = appendDigit(state.multiRawAmountInput, digit))
    }
  }

  fun onMultiKeyDoubleZero() {
    transientState.update { state ->
      var next = appendDigit(state.multiRawAmountInput, "0")
      next = appendDigit(next, "0")
      state.copy(multiRawAmountInput = next)
    }
  }

  fun onMultiKeyDecimal() {
    transientState.update { state ->
      state.copy(multiRawAmountInput = appendDecimal(state.multiRawAmountInput))
    }
  }

  fun onMultiBackspace() {
    transientState.update { state ->
      state.copy(multiRawAmountInput = backspaceInput(state.multiRawAmountInput))
    }
  }

  fun onMultiClear() {
    transientState.update { it.copy(multiRawAmountInput = "") }
  }

  fun onMultiPreset(preset: String) {
    transientState.update { it.copy(multiRawAmountInput = preset) }
  }

  // --- Currency Selection & Favorites ---

  fun swapConvertCurrencies() {
    val current = uiState.value.preferences
    viewModelScope.launch {
      repository.setConvertPair(current.convertToCurrency, current.convertFromCurrency)
      transientState.update { it.copy(swapCounter = it.swapCounter + 1) }
    }
  }

  fun selectConvertFrom(code: String) {
    val currentTo = uiState.value.preferences.convertToCurrency
    viewModelScope.launch {
      repository.setConvertPair(code, currentTo)
      repository.recordRecent(code)
    }
  }

  fun selectConvertTo(code: String) {
    val currentFrom = uiState.value.preferences.convertFromCurrency
    viewModelScope.launch {
      repository.setConvertPair(currentFrom, code)
      repository.recordRecent(code)
    }
  }

  fun toggleFavorite(code: String) {
    viewModelScope.launch {
      repository.toggleFavorite(code)
    }
  }

  // --- Multi-Currency List Management ---

  fun selectMultiBase(code: String) {
    viewModelScope.launch {
      repository.setMultiBaseCurrency(code)
      repository.recordRecent(code)
    }
  }

  fun addMultiTarget(code: String) {
    val current = uiState.value.preferences.multiTargets
    if (code in current) {
      _snackbarMessages.tryEmit("$code is already in your Multi list")
      return
    }
    viewModelScope.launch {
      repository.setMultiTargets(current + code)
      repository.recordRecent(code)
    }
  }

  fun removeMultiTarget(code: String) {
    val current = uiState.value.preferences.multiTargets
    viewModelScope.launch {
      repository.setMultiTargets(current.filterNot { it == code })
    }
  }

  fun moveMultiTarget(fromIndex: Int, toIndex: Int) {
    val current = uiState.value.preferences.multiTargets.toMutableList()
    if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
    val item = current.removeAt(fromIndex)
    current.add(toIndex, item)
    viewModelScope.launch {
      repository.setMultiTargets(current)
    }
  }

  // --- Settings ---

  fun setDefaultBaseCurrency(code: String) {
    viewModelScope.launch {
      repository.setDefaultBaseCurrency(code)
      repository.recordRecent(code)
      _snackbarMessages.tryEmit("Default base currency set to $code")
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      repository.setThemeMode(mode)
    }
  }

  fun setPrecisionMode(mode: DecimalPrecisionMode) {
    viewModelScope.launch {
      repository.setPrecisionMode(mode)
    }
  }

  fun setRefreshIntervalMinutes(minutes: Int) {
    viewModelScope.launch {
      repository.setRefreshIntervalMinutes(minutes)
    }
  }

  fun clearCache() {
    viewModelScope.launch {
      repository.clearCache()
      transientState.update {
        it.copy(
          isOfflineBannerVisible = false,
          errorMessage = "Cache cleared. Pull down or tap Refresh to fetch live rates.",
        )
      }
      _snackbarMessages.tryEmit("Cached rates cleared")
    }
  }

  fun notifyCopied(formattedValue: String, currencyCode: String) {
    _snackbarMessages.tryEmit("Copied $formattedValue $currencyCode")
  }

  private fun appendDigit(current: String, digit: String): String {
    val dotIdx = current.indexOf('.')
    if (dotIdx >= 0) {
      val decimals = current.length - dotIdx - 1
      if (decimals >= 8) return current
    } else if (current.length >= 13) {
      return current
    }
    return when {
      current.isEmpty() || current == "0" -> digit
      else -> current + digit
    }
  }

  private fun appendDecimal(current: String): String {
    if (current.contains('.')) return current
    return if (current.isEmpty()) "0." else "$current."
  }

  private fun backspaceInput(current: String): String {
    if (current.isEmpty()) return ""
    return current.dropLast(1)
  }

  private fun formatLastUpdated(epochMs: Long): String {
    if (epochMs <= 0L) return "Not updated yet"
    val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMs))
  }
}
