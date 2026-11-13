package com.example.domain

import java.util.Currency as JavaCurrency
import java.util.Locale

/**
 * Metadata for a supported fiat or Bitcoin-denominated currency.
 */
data class CurrencyInfo(
  val code: String,
  val name: String,
  val flagEmoji: String,
  val defaultDecimals: Int = 2,
  val isCrypto: Boolean = false,
)

/**
 * Built-in catalog of 160+ world fiat currencies plus Bitcoin (BTC) and Satoshis (SATS).
 * Any additional ISO code returned dynamically by open.er-api.com falls back gracefully to
 * JVM Currency metadata so newly added currencies still appear and convert seamlessly.
 */
object CurrencyCatalog {

  const val CODE_USD = "USD"
  const val CODE_BTC = "BTC"
  const val CODE_SATS = "SATS"
  const val SATS_PER_BTC = 100_000_000.0

  private val zeroDecimalCodes = setOf(
    "SATS", "JPY", "IDR", "KRW", "VND", "CLP", "PYG", "UGX", "RWF",
    "BIF", "DJF", "GNF", "ISK", "KMF", "VUV", "XAF", "XOF", "XPF", "IRR", "LAK",
  )

  private val catalogList: List<CurrencyInfo> = listOf(
    CurrencyInfo("BTC", "Bitcoin", "\u20BF", defaultDecimals = 8, isCrypto = true),
    CurrencyInfo("SATS", "Satoshis", "\u26A1", defaultDecimals = 0, isCrypto = true),
    CurrencyInfo("USD", "United States Dollar", "\uD83C\uDDFA\uD83C\uDDF8"),
    CurrencyInfo("EUR", "Euro", "\uD83C\uDDEA\uD83C\uDDFA"),
    CurrencyInfo("GBP", "British Pound Sterling", "\uD83C\uDDEC\uD83C\uDDE7"),
    CurrencyInfo("JPY", "Japanese Yen", "\uD83C\uDDEF\uD83C\uDDF5", defaultDecimals = 0),
    CurrencyInfo("IDR", "Indonesian Rupiah", "\uD83C\uDDEE\uD83C\uDDE9", defaultDecimals = 0),
    CurrencyInfo("CHF", "Swiss Franc", "\uD83C\uDDE8\uD83C\uDDED"),
    CurrencyInfo("CAD", "Canadian Dollar", "\uD83C\uDDE8\uD83C\uDDE6"),
    CurrencyInfo("AUD", "Australian Dollar", "\uD83C\uDDE6\uD83C\uDDFA"),
    CurrencyInfo("CNY", "Chinese Yuan", "\uD83C\uDDE8\uD83C\uDDF3"),
    CurrencyInfo("INR", "Indian Rupee", "\uD83C\uDDEE\uD83C\uDDF3"),
    CurrencyInfo("KRW", "South Korean Won", "\uD83C\uDDF0\uD83C\uDDF7", defaultDecimals = 0),
    CurrencyInfo("SGD", "Singapore Dollar", "\uD83C\uDDF8\uD83C\uDDEC"),
    CurrencyInfo("HKD", "Hong Kong Dollar", "\uD83C\uDDED\uD83C\uDDF0"),
    CurrencyInfo("NZD", "New Zealand Dollar", "\uD83C\uDDF3\uD83C\uDDFF"),
    CurrencyInfo("SEK", "Swedish Krona", "\uD83C\uDDF8\uD83C\uDDEA"),
    CurrencyInfo("NOK", "Norwegian Krone", "\uD83C\uDDF3\uD83C\uDDF4"),
    CurrencyInfo("DKK", "Danish Krone", "\uD83C\uDDE9\uD83C\uDDF0"),
    CurrencyInfo("MXN", "Mexican Peso", "\uD83C\uDDF2\uD83C\uDDFD"),
    CurrencyInfo("BRL", "Brazilian Real", "\uD83C\uDDE7\uD83C\uDDF7"),
    CurrencyInfo("AED", "United Arab Emirates Dirham", "\uD83C\uDDE6\uD83C\uDDEA"),
    CurrencyInfo("AFN", "Afghan Afghani", "\uD83C\uDDE6\uD83C\uDDEB"),
    CurrencyInfo("ALL", "Albanian Lek", "\uD83C\uDDE6\uD83C\uDDF1"),
    CurrencyInfo("AMD", "Armenian Dram", "\uD83C\uDDE6\uD83C\uDDF2"),
    CurrencyInfo("ANG", "Netherlands Antillean Guilder", "\uD83C\uDDE8\uD83C\uDDFC"),
    CurrencyInfo("AOA", "Angolan Kwanza", "\uD83C\uDDE6\uD83C\uDDF4"),
    CurrencyInfo("ARS", "Argentine Peso", "\uD83C\uDDE6\uD83C\uDDF7"),
    CurrencyInfo("AWG", "Aruban Florin", "\uD83C\uDDE6\uD83C\uDDFC"),
    CurrencyInfo("AZN", "Azerbaijani Manat", "\uD83C\uDDE6\uD83C\uDDFF"),
    CurrencyInfo("BAM", "Bosnia-Herzegovina Convertible Mark", "\uD83C\uDDE7\uD83C\uDDE6"),
    CurrencyInfo("BBD", "Barbadian Dollar", "\uD83C\uDDE7\uD83C\uDDE7"),
    CurrencyInfo("BDT", "Bangladeshi Taka", "\uD83C\uDDE7\uD83C\uDDE9"),
    CurrencyInfo("BGN", "Bulgarian Lev", "\uD83C\uDDE7\uD83C\uDDEC"),
    CurrencyInfo("BHD", "Bahraini Dinar", "\uD83C\uDDE7\uD83C\uDDED"),
    CurrencyInfo("BIF", "Burundian Franc", "\uD83C\uDDE7\uD83C\uDDEE", defaultDecimals = 0),
    CurrencyInfo("BMD", "Bermudian Dollar", "\uD83C\uDDE7\uD83C\uDDF2"),
    CurrencyInfo("BND", "Brunei Dollar", "\uD83C\uDDE7\uD83C\uDDF3"),
    CurrencyInfo("BOB", "Bolivian Boliviano", "\uD83C\uDDE7\uD83C\uDDF4"),
    CurrencyInfo("BSD", "Bahamian Dollar", "\uD83C\uDDE7\uD83C\uDDF8"),
    CurrencyInfo("BTN", "Bhutanese Ngultrum", "\uD83C\uDDE7\uD83C\uDDF9"),
    CurrencyInfo("BWP", "Botswanan Pula", "\uD83C\uDDE7\uD83C\uDDFC"),
    CurrencyInfo("BYN", "Belarusian Ruble", "\uD83C\uDDE7\uD83C\uDDFE"),
    CurrencyInfo("BZD", "Belize Dollar", "\uD83C\uDDE7\uD83C\uDDFF"),
    CurrencyInfo("CDF", "Congolese Franc", "\uD83C\uDDE8\uD83C\uDDE9"),
    CurrencyInfo("CLP", "Chilean Peso", "\uD83C\uDDE8\uD83C\uDDF1", defaultDecimals = 0),
    CurrencyInfo("COP", "Colombian Peso", "\uD83C\uDDE8\uD83C\uDDF4"),
    CurrencyInfo("CRC", "Costa Rican Colón", "\uD83C\uDDE8\uD83C\uDDF7"),
    CurrencyInfo("CUP", "Cuban Peso", "\uD83C\uDDE8\uD83C\uDDFA"),
    CurrencyInfo("CVE", "Cape Verdean Escudo", "\uD83C\uDDE8\uD83C\uDDFB"),
    CurrencyInfo("CZK", "Czech Koruna", "\uD83C\uDDE8\uD83C\uDDFF"),
    CurrencyInfo("DJF", "Djiboutian Franc", "\uD83C\uDDE9\uD83C\uDDEF", defaultDecimals = 0),
    CurrencyInfo("DOP", "Dominican Peso", "\uD83C\uDDE9\uD83C\uDDF4"),
    CurrencyInfo("DZD", "Algerian Dinar", "\uD83C\uDDE9\uD83C\uDDFF"),
    CurrencyInfo("EGP", "Egyptian Pound", "\uD83C\uDDEA\uD83C\uDDEC"),
    CurrencyInfo("ERN", "Eritrean Nakfa", "\uD83C\uDDEA\uD83C\uDDF7"),
    CurrencyInfo("ETB", "Ethiopian Birr", "\uD83C\uDDEA\uD83C\uDDF9"),
    CurrencyInfo("FJD", "Fijian Dollar", "\uD83C\uDDEB\uD83C\uDDEF"),
    CurrencyInfo("FKP", "Falkland Islands Pound", "\uD83C\uDDEB\uD83C\uDDF0"),
    CurrencyInfo("FOK", "Faroese Króna", "\uD83C\uDDEB\uD83C\uDDF4"),
    CurrencyInfo("GEL", "Georgian Lari", "\uD83C\uDDEC\uD83C\uDDEA"),
    CurrencyInfo("GGP", "Guernsey Pound", "\uD83C\uDDEC\uD83C\uDDEC"),
    CurrencyInfo("GHS", "Ghanaian Cedi", "\uD83C\uDDEC\uD83C\uDDED"),
    CurrencyInfo("GIP", "Gibraltar Pound", "\uD83C\uDDEC\uD83C\uDDEE"),
    CurrencyInfo("GMD", "Gambian Dalasi", "\uD83C\uDDEC\uD83C\uDDF2"),
    CurrencyInfo("GNF", "Guinean Franc", "\uD83C\uDDEC\uD83C\uDDF3", defaultDecimals = 0),
    CurrencyInfo("GTQ", "Guatemalan Quetzal", "\uD83C\uDDEC\uD83C\uDDF9"),
    CurrencyInfo("GYD", "Guyanaese Dollar", "\uD83C\uDDEC\uD83C\uDDFE"),
    CurrencyInfo("HNL", "Honduran Lempira", "\uD83C\uDDED\uD83C\uDDF3"),
    CurrencyInfo("HRK", "Croatian Kuna", "\uD83C\uDDED\uD83C\uDDF7"),
    CurrencyInfo("HTG", "Haitian Gourde", "\uD83C\uDDED\uD83C\uDDF9"),
    CurrencyInfo("HUF", "Hungarian Forint", "\uD83C\uDDED\uD83C\uDDFA"),
    CurrencyInfo("ILS", "Israeli New Shekel", "\uD83C\uDDEE\uD83C\uDDF1"),
    CurrencyInfo("IMP", "Isle of Man Pound", "\uD83C\uDDEE\uD83C\uDDF2"),
    CurrencyInfo("IQD", "Iraqi Dinar", "\uD83C\uDDEE\uD83C\uDDF6"),
    CurrencyInfo("IRR", "Iranian Rial", "\uD83C\uDDEE\uD83C\uDDF7", defaultDecimals = 0),
    CurrencyInfo("ISK", "Icelandic Króna", "\uD83C\uDDEE\uD83C\uDDF8", defaultDecimals = 0),
    CurrencyInfo("JEP", "Jersey Pound", "\uD83C\uDDEF\uD83C\uDDEA"),
    CurrencyInfo("JMD", "Jamaican Dollar", "\uD83C\uDDEF\uD83C\uDDF2"),
    CurrencyInfo("JOD", "Jordanian Dinar", "\uD83C\uDDEF\uD83C\uDDF4"),
    CurrencyInfo("KES", "Kenyan Shilling", "\uD83C\uDDF0\uD83C\uDDEA"),
    CurrencyInfo("KGS", "Kyrgystani Som", "\uD83C\uDDF0\uD83C\uDDEC"),
    CurrencyInfo("KHR", "Cambodian Riel", "\uD83C\uDDF0\uD83C\uDDED"),
    CurrencyInfo("KID", "Kiribati Dollar", "\uD83C\uDDF0\uD83C\uDDEE"),
    CurrencyInfo("KMF", "Comorian Franc", "\uD83C\uDDF0\uD83C\uDDF2", defaultDecimals = 0),
    CurrencyInfo("KWD", "Kuwaiti Dinar", "\uD83C\uDDF0\uD83C\uDDFC"),
    CurrencyInfo("KYD", "Cayman Islands Dollar", "\uD83C\uDDF0\uD83C\uDDFE"),
    CurrencyInfo("KZT", "Kazakhstani Tenge", "\uD83C\uDDF0\uD83C\uDDFF"),
    CurrencyInfo("LAK", "Laotian Kip", "\uD83C\uDDF1\uD83C\uDDE6", defaultDecimals = 0),
    CurrencyInfo("LBP", "Lebanese Pound", "\uD83C\uDDF1\uD83C\uDDE7"),
    CurrencyInfo("LKR", "Sri Lankan Rupee", "\uD83C\uDDF1\uD83C\uDDF0"),
    CurrencyInfo("LRD", "Liberian Dollar", "\uD83C\uDDF1\uD83C\uDDF7"),
    CurrencyInfo("LSL", "Lesotho Loti", "\uD83C\uDDF1\uD83C\uDDF8"),
    CurrencyInfo("LYD", "Libyan Dinar", "\uD83C\uDDF1\uD83C\uDDFE"),
    CurrencyInfo("MAD", "Moroccan Dirham", "\uD83C\uDDF2\uD83C\uDDE6"),
    CurrencyInfo("MDL", "Moldovan Leu", "\uD83C\uDDF2\uD83C\uDDE9"),
    CurrencyInfo("MGA", "Malagasy Ariary", "\uD83C\uDDF2\uD83C\uDDEC"),
    CurrencyInfo("MKD", "Macedonian Denar", "\uD83C\uDDF2\uD83C\uDDF0"),
    CurrencyInfo("MMK", "Myanmar Kyat", "\uD83C\uDDF2\uD83C\uDDF2"),
    CurrencyInfo("MNT", "Mongolian Tugrik", "\uD83C\uDDF2\uD83C\uDDF3"),
    CurrencyInfo("MOP", "Macanese Pataca", "\uD83C\uDDF2\uD83C\uDDF4"),
    CurrencyInfo("MRU", "Mauritanian Ouguiya", "\uD83C\uDDF2\uD83C\uDDF7"),
    CurrencyInfo("MUR", "Mauritian Rupee", "\uD83C\uDDF2\uD83C\uDDFA"),
    CurrencyInfo("MVR", "Maldivian Rufiyaa", "\uD83C\uDDF2\uD83C\uDDFB"),
    CurrencyInfo("MWK", "Malawian Kwacha", "\uD83C\uDDF2\uD83C\uDDFC"),
    CurrencyInfo("MYR", "Malaysian Ringgit", "\uD83C\uDDF2\uD83C\uDDFE"),
    CurrencyInfo("MZN", "Mozambican Metical", "\uD83C\uDDF2\uD83C\uDDFF"),
    CurrencyInfo("NAD", "Namibian Dollar", "\uD83C\uDDF3\uD83C\uDDE6"),
    CurrencyInfo("NGN", "Nigerian Naira", "\uD83C\uDDF3\uD83C\uDDEC"),
    CurrencyInfo("NIO", "Nicaraguan Córdoba", "\uD83C\uDDF3\uD83C\uDDEE"),
    CurrencyInfo("NPR", "Nepalese Rupee", "\uD83C\uDDF3\uD83C\uDDF5"),
    CurrencyInfo("OMR", "Omani Rial", "\uD83C\uDDF4\uD83C\uDDF2"),
    CurrencyInfo("PAB", "Panamanian Balboa", "\uD83C\uDDF5\uD83C\uDDE6"),
    CurrencyInfo("PEN", "Peruvian Sol", "\uD83C\uDDF5\uD83C\uDDEA"),
    CurrencyInfo("PGK", "Papua New Guinean Kina", "\uD83C\uDDF5\uD83C\uDDEC"),
    CurrencyInfo("PHP", "Philippine Peso", "\uD83C\uDDF5\uD83C\uDDED"),
    CurrencyInfo("PKR", "Pakistani Rupee", "\uD83C\uDDF5\uD83C\uDDF0"),
    CurrencyInfo("PLN", "Polish Zloty", "\uD83C\uDDF5\uD83C\uDDF1"),
    CurrencyInfo("PYG", "Paraguayan Guarani", "\uD83C\uDDF5\uD83C\uDDFE", defaultDecimals = 0),
    CurrencyInfo("QAR", "Qatari Rial", "\uD83C\uDDF6\uD83C\uDDE6"),
    CurrencyInfo("RON", "Romanian Leu", "\uD83C\uDDF7\uD83C\uDDF4"),
    CurrencyInfo("RSD", "Serbian Dinar", "\uD83C\uDDF7\uD83C\uDDF8"),
    CurrencyInfo("RUB", "Russian Ruble", "\uD83C\uDDF7\uD83C\uDDFA"),
    CurrencyInfo("RWF", "Rwandan Franc", "\uD83C\uDDF7\uD83C\uDDFC", defaultDecimals = 0),
    CurrencyInfo("SAR", "Saudi Riyal", "\uD83C\uDDF8\uD83C\uDDE6"),
    CurrencyInfo("SBD", "Solomon Islands Dollar", "\uD83C\uDDF8\uD83C\uDDE7"),
    CurrencyInfo("SCR", "Seychellois Rupee", "\uD83C\uDDF8\uD83C\uDDE8"),
    CurrencyInfo("SDG", "Sudanese Pound", "\uD83C\uDDF8\uD83C\uDDE9"),
    CurrencyInfo("SHP", "Saint Helena Pound", "\uD83C\uDDF8\uD83C\uDDED"),
    CurrencyInfo("SLE", "Sierra Leonean Leone", "\uD83C\uDDF8\uD83C\uDDF1"),
    CurrencyInfo("SLL", "Sierra Leonean Leone (Old)", "\uD83C\uDDF8\uD83C\uDDF1"),
    CurrencyInfo("SOS", "Somali Shilling", "\uD83C\uDDF8\uD83C\uDDF4"),
    CurrencyInfo("SRD", "Surinamese Dollar", "\uD83C\uDDF8\uD83C\uDDF7"),
    CurrencyInfo("SSP", "South Sudanese Pound", "\uD83C\uDDF8\uD83C\uDDF8"),
    CurrencyInfo("STN", "São Tomé and Príncipe Dobra", "\uD83C\uDDF8\uD83C\uDDF9"),
    CurrencyInfo("SYP", "Syrian Pound", "\uD83C\uDDF8\uD83C\uDDFE"),
    CurrencyInfo("SZL", "Swazi Lilangeni", "\uD83C\uDDF8\uD83C\uDDFF"),
    CurrencyInfo("THB", "Thai Baht", "\uD83C\uDDF9\uD83C\uDDED"),
    CurrencyInfo("TJS", "Tajikistani Somoni", "\uD83C\uDDF9\uD83C\uDDEF"),
    CurrencyInfo("TMT", "Turkmenistani Manat", "\uD83C\uDDF9\uD83C\uDDF2"),
    CurrencyInfo("TND", "Tunisian Dinar", "\uD83C\uDDF9\uD83C\uDDF3"),
    CurrencyInfo("TOP", "Tongan Paʻanga", "\uD83C\uDDF9\uD83C\uDDF4"),
    CurrencyInfo("TRY", "Turkish Lira", "\uD83C\uDDF9\uD83C\uDDF7"),
    CurrencyInfo("TTD", "Trinidad and Tobago Dollar", "\uD83C\uDDF9\uD83C\uDDF9"),
    CurrencyInfo("TVD", "Tuvaluan Dollar", "\uD83C\uDDF9\uD83C\uDDFB"),
    CurrencyInfo("TWD", "New Taiwan Dollar", "\uD83C\uDDF9\uD83C\uDDFC"),
    CurrencyInfo("TZS", "Tanzanian Shilling", "\uD83C\uDDF9\uD83C\uDDFF"),
    CurrencyInfo("UAH", "Ukrainian Hryvnia", "\uD83C\uDDFA\uD83C\uDDE6"),
    CurrencyInfo("UGX", "Ugandan Shilling", "\uD83C\uDDFA\uD83C\uDDEC", defaultDecimals = 0),
    CurrencyInfo("UYU", "Uruguayan Peso", "\uD83C\uDDFA\uD83C\uDDFE"),
    CurrencyInfo("UZS", "Uzbekistani Som", "\uD83C\uDDFA\uD83C\uDDFF"),
    CurrencyInfo("VES", "Venezuelan Bolívar Soberano", "\uD83C\uDDFB\uD83C\uDDEA"),
    CurrencyInfo("VND", "Vietnamese Dong", "\uD83C\uDDFB\uD83C\uDDF3", defaultDecimals = 0),
    CurrencyInfo("VUV", "Vanuatu Vatu", "\uD83C\uDDFB\uD83C\uDDFA", defaultDecimals = 0),
    CurrencyInfo("WST", "Samoan Tala", "\uD83C\uDDFC\uD83C\uDDF8"),
    CurrencyInfo("XAF", "Central African CFA Franc", "\uD83C\uDDE8\uD83C\uDDF2", defaultDecimals = 0),
    CurrencyInfo("XCD", "East Caribbean Dollar", "\uD83C\uDDF1\uD83C\uDDE8"),
    CurrencyInfo("XCG", "Caribbean Guilder", "\uD83C\uDDE8\uD83C\uDDFC"),
    CurrencyInfo("XDR", "Special Drawing Rights", "\uD83C\uDF10"),
    CurrencyInfo("XOF", "West African CFA Franc", "\uD83C\uDDF8\uD83C\uDDF3", defaultDecimals = 0),
    CurrencyInfo("XPF", "CFP Franc", "\uD83C\uDDF5\uD83C\uDDEB", defaultDecimals = 0),
    CurrencyInfo("YER", "Yemeni Rial", "\uD83C\uDDFE\uD83C\uDDEA"),
    CurrencyInfo("ZAR", "South African Rand", "\uD83C\uDDFF\uD83C\uDDE6"),
    CurrencyInfo("ZMW", "Zambian Kwacha", "\uD83C\uDDFF\uD83C\uDDF2"),
    CurrencyInfo("ZWL", "Zimbabwean Dollar", "\uD83C\uDDFF\uD83C\uDDFC"),
    CurrencyInfo("ZWG", "Zimbabwe Gold", "\uD83C\uDDFF\uD83C\uDDFC"),
  )

