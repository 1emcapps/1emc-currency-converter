package com.example

import com.example.domain.ConversionOutcome
import com.example.domain.CurrencyCatalog
import com.example.domain.CurrencyConverter
import com.example.domain.DecimalPrecisionMode
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConverterTest {

  private val sampleRates = CurrencyConverter.buildRatesPerUsdMap(
    fiatRatesPerUsd = mapOf(
      "USD" to 1.0,
      "EUR" to 0.92,
      "GBP" to 0.79,
      "JPY" to 150.0,
      "IDR" to 16000.0,
    ),
    btcPriceInUsd = 80_000.0,
  )

  @Test
  fun `pure conversion formula amount times rateTo over rateFrom`() {
    // 100 EUR -> JPY: 100 * (150 / 0.92)
    val result = CurrencyConverter.convert(
      amount = BigDecimal("100"),
      rateFromPerUsd = BigDecimal("0.92"),
      rateToPerUsd = BigDecimal("150"),
    )!!
    assertEquals(16304.3478, result.toDouble(), 0.001)
  }

  @Test
  fun `btc to usd and usd to btc via pivot`() {
    val btcToUsd = CurrencyConverter.convertByCode("1.5", "BTC", "USD", sampleRates)
    assertTrue(btcToUsd is ConversionOutcome.Success)
    assertEquals("120,000.00", (btcToUsd as ConversionOutcome.Success).formatted)

    val usdToBtc = CurrencyConverter.convertByCode("40000", "USD", "BTC", sampleRates)
    assertTrue(usdToBtc is ConversionOutcome.Success)
    assertEquals("0.5", (usdToBtc as ConversionOutcome.Success).formatted)
  }

  @Test
  fun `btc to sats and sats to fiat conversion`() {
    val btcToSats = CurrencyConverter.convertByCode("1", "BTC", "SATS", sampleRates)
    assertTrue(btcToSats is ConversionOutcome.Success)
    assertEquals("100,000,000", (btcToSats as ConversionOutcome.Success).formatted)

    // At 80,000 USD/BTC, 1,000 SATS = 0.00001 BTC = 0.80 USD = 120 JPY
    val satsToJpy = CurrencyConverter.convertByCode("1000", "SATS", "JPY", sampleRates)
    assertTrue(satsToJpy is ConversionOutcome.Success)
    assertEquals("120", (satsToJpy as ConversionOutcome.Success).formatted)
  }

  @Test
  fun `zero decimal currencies and btc 8 decimal formatting`() {
    val usdToIdr = CurrencyConverter.convertByCode("2", "USD", "IDR", sampleRates)
    assertEquals("32,000", (usdToIdr as ConversionOutcome.Success).formatted)

    // 1 USD = 1 / 80,000 = 0.0000125 BTC
    val usdToBtc = CurrencyConverter.convertByCode("1", "USD", "BTC", sampleRates)
    assertEquals("0.0000125", (usdToBtc as ConversionOutcome.Success).formatted)

    // 1 JPY = 1 / (150 * 80,000) = 0.0000000833 -> rounds to 0.00000008 (8 decimals)
    val jpyToBtc = CurrencyConverter.convertByCode("1", "JPY", "BTC", sampleRates)
    assertEquals("0.00000008", (jpyToBtc as ConversionOutcome.Success).formatted)
  }

  @Test
  fun `edge cases empty input lone decimal same currency missing rate large numbers`() {
    // Empty input -> 0
    val emptyOutcome = CurrencyConverter.convertByCode("", "USD", "EUR", sampleRates)
    assertEquals("0.00", (emptyOutcome as ConversionOutcome.Success).formatted)

    // Lone decimal point -> 0
    val dotOutcome = CurrencyConverter.convertByCode(".", "USD", "EUR", sampleRates)
    assertEquals("0.00", (dotOutcome as ConversionOutcome.Success).formatted)

    // Same currency -> exact input value even if missing from rate map
    val sameCurrency = CurrencyConverter.convertByCode("1234.5", "XYZ", "XYZ", emptyMap())
    assertEquals("1,234.50", (sameCurrency as ConversionOutcome.Success).formatted)

    // Missing rate -> MissingRate
    val missingRate = CurrencyConverter.convertByCode("10", "USD", "ZZZ", sampleRates)
    assertEquals(ConversionOutcome.MissingRate, missingRate)

    // Invalid zero or negative rates -> null
    assertNull(CurrencyConverter.convert(10.0, 0.0, 1.0))
    assertNull(CurrencyConverter.convert(10.0, 1.0, -5.0))

    // Very large number stays finite and formatted with thousands separators
    val huge = CurrencyConverter.convertByCode("999999999999", "USD", "SATS", sampleRates)
    assertTrue(huge is ConversionOutcome.Success)
    assertTrue((huge as ConversionOutcome.Success).formatted.contains(","))
  }

  @Test
  fun `decimal precision mode overrides apply cleanly`() {
    val fourDec = CurrencyConverter.convertByCode(
      "1",
      "USD",
      "EUR",
      sampleRates,
      DecimalPrecisionMode.FOUR,
    ) as ConversionOutcome.Success
    assertEquals("0.9200", fourDec.formatted)

    val eightDec = CurrencyConverter.convertByCode(
      "1",
      "EUR",
      "USD",
      sampleRates,
      DecimalPrecisionMode.EIGHT,
    ) as ConversionOutcome.Success
    assertEquals("1.08695652", eightDec.formatted)
  }

  @Test
  fun `sparkline computation scales btc history into target fiat`() {
    val btcHistory = listOf(70_000.0, 75_000.0, 80_000.0)
    val btcEurSeries = CurrencyConverter.computePairSparkline("BTC", "EUR", btcHistory, sampleRates)
    assertEquals(3, btcEurSeries.size)
    assertEquals(70_000.0 * 0.92, btcEurSeries[0], 0.01)
    assertEquals(80_000.0 * 0.92, btcEurSeries[2], 0.01)

    // Fiat-to-fiat returns empty sparkline
    val fiatSeries = CurrencyConverter.computePairSparkline("USD", "EUR", btcHistory, sampleRates)
    assertTrue(fiatSeries.isEmpty())
  }

  @Test
  fun `catalog supports 150 plus currencies`() {
    assertTrue(CurrencyCatalog.defaultCatalog.size >= 150)
  }
}
