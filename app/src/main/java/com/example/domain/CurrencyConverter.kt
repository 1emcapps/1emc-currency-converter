package com.example.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class DecimalPrecisionMode(val label: String, val subtitle: String) {
  AUTO("Smart (Auto)", "8 for BTC, 0 for SATS/JPY/IDR, 2 for fiat"),
  TWO("2 Decimals", "Standard 2 decimal places (0 for SATS)"),
  FOUR("4 Decimals", "Up to 4 decimal places for high precision"),
  EIGHT("8 Decimals", "Up to 8 decimal places (Satoshi-level)"),
}

sealed interface ConversionOutcome {
  data class Success(val amount: BigDecimal, val formatted: String) : ConversionOutcome
  data object MissingRate : ConversionOutcome
}

/**
 * Pure, side-effect-free currency conversion math pivoted through USD.
 * Every rate in [ratesPerUsd] represents how many units of that currency equal 1 USD.
 */
object CurrencyConverter {

  private val CALC_CONTEXT = MathContext(24, RoundingMode.HALF_UP)
  private val US_SYMBOLS = DecimalFormatSymbols(Locale.US)
  private val MAX_INPUT_BIG_DECIMAL = BigDecimal("1000000000000000") // 1 quadrillion safety cap

  /**
   * Pure conversion formula: amount * (rate_to / rate_from) via USD.
   * Returns null if either rate is missing, zero, or negative, or if inputs are non-finite.
   */
  fun convert(
    amount: BigDecimal,
    rateFromPerUsd: BigDecimal?,
    rateToPerUsd: BigDecimal?,
  ): BigDecimal? {
    if (rateFromPerUsd == null || rateToPerUsd == null) return null
    if (rateFromPerUsd <= BigDecimal.ZERO || rateToPerUsd <= BigDecimal.ZERO) return null
    if (amount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
    if (rateFromPerUsd.compareTo(rateToPerUsd) == 0) return amount

    // amount * (rate_to / rate_from)
    val ratio = rateToPerUsd.divide(rateFromPerUsd, CALC_CONTEXT)
    return amount.multiply(ratio, CALC_CONTEXT)
  }

  /**
   * Double overload for convenience while still executing division/multiplication in BigDecimal
   * to avoid floating-point drift on large or Satoshi-scale numbers.
   */
  fun convert(
    amount: Double,
    rateFromPerUsd: Double?,
    rateToPerUsd: Double?,
  ): Double? {
    if (!amount.isFinite()) return null
    if (rateFromPerUsd == null || rateToPerUsd == null) return null
    if (!rateFromPerUsd.isFinite() || !rateToPerUsd.isFinite()) return null
    if (rateFromPerUsd <= 0.0 || rateToPerUsd <= 0.0) return null
    if (amount == 0.0) return 0.0
    if (rateFromPerUsd == rateToPerUsd) return amount

    val result = convert(
      amount = BigDecimal.valueOf(amount),
      rateFromPerUsd = BigDecimal.valueOf(rateFromPerUsd),
      rateToPerUsd = BigDecimal.valueOf(rateToPerUsd),
    )
    return result?.toDouble()?.takeIf { it.isFinite() }
  }

  /**
   * Combines fiat rates (units per 1 USD) and Bitcoin USD price into a single USD-pivoted rate map,
   * injecting USD = 1.0, BTC = 1 / btcUsd, and SATS = 100_000_000 / btcUsd.
   */
  fun buildRatesPerUsdMap(
    fiatRatesPerUsd: Map<String, Double>,
    btcPriceInUsd: Double?,
  ): Map<String, Double> {
    val merged = LinkedHashMap<String, Double>(fiatRatesPerUsd.size + 3)
    for ((rawCode, rate) in fiatRatesPerUsd) {
      if (rate.isFinite() && rate > 0.0) {
        merged[rawCode.uppercase(Locale.US)] = rate
      }
    }
    merged[CurrencyCatalog.CODE_USD] = 1.0
    if (btcPriceInUsd != null && btcPriceInUsd.isFinite() && btcPriceInUsd > 0.0) {
      val btcPerUsd = 1.0 / btcPriceInUsd
      merged[CurrencyCatalog.CODE_BTC] = btcPerUsd
      merged[CurrencyCatalog.CODE_SATS] = CurrencyCatalog.SATS_PER_BTC / btcPriceInUsd
    }
    return merged
  }

  /**
   * Parses raw keypad/text input safely, gracefully handling "", ".", trailing ".", commas,
   * and very large numbers.
   */
  fun parseInputAmount(raw: String): BigDecimal {
    val sanitized = raw.replace(",", "").trim()
    if (sanitized.isEmpty() || sanitized == "." || sanitized == "-" || sanitized == "-.") {
      return BigDecimal.ZERO
    }
    val normalized = when {
      sanitized.startsWith(".") -> "0$sanitized"
      sanitized.endsWith(".") -> sanitized.dropLast(1)
      else -> sanitized
    }
    val parsed = normalized.toBigDecimalOrNull() ?: return BigDecimal.ZERO
    return if (parsed < BigDecimal.ZERO) BigDecimal.ZERO
    else if (parsed > MAX_INPUT_BIG_DECIMAL) MAX_INPUT_BIG_DECIMAL
    else parsed
  }

  /**
   * Formats raw user typing while preserving trailing decimal points (e.g. "12." or "0.")
   * and adding thousands separators to the integer part.
   */
  fun formatEditableInput(raw: String): String {
    if (raw.isEmpty()) return "0"
    if (raw == ".") return "0."
    val clean = raw.replace(",", "")
    val dotIndex = clean.indexOf('.')
    val intPart = if (dotIndex >= 0) clean.substring(0, dotIndex) else clean
    val fracPart = if (dotIndex >= 0) clean.substring(dotIndex) else ""

    val formattedInt = intPart.toBigIntegerOrNull()?.let { bigInt ->
      DecimalFormat("#,##0", US_SYMBOLS).format(bigInt)
    } ?: "0"

    return formattedInt + fracPart
  }

  /**
   * Full end-to-end conversion from raw input string + currency codes + USD-pivoted rate map.
   */
  fun convertByCode(
    amountInput: String,
    fromCode: String,
    toCode: String,
    ratesPerUsd: Map<String, Double>,
    precisionMode: DecimalPrecisionMode = DecimalPrecisionMode.AUTO,
  ): ConversionOutcome {
    val normalizedFrom = fromCode.uppercase(Locale.US)
    val normalizedTo = toCode.uppercase(Locale.US)
    val amount = parseInputAmount(amountInput)

    if (normalizedFrom == normalizedTo) {
      return ConversionOutcome.Success(
        amount = amount,
        formatted = formatConvertedAmount(amount, normalizedTo, precisionMode),
      )
    }

    val rateFrom = ratesPerUsd[normalizedFrom]
    val rateTo = ratesPerUsd[normalizedTo]
    if (rateFrom == null || rateTo == null || rateFrom <= 0.0 || rateTo <= 0.0) {
      return ConversionOutcome.MissingRate
    }

    val converted = convert(
      amount = amount,
      rateFromPerUsd = BigDecimal.valueOf(rateFrom),
      rateToPerUsd = BigDecimal.valueOf(rateTo),
    ) ?: return ConversionOutcome.MissingRate

    return ConversionOutcome.Success(
      amount = converted,
      formatted = formatConvertedAmount(converted, normalizedTo, precisionMode),
    )
  }

  /**
   * Formats a converted BigDecimal value using thousands separators and currency-appropriate
   * decimal rules (up to 8 for BTC, 0 for SATS and zero-decimal currencies like JPY/IDR, 2 for fiat).
   */
  fun formatConvertedAmount(
    value: BigDecimal,
    currencyCode: String,
    precisionMode: DecimalPrecisionMode = DecimalPrecisionMode.AUTO,
  ): String {
    val code = currencyCode.uppercase(Locale.US)
    if (code == CurrencyCatalog.CODE_SATS) {
      val rounded = value.setScale(0, RoundingMode.HALF_UP)
      return DecimalFormat("#,##0", US_SYMBOLS).format(rounded)
    }

    val (minDecimals, maxDecimals) = when (precisionMode) {
      DecimalPrecisionMode.AUTO -> when {
        code == CurrencyCatalog.CODE_BTC -> 0 to 8
        CurrencyCatalog.isZeroDecimal(code) -> 0 to 0
        else -> 2 to 2
      }
      DecimalPrecisionMode.TWO -> 2 to 2
      DecimalPrecisionMode.FOUR -> 4 to 4
      DecimalPrecisionMode.EIGHT -> 2 to 8
    }

    val rounded = value.setScale(maxDecimals, RoundingMode.HALF_UP)
    val formatter = DecimalFormat().apply {
      decimalFormatSymbols = US_SYMBOLS
      isGroupingUsed = true
      groupingSize = 3
      minimumFractionDigits = minDecimals
      maximumFractionDigits = maxDecimals
      roundingMode = RoundingMode.HALF_UP
    }
    return formatter.format(rounded)
  }

  /**
   * Computes a 7-day trend series for the selected pair when BTC or SATS is involved,
   * using CoinGecko's 7-day BTC/USD prices and current fiat cross-rates relative to USD.
   * Returns an empty list for fiat-to-fiat pairs (where historical data is unavailable).
   */
  fun computePairSparkline(
    fromCode: String,
    toCode: String,
    btcHistoryUsd: List<Double>,
    ratesPerUsd: Map<String, Double>,
  ): List<Double> {
    if (btcHistoryUsd.size < 2) return emptyList()
    val from = fromCode.uppercase(Locale.US)
    val to = toCode.uppercase(Locale.US)
    if (from == to) return emptyList()

    val fromIsBtc = from == CurrencyCatalog.CODE_BTC
    val fromIsSats = from == CurrencyCatalog.CODE_SATS
    val toIsBtc = to == CurrencyCatalog.CODE_BTC
    val toIsSats = to == CurrencyCatalog.CODE_SATS

    if (!fromIsBtc && !fromIsSats && !toIsBtc && !toIsSats) {
      return emptyList()
    }

    // BTC <-> SATS has a fixed 100,000,000:1 peg, so no market volatility chart needed.
    if ((fromIsBtc || fromIsSats) && (toIsBtc || toIsSats)) {
      return emptyList()
    }

    return when {
      fromIsBtc -> {
        val rateTo = ratesPerUsd[to] ?: return emptyList()
        btcHistoryUsd.filter { it > 0.0 }.map { btcUsd -> btcUsd * rateTo }
      }
      fromIsSats -> {
        val rateTo = ratesPerUsd[to] ?: return emptyList()
        btcHistoryUsd.filter { it > 0.0 }.map { btcUsd -> (btcUsd / CurrencyCatalog.SATS_PER_BTC) * rateTo }
      }
      toIsBtc -> {
        val rateFrom = ratesPerUsd[from] ?: return emptyList()
        if (rateFrom <= 0.0) return emptyList()
        btcHistoryUsd.filter { it > 0.0 }.map { btcUsd -> (1.0 / rateFrom) / btcUsd }
      }
      toIsSats -> {
        val rateFrom = ratesPerUsd[from] ?: return emptyList()
        if (rateFrom <= 0.0) return emptyList()
        btcHistoryUsd.filter { it > 0.0 }.map { btcUsd -> ((1.0 / rateFrom) / btcUsd) * CurrencyCatalog.SATS_PER_BTC }
      }
      else -> emptyList()
    }
  }
}