  private val byCode: Map<String, CurrencyInfo> = catalogList.associateBy { it.code }

  val defaultCatalog: List<CurrencyInfo> = catalogList

  fun isZeroDecimal(code: String): Boolean = code.uppercase(Locale.US) in zeroDecimalCodes

  fun getInfo(code: String): CurrencyInfo {
    val normalized = code.trim().uppercase(Locale.US)
    byCode[normalized]?.let { return it }

    val fallbackName = runCatching {
      JavaCurrency.getInstance(normalized).getDisplayName(Locale.ENGLISH)
    }.getOrNull() ?: normalized

    return CurrencyInfo(
      code = normalized,
      name = fallbackName,
      flagEmoji = countryCodeToFlag(normalized),
      defaultDecimals = if (normalized in zeroDecimalCodes) 0 else 2,
      isCrypto = false,
    )
  }

  /**
   * Builds a complete ordered list of currencies combining the built-in catalog
   * and any extra codes supplied by live/cached rates.
   */
  fun allCurrencies(availableRateCodes: Set<String>): List<CurrencyInfo> {
    val extraCodes = availableRateCodes
      .map { it.uppercase(Locale.US) }
      .filter { it !in byCode }
      .sorted()
      .map { getInfo(it) }
    return catalogList + extraCodes
  }

  private fun countryCodeToFlag(currencyCode: String): String {
    if (currencyCode.length < 2) return "\uD83C\uDF10"
    val c1 = currencyCode[0]
    val c2 = currencyCode[1]
    if (c1 !in 'A'..'Z' || c2 !in 'A'..'Z') return "\uD83C\uDF10"
    val regionalA = 0x1F1E6
    val first = Character.toChars(regionalA + (c1 - 'A'))
    val second = Character.toChars(regionalA + (c2 - 'A'))
    return String(first) + String(second)
  }
}
